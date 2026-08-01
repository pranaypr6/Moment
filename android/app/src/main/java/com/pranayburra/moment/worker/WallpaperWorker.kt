package com.pranayburra.moment.worker

import android.app.WallpaperManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.pranayburra.moment.domain.repository.MomentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.pranayburra.moment.MainActivity

@HiltWorker
class WallpaperWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MomentRepository,
    private val momentDao: com.pranayburra.moment.data.local.MomentDao
) : CoroutineWorker(context, params) {

    // Deliberately no getForegroundInfo()/setForeground() override here. This worker used to
    // promote itself to a foreground service (FOREGROUND_SERVICE_TYPE_DATA_SYNC) on API < 31
    // as part of the pre-Android-12 expedited-work compat path, but that required the
    // FOREGROUND_SERVICE / FOREGROUND_SERVICE_DATA_SYNC manifest permissions, which were
    // deliberately removed (see AndroidManifest.xml) specifically to avoid Google Play
    // Console's foreground-service-type justification-video requirement. Removing only the
    // permissions while this override still called setForeground() would have left a live
    // crash/degraded-behavior path on Android 8-11 (API 26-30) for every FCM-triggered
    // wallpaper apply - a real, still-supported range given minSdk 26 - so this override was
    // removed too, not just the manifest permissions. The enqueue sites
    // (MomentFirebaseMessagingService, MomentRepositoryImpl) already set
    // OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST, so if expedited quota isn't
    // available on an older device, WorkManager just runs this as regular (non-expedited)
    // background work instead of needing a foreground service at all - the correct, intended
    // use of that fallback policy rather than accidentally relying on a caught exception.
    private fun showNotification(context: Context, senderName: String) {
        val channelId = "moment_delivery_heartbeat"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Moment Delivery", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifies you when a new moment has been successfully applied to your wallpaper."
                enableVibration(true)
                // Heartbeat pattern: thump... THUMP
                vibrationPattern = longArrayOf(0, 50, 150, 60)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("openTab", "Moments")
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("❤️ $senderName left something on your screen")
            .setContentText("Go take a look.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 50, 150, 60))
            .build()

        notificationManager.notify(1001, notification)
    }

    private fun backupCurrentWallpaper(context: Context, wallpaperManager: WallpaperManager, target: String) {
        try {
            Log.d("WallpaperWorker", "Backing up current wallpaper for $target")
            val flag = if (target == "LOCK") WallpaperManager.FLAG_LOCK else WallpaperManager.FLAG_SYSTEM
            val drawable = wallpaperManager.getDrawable(flag)
            if (drawable != null) {
                val bitmap = when (drawable) {
                    is BitmapDrawable -> drawable.bitmap
                    else -> {
                        val maxD = 2160
                        val rawW = drawable.intrinsicWidth
                        val rawH = drawable.intrinsicHeight
                        val w = if (rawW > 0) rawW.coerceAtMost(maxD) else 1080
                        val h = if (rawH > 0) rawH.coerceAtMost(maxD) else 1920
                        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(b)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        b
                    }
                }
                val file = File(context.cacheDir, "wallpaper_backup_${target}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                Log.d("WallpaperWorker", "Backup successful: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e("WallpaperWorker", "Backup failed", e)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val imageUrl = inputData.getString("imageUrl") ?: return@withContext Result.failure()
        val momentId = inputData.getString("momentId") ?: return@withContext Result.failure()
        val target = inputData.getString("wallpaperTarget")?.uppercase() ?: "BOTH"
        val senderName = inputData.getString("senderName") ?: "Someone"
        
        val relationshipId = inputData.getString("relationshipId") ?: ""
        val creatorId = inputData.getString("creatorId") ?: ""
        val thumbnailUrl = inputData.getString("thumbnailUrl")
        val note = inputData.getString("note")
        val createdAt = inputData.getLong("createdAt", System.currentTimeMillis())

        Log.d("WallpaperWorker", "WORKER_START: $momentId | Target: $target")

        // De-dup guard: FCM is at-least-once delivery, so the same push can legitimately
        // arrive twice. enqueueUniqueWork only protects against a still-pending/running
        // duplicate; it does nothing once the first attempt has already completed. Without
        // this check a redelivered push would silently re-download and re-apply the same
        // wallpaper and re-fire the "someone left you something" notification.
        val existing = momentDao.getMomentById(momentId)
        if (existing != null && existing.status == "APPLIED") {
            Log.d("WallpaperWorker", "SKIP_DUPLICATE: $momentId already applied.")
            // The local DB thinks this is done, but confirm with the server too in case an
            // earlier attempt's confirmation never made it through (offline at the time) -
            // idempotent and cheap, and closes the loop so a future resync on a *different*
            // device/reinstall doesn't redeliver this same moment.
            repository.markMomentApplied(momentId)
            return@withContext Result.success()
        }

        // Ordering guard: WallpaperWorker jobs for different moments have no ordering
        // guarantee relative to each other (each has its own unique work name and its own
        // network download, so they can genuinely run out of order). Normally that's fine
        // since only one moment arrives at a time, but after being offline for a while, a
        // backlog of pending moments can all get queued together - if an older one's job
        // happens to finish after a newer one (which can easily happen; e.g. after a forced
        // re-login moves a stale, previously-undelivered moment through the pending-sync
        // path), it would silently roll the wallpaper back to something older than what's
        // already showing. Skip actually re-wallpapering for anything older than the most
        // recent moment we've already applied for this relationship - still record it below
        // so it shows up correctly in the Moments timeline.
        if (relationshipId.isNotEmpty()) {
            val latestApplied = momentDao.getLatestAppliedMoment(relationshipId)
            if (latestApplied != null && latestApplied.id != momentId && latestApplied.createdAt > createdAt) {
                Log.d("WallpaperWorker", "SKIP_STALE: $momentId ($createdAt) is older than already-applied ${latestApplied.id} (${latestApplied.createdAt}); recording without re-wallpapering.")
                val entity = com.pranayburra.moment.data.local.MomentEntity(
                    id = momentId,
                    relationshipId = relationshipId,
                    creatorId = creatorId,
                    creatorName = senderName,
                    imageUrl = imageUrl,
                    thumbnailUrl = thumbnailUrl,
                    note = note,
                    wallpaperTarget = target,
                    isFavorite = false,
                    status = "APPLIED",
                    createdAt = createdAt
                )
                momentDao.insertMoment(entity)
                repository.markMomentApplied(momentId)
                return@withContext Result.success()
            }
        }

        // Only trust image URLs that are HTTPS and (when configured) match our known
        // storage/CDN host. FCM data payloads are not cryptographically tied to our backend
        // as far as this client can verify, so a compromised/spoofed message could otherwise
        // point this worker at an attacker-controlled URL and have it silently become the
        // user's wallpaper.
        val parsedUri = android.net.Uri.parse(imageUrl)
        val trustedSuffixes = com.pranayburra.moment.BuildConfig.TRUSTED_IMAGE_HOST_SUFFIX
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val hostOk = parsedUri.host != null &&
            (trustedSuffixes.isEmpty() || trustedSuffixes.any { parsedUri.host!!.endsWith(it, ignoreCase = true) })
        if (parsedUri.scheme != "https" || !hostOk) {
            Log.e("WallpaperWorker", "REJECTED_UNTRUSTED_URL: $momentId host=${parsedUri.host} scheme=${parsedUri.scheme}")
            return@withContext Result.failure()
        }

        try {
            val imageLoader = ImageLoader(applicationContext)
            val request = ImageRequest.Builder(applicationContext)
                .data(imageUrl)
                .size(2160) // Prevent OOM by capping decode size
                .allowHardware(false)
                .build()

            Log.d("WallpaperWorker", "Downloading image from: $imageUrl")
            val result = imageLoader.execute(request)
            
            if (result is SuccessResult) {
                Log.d("WallpaperWorker", "Download complete. Processing bitmap...")
                val drawable = result.drawable
                val bitmap = when (drawable) {
                    is BitmapDrawable -> drawable.bitmap
                    else -> {
                        val maxD = 2160
                        val rawW = drawable.intrinsicWidth
                        val rawH = drawable.intrinsicHeight
                        val w = if (rawW > 0) rawW.coerceAtMost(maxD) else 1080
                        val h = if (rawH > 0) rawH.coerceAtMost(maxD) else 1920
                        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(b)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        b
                    }
                }

                val wallpaperManager = WallpaperManager.getInstance(applicationContext)
                backupCurrentWallpaper(applicationContext, wallpaperManager, target)
                
                Log.d("WallpaperWorker", "Applying wallpaper...")
                try {
                    when (target) {
                        "HOME" -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        "LOCK" -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                        "BOTH" -> {
                            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                        }
                    }
                    Log.d("WallpaperWorker", "APPLY_SUCCESS: $momentId")
                    
                    // Insert into local DB so UI updates instantly
                    if (relationshipId.isNotEmpty()) {
                        val entity = com.pranayburra.moment.data.local.MomentEntity(
                            id = momentId,
                            relationshipId = relationshipId,
                            creatorId = creatorId,
                            creatorName = senderName,
                            imageUrl = imageUrl,
                            thumbnailUrl = thumbnailUrl,
                            note = note,
                            wallpaperTarget = target,
                            isFavorite = false,
                            status = "APPLIED",
                            createdAt = createdAt
                        )
                        momentDao.insertMoment(entity)
                    }
                    repository.markMomentApplied(momentId)
                } catch (se: SecurityException) {
                    Log.e("WallpaperWorker", "Missing SET_WALLPAPER permission", se)
                    return@withContext Result.failure()
                } catch (e: Exception) {
                    Log.e("WallpaperWorker", "WallpaperManager.setBitmap failed", e)
                    throw e
                }

                val shouldShowNotification = inputData.getBoolean("showNotification", true)
                if (shouldShowNotification) {
                    showNotification(applicationContext, senderName)
                }
                Result.success()
            } else {
                Log.e("WallpaperWorker", "DOWNLOAD_FAILED: $momentId")
                Result.retry()
            }
        } catch (oom: OutOfMemoryError) {
            Log.e("WallpaperWorker", "WORKER_OOM: $momentId", oom)
            Result.failure()
        } catch (e: Exception) {
            Log.e("WallpaperWorker", "WORKER_ERROR: $momentId", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

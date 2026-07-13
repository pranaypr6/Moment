package com.moment.app.worker

import android.app.WallpaperManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ForegroundInfo
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.moment.app.domain.repository.MomentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.moment.app.MainActivity

@HiltWorker
class WallpaperWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MomentRepository,
    private val momentDao: com.moment.app.data.local.MomentDao
) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val channelId = "moment_updates"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, "Moment Updates", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("Applying Moment...")
            .setContentText("A new surprise is being prepared.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(1002, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(1002, notification)
        }
    }

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
            .setContentTitle("❤️ $senderName left something for you")
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
                        val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1080
                        val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1920
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

        try {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    setForeground(getForegroundInfo())
                }
            } catch (e: Exception) {
                Log.e("WallpaperWorker", "Failed to setForeground", e)
            }

            val imageLoader = ImageLoader(applicationContext)
            val request = ImageRequest.Builder(applicationContext)
                .data(imageUrl)
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
                        val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1080
                        val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1920
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
                        val entity = com.moment.app.data.local.MomentEntity(
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

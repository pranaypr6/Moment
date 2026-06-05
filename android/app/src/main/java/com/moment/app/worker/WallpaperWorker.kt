package com.moment.app.worker

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
    private val repository: MomentRepository
) : CoroutineWorker(context, params) {

    private fun showNotification(context: Context, senderName: String) {
        val channelId = "moment_updates"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Moment Updates", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("✨ New Moment Applied")
            .setContentText("Sent by $senderName")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    private fun backupCurrentWallpaper(context: Context, wallpaperManager: WallpaperManager, target: String) {
        try {
            val flag = if (target == "LOCK") WallpaperManager.FLAG_LOCK else WallpaperManager.FLAG_SYSTEM
            val drawable = wallpaperManager.getDrawable(flag)
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                val file = File(context.cacheDir, "wallpaper_backup_${target}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
            }
        } catch (e: Exception) {
            // Log backup failure, but don't block applying the new one
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val imageUrl = inputData.getString("imageUrl") ?: return@withContext Result.failure()
        val momentId = inputData.getString("momentId") ?: return@withContext Result.failure()
        val target = inputData.getString("wallpaperTarget") ?: "BOTH"
        val senderName = inputData.getString("senderName") ?: "Someone"

        try {
            Log.d("WallpaperWorker", "Starting wallpaper application for moment: $momentId")
            Log.d("WallpaperWorker", "Downloading image: $imageUrl")
            
            val imageLoader = ImageLoader(applicationContext)
            val request = ImageRequest.Builder(applicationContext)
                .data(imageUrl)
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                Log.d("WallpaperWorker", "Image downloaded successfully")
                val drawable = result.drawable
                val bitmap = (drawable as BitmapDrawable).bitmap

                val wallpaperManager = WallpaperManager.getInstance(applicationContext)
                
                // Backup before changing
                backupCurrentWallpaper(applicationContext, wallpaperManager, target)
                
                Log.d("WallpaperWorker", "Applying wallpaper to target: $target")
                when (target) {
                    "HOME" -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    "LOCK" -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    "BOTH" -> {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    }
                }

                repository.updateMomentStatus(momentId, "APPLIED")
                Log.d("WallpaperWorker", "Wallpaper applied successfully")
                showNotification(applicationContext, senderName)
                Result.success()
            } else {
                Log.e("WallpaperWorker", "Failed to download image (Result is not SuccessResult)")
                repository.updateMomentStatus(momentId, "FAILED")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("WallpaperWorker", "Error applying wallpaper", e)
            repository.updateMomentStatus(momentId, "FAILED")
            Result.failure()
        }
    }
}

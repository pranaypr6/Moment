package com.moment.app.service

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.work.*
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.moment.app.worker.WallpaperWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MomentFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        if (data.containsKey("momentId") && data.containsKey("imageUrl")) {
            val momentId = data["momentId"] ?: return
            val imageUrl = data["imageUrl"] ?: return
            val senderName = data["senderName"] ?: "Someone"
            val wallpaperTarget = data["wallpaperTarget"] ?: "BOTH"

            // Architect Tier: Acquire a WakeLock to prevent the OS from freezing the process
            // before WorkManager can start the expedited job.
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Moment:FCM_RECEIVE_WAKE_LOCK"
            )
            
            // Hold for 30 seconds max to bridge the gap to Worker start
            wakeLock.acquire(30 * 1000L)

            val workData = Data.Builder()
                .putString("momentId", momentId)
                .putString("imageUrl", imageUrl)
                .putString("senderName", senderName)
                .putString("wallpaperTarget", wallpaperTarget)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<WallpaperWorker>()
                .setInputData(workData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            // Use REPLACE to ensure any deferred background jobs are superseded by this fresh request
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "apply_moment_$momentId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            
            Log.d("FCM", "Enqueued Expedited Worker for moment: $momentId")
        }
    }
}

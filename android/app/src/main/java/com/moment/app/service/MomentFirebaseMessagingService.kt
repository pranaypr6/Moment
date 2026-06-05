package com.moment.app.service

import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.moment.app.worker.WallpaperWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MomentFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        // The repository should ideally listen to this and sync it to the backend.
        // For MVP, we'll register the device on login/startup instead to ensure it has the user token.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        if (data.containsKey("momentId") && data.containsKey("imageUrl")) {
            val momentId = data["momentId"]
            val imageUrl = data["imageUrl"]
            val wallpaperTarget = data["wallpaperTarget"] ?: "BOTH"

            val workData = Data.Builder()
                .putString("momentId", momentId)
                .putString("imageUrl", imageUrl)
                .putString("wallpaperTarget", wallpaperTarget)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<WallpaperWorker>()
                .setInputData(workData)
                .build()

            WorkManager.getInstance(applicationContext).enqueue(workRequest)
        }
    }
}

package com.moment.app.service

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.work.*
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.moment.app.worker.WallpaperWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.moment.app.MainActivity

@AndroidEntryPoint
class MomentFirebaseMessagingService : FirebaseMessagingService() {

    @javax.inject.Inject
    lateinit var deviceRepository: com.moment.app.domain.repository.DeviceRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            deviceRepository.registerDevice(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        if (data.containsKey("momentId") && data.containsKey("imageUrl")) {
            val momentId = data["momentId"] ?: return
            val imageUrl = data["imageUrl"] ?: return
            val senderName = data["senderName"] ?: "Someone"
            val wallpaperTarget = data["wallpaperTarget"] ?: "BOTH"
            
            val relationshipId = data["relationshipId"] ?: ""
            val creatorId = data["creatorId"] ?: ""
            val thumbnailUrl = data["thumbnailUrl"]
            val note = data["note"]
            val status = data["status"] ?: "PENDING"
            val createdAtStr = data["createdAt"]
            val createdAt = createdAtStr?.toLongOrNull() ?: System.currentTimeMillis()

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
                .putString("relationshipId", relationshipId)
                .putString("creatorId", creatorId)
                .putString("thumbnailUrl", thumbnailUrl)
                .putString("note", note)
                .putString("status", status)
                .putLong("createdAt", createdAt)
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
        } else if (data["signalType"] == "presence") {
            val presenceType = data["presenceType"] ?: return
            val senderName = data["senderName"] ?: "Someone"
            
            if (presenceType != "None") {
                showEmotionalActionNotification(applicationContext, presenceType, senderName)
            }
        }
    }

    private fun showEmotionalActionNotification(context: Context, presenceType: String, senderName: String) {
        val channelId = "presence_signals"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Presence Signals", 
                NotificationManager.IMPORTANCE_HIGH // High importance for Heads-Up
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("interactionType", presenceType)
        }
        val pendingIntent = PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val (title, body) = when (presenceType) {
            "ThinkingOfYou" -> Pair("💕 $senderName sent you some love", "You're on their mind right now.")
            "PlayfulPunch" -> Pair("👊 You got playfully punched", "Time to get revenge 😄")
            "Poke" -> Pair("👊 You got playfully punched", "Time to get revenge 😄") // Legacy fallback
            "Hug" -> Pair("🤗 $senderName sent you a hug", "Take a deep breath. This one is for you.")
            "Kiss" -> Pair("😘 A kiss is waiting for you", "Sent with absolutely no reason.")
            "Rose" -> Pair("🌹 $senderName sent you a rose", "Just because today deserved one.")
            else -> Pair("❤️ $senderName", "$senderName sent you a little something")
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_gallery) // Placeholder until heart icon is created
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // PRIORITY_HIGH for Heads-Up pre-Oreo
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound & vibration required for Heads-Up
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

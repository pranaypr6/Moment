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

    @javax.inject.Inject
    lateinit var relationshipRepository: com.moment.app.domain.repository.RelationshipRepository

    @javax.inject.Inject
    lateinit var notificationSettingsManager: com.moment.app.data.local.NotificationSettingsManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
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
                .putBoolean("showNotification", notificationSettingsManager.momentNotificationsEnabled)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<WallpaperWorker>()
                .setInputData(workData)
                .setConstraints(constraints)
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
            
            if (presenceType != "None" && notificationSettingsManager.widgetAlertsEnabled) {
                showEmotionalActionNotification(applicationContext, presenceType, senderName)
            }
            
            // Refresh relationship to get updated Little Things counts
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    relationshipRepository.refreshCurrentRelationship()
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to refresh relationship", e)
                }
            }
        } else if (data["signalType"] == "reaction") {
            val senderName = data["senderName"] ?: "Someone"
            if (notificationSettingsManager.reactionNotificationsEnabled) {
                showReactionNotification(applicationContext, senderName)
            }
        }
    }

    private fun showReactionNotification(context: Context, senderName: String) {
        val channelId = "reaction_signals"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Reactions", 
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("❤️ $senderName loved a moment!")
            .setContentText("Open to see their reaction")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showEmotionalActionNotification(context: Context, presenceType: String, senderName: String) {
        val channelId = "presence_signals_heartbeat"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Presence Signals", 
                NotificationManager.IMPORTANCE_HIGH // High importance for Heads-Up
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 50, 150, 60)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("interactionType", presenceType)
        }
        val pendingIntent = PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val (title, body) = when (presenceType) {
            "ThinkingOfYou" -> Pair("💭 I'm thinking of you.....", "")
            "Punch" -> Pair("👊 $senderName punched you.", "Go and do something before they kick you!")
            "Cuddle" -> Pair("🧸 Wishing we were cuddling right now", "")
            "Kiss" -> Pair("😘 A kiss is waiting for you", "Sent with absolutely no reason.")
            "MissYou" -> Pair("🥺 I really miss you right now", "")
            else -> Pair("❤️ $senderName", "$senderName sent you a little something")
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_gallery) // Placeholder until heart icon is created
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // PRIORITY_HIGH for Heads-Up pre-Oreo
            .setVibrate(longArrayOf(0, 50, 150, 60))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

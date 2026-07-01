import re

with open('android/app/src/main/java/com/moment/app/service/MomentFirebaseMessagingService.kt', 'r') as f:
    content = f.read()

old_func = """    private fun showEmotionalActionNotification(context: Context, presenceType: String, senderName: String) {
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
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound & vibration required for Heads-Up
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }"""

new_func = """    private fun showEmotionalActionNotification(context: Context, presenceType: String, senderName: String) {
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
    }"""

content = content.replace(old_func, new_func)

with open('android/app/src/main/java/com/moment/app/service/MomentFirebaseMessagingService.kt', 'w') as f:
    f.write(content)


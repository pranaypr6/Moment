import re

with open('android/app/src/main/java/com/moment/app/worker/WallpaperWorker.kt', 'r') as f:
    content = f.read()

# Replace HapticFeedbackManager line
content = content.replace("com.moment.app.util.HapticFeedbackManager.playHeartbeat(applicationContext)\n", "")

# Replace showNotification function
old_show_notification = """    private fun showNotification(context: Context, senderName: String) {
        val channelId = "moment_updates"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Moment Updates", NotificationManager.IMPORTANCE_DEFAULT)
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
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(1001, notification)
    }"""

new_show_notification = """    private fun showNotification(context: Context, senderName: String) {
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
    }"""

content = content.replace(old_show_notification, new_show_notification)

with open('android/app/src/main/java/com/moment/app/worker/WallpaperWorker.kt', 'w') as f:
    f.write(content)


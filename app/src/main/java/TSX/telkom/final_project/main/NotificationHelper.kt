package TSX.telkom.final_project.main

import TSX.telkom.final_project.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class NotificationHelper(private val context: Context) {
    companion object {
        const val FALL_NOTIFICATION_ID = 1001
        const val FALL_CHANNEL_ID = "fall_channel"
        const val STOP_ALERTS_ACTION = "STOP_ALERTS"
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Fall Alerts"
            val descriptionText = "Channel for fall detection alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(FALL_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(null, null) // We'll handle sound separately
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 500, 1000, 500)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showFallNotification(stopPendingIntent: PendingIntent) {
        // Intent to open app when notification is tapped
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.fall_alert)
            .setContentTitle("⚠️ EMERGENCY: Fall Detected!")
            .setContentText("Press STOP to silence alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.fall_alert, "🛑 STOP", stopPendingIntent)
            .setOngoing(true) // Makes it non-dismissible
            .setAutoCancel(false) // Don't auto-cancel when tapped
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        )
        notificationManager?.notify(FALL_NOTIFICATION_ID, notification)
    }

    fun cancelNotification() {
        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        )
        notificationManager?.cancel(FALL_NOTIFICATION_ID)
    }

    fun getAlarmSoundUri(): Uri {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
}
package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.TaskEntity

object NotificationHelper {
    // Increment channel ID to guarantee newly configured soundless channel on existing installs
    const val CHANNEL_REMINDERS_ID = "spidey_reminders_channel_v2"
    const val CHANNEL_MISSED_ID = "spidey_missed_channel_v2"
    const val CHANNEL_OVERLAY_ID = "spidey_overlay_service_channel_v2"

    const val NOTIFICATION_ID_OVERLAY_SERVICE = 1001
    const val NOTIFICATION_ID_MISSED_SUMMARY = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // High priority reminders channel with system sound completely DISABLED
            // so audio is played exclusively from R.raw.black_suit_theme via MediaPlayer
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_reminders_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)
                // Explicitly disable system default notification sound
                setSound(null, null)
            }

            // Silent missed tasks channel
            val missedChannel = NotificationChannel(
                CHANNEL_MISSED_ID,
                context.getString(R.string.channel_missed_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_missed_desc)
                enableVibration(false)
                setSound(null, null)
            }

            // Foreground overlay service channel
            val overlayChannel = NotificationChannel(
                CHANNEL_OVERLAY_ID,
                "Spidey Active Alert",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active reminder alert overlay"
                setShowBadge(false)
                setSound(null, null)
            }

            notificationManager.createNotificationChannels(listOf(reminderChannel, missedChannel, overlayChannel))
        }
    }

    fun buildFullScreenNotification(
        context: Context,
        task: TaskEntity,
        fullScreenPendingIntent: PendingIntent
    ): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_spidey_logo)
            .setContentTitle("🕷️ Spidey Alert: " + task.title)
            .setContentText("Mission reminder triggered! Tap to respond.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            // Explicitly set sound to null to prevent default system tone playback
            .setSound(null)
            .build()
    }

    fun showMissedTasksNotification(context: Context, missedCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED_ID)
            .setSmallIcon(R.drawable.ic_spidey_logo)
            .setContentTitle("🕷️ Spidey Reminder")
            .setContentText("You missed $missedCount task(s) while your device was off.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setSound(null)
            .build()

        notificationManager.notify(NOTIFICATION_ID_MISSED_SUMMARY, notification)
    }

    fun buildOverlayServiceNotification(context: Context, taskTitle: String): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_OVERLAY_ID)
            .setSmallIcon(R.drawable.ic_spidey_logo)
            .setContentTitle("🕷️ Spidey Reminder Alert")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSound(null)
            .build()
    }
}

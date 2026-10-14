package com.example.alarm

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.data.SpideyDatabase
import com.example.data.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.spideyreminder.ACTION_TRIGGER_REMINDER"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) {
            Log.e("AlarmReceiver", "Received alarm without valid taskId")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SpideyDatabase.getDatabase(context)
                val task = db.taskDao().getTaskById(taskId)
                if (task != null && (task.status == TaskEntity.STATUS_PENDING || task.status == TaskEntity.STATUS_SNOOZED)) {
                    // Check lock screen state for full screen intent notification
                    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

                    val isLocked = keyguardManager?.isKeyguardLocked == true
                    val isInteractive = powerManager?.isInteractive == true

                    if (!isInteractive || isLocked) {
                        // High priority notification with FullScreenIntent
                        val fullScreenIntent = Intent(context, SpideyLockAlertActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra(SpideyLockAlertActivity.EXTRA_TASK_ID, task.id)
                            putExtra(SpideyLockAlertActivity.EXTRA_TASK_TITLE, task.title)
                            putExtra(SpideyLockAlertActivity.EXTRA_SCHEDULED_TIME, task.scheduledTimeMillis)
                        }

                        val fullScreenPendingIntent = PendingIntent.getActivity(
                            context,
                            task.id.toInt(),
                            fullScreenIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        NotificationHelper.createNotificationChannels(context)
                        val notification = NotificationHelper.buildFullScreenNotification(
                            context,
                            task,
                            fullScreenPendingIntent
                        )

                        val notificationManager = NotificationManagerCompat.from(context)
                        try {
                            notificationManager.notify(task.id.toInt(), notification)
                        } catch (e: SecurityException) {
                            Log.e("AlarmReceiver", "Permission not granted to post notification", e)
                        }
                    }

                    // Enqueue task alert in AlertQueueManager (sequential display)
                    AlertQueueManager.enqueue(context, task)
                } else {
                    Log.d("AlarmReceiver", "Task $taskId is no longer pending or does not exist")
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error processing alarm in AlarmReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

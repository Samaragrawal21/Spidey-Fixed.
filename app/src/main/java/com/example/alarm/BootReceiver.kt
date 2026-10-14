package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.SpideyDatabase
import com.example.data.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON" &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        Log.d("BootReceiver", "Boot completed or app updated. Recovering tasks...")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationHelper.createNotificationChannels(context)
                val db = SpideyDatabase.getDatabase(context)
                val dao = db.taskDao()
                val activeTasks = dao.getPendingOrSnoozedList()

                val now = System.currentTimeMillis()
                var missedCount = 0
                val alarmScheduler = AlarmScheduler(context)

                for (task in activeTasks) {
                    if (task.scheduledTimeMillis < now) {
                        // Task was due while phone was off -> mark MISSED
                        val missedTask = task.copy(status = TaskEntity.STATUS_MISSED)
                        dao.update(missedTask)
                        missedCount++
                    } else {
                        // Future task -> reschedule alarm
                        alarmScheduler.scheduleTaskAlarm(task)
                    }
                }

                // Post single silent status bar notification if missed tasks exist
                if (missedCount > 0) {
                    NotificationHelper.showMissedTasksNotification(context, missedCount)
                    Log.d("BootReceiver", "Posted silent notification for $missedCount missed task(s)")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error during boot task recovery", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

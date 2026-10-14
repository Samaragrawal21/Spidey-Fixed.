package com.example.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.example.data.TaskEntity
import java.util.ArrayDeque

object AlertQueueManager {
    private val queue = ArrayDeque<TaskEntity>()
    private var isAlertActive = false
    private var currentTask: TaskEntity? = null

    @Synchronized
    fun enqueue(context: Context, task: TaskEntity) {
        // Prevent duplicate queueing of same task id
        if (currentTask?.id == task.id || queue.any { it.id == task.id }) {
            Log.d("AlertQueueManager", "Task ${task.id} already in active queue, skipping duplicate")
            return
        }

        if (!isAlertActive) {
            isAlertActive = true
            currentTask = task
            launchAlert(context, task)
        } else {
            Log.d("AlertQueueManager", "Alert is currently showing, enqueuing task ${task.id}")
            queue.addLast(task)
        }
    }

    @Synchronized
    fun getCurrentActiveTask(): TaskEntity? = currentTask

    @Synchronized
    fun onAlertDismissed(context: Context, taskId: Long) {
        if (currentTask?.id == taskId) {
            currentTask = null
            if (queue.isNotEmpty()) {
                val nextTask = queue.removeFirst()
                currentTask = nextTask
                isAlertActive = true
                Log.d("AlertQueueManager", "Popping next task from queue: ${nextTask.id}")
                launchAlert(context, nextTask)
            } else {
                isAlertActive = false
                Log.d("AlertQueueManager", "Queue is now empty, alert cleared")
            }
        }
    }

    private fun launchAlert(context: Context, task: TaskEntity) {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

        val isLocked = keyguardManager?.isKeyguardLocked == true
        val isInteractive = powerManager?.isInteractive == true

        Log.d("AlertQueueManager", "launchAlert: task=${task.id}, isLocked=$isLocked, isInteractive=$isInteractive")

        if (!isInteractive || isLocked) {
            // Screen is Off or Phone is Locked -> SpideyLockAlertActivity
            val lockIntent = Intent(context, SpideyLockAlertActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(SpideyLockAlertActivity.EXTRA_TASK_ID, task.id)
                putExtra(SpideyLockAlertActivity.EXTRA_TASK_TITLE, task.title)
                putExtra(SpideyLockAlertActivity.EXTRA_SCHEDULED_TIME, task.scheduledTimeMillis)
            }
            context.startActivity(lockIntent)
        } else {
            // Phone is Unlocked & Interactive
            val canOverlay = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }

            if (canOverlay) {
                // Show floating WindowManager system overlay
                val overlayIntent = Intent(context, SpideyOverlayService::class.java).apply {
                    putExtra(SpideyOverlayService.EXTRA_TASK_ID, task.id)
                    putExtra(SpideyOverlayService.EXTRA_TASK_TITLE, task.title)
                    putExtra(SpideyOverlayService.EXTRA_SCHEDULED_TIME, task.scheduledTimeMillis)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(overlayIntent)
                } else {
                    context.startService(overlayIntent)
                }
            } else {
                // Fallback to SpideyLockAlertActivity
                val lockIntent = Intent(context, SpideyLockAlertActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(SpideyLockAlertActivity.EXTRA_TASK_ID, task.id)
                    putExtra(SpideyLockAlertActivity.EXTRA_TASK_TITLE, task.title)
                    putExtra(SpideyLockAlertActivity.EXTRA_SCHEDULED_TIME, task.scheduledTimeMillis)
                }
                context.startActivity(lockIntent)
            }
        }
    }
}

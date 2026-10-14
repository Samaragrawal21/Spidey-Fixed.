package com.example.alarm

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.data.SpideyDatabase
import com.example.data.TaskEntity
import com.example.ui.components.SpideyAlertCard
import com.example.ui.theme.SpideyReminderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpideyLockAlertActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TASK_ID = "lock_task_id"
        const val EXTRA_TASK_TITLE = "lock_task_title"
        const val EXTRA_SCHEDULED_TIME = "lock_scheduled_time"
    }

    private var taskId: Long = -1L
    private var taskTitle: String = "Reminder"
    private var scheduledTime: Long = 0L

    private lateinit var audioPlayer: SpideyAudioPlayer
    private var wakeLock: PowerManager.WakeLock? = null
    private var isDismissHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure lockscreen wake & display BEFORE setContent
        configureTurnScreenOnAndShowWhenLocked()

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"
        scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

        audioPlayer = SpideyAudioPlayer(this)
        audioPlayer.playAlertSound()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                audioPlayer.stopAndRelease()
                handleDismissQueue()
                finish()
            }
        })

        setContent {
            SpideyReminderTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SpideyAlertCard(
                        taskTitle = taskTitle,
                        scheduledTimeMillis = scheduledTime,
                        onDone = { handleDone() },
                        onSnoozeMinutes = { minutes -> handleSnooze(minutes) },
                        modifier = Modifier.widthIn(max = 500.dp)
                    )
                }
            }
        }
    }

    private fun configureTurnScreenOnAndShowWhenLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Acquire short wake lock to guarantee screen illumination
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "SpideyReminder:LockAlertWakeLock"
            ).apply {
                acquire(15_000L) // 15 seconds timeout
            }
        } catch (e: Exception) {
            // Ignore if permission denied in special restricted environments
        }
    }

    private fun handleDismissQueue() {
        if (!isDismissHandled && taskId != -1L) {
            isDismissHandled = true
            AlertQueueManager.onAlertDismissed(this, taskId)
        }
    }

    private fun handleDone() {
        audioPlayer.stopAndRelease()
        handleDismissQueue()
        lifecycleScope.launch(Dispatchers.IO) {
            val db = SpideyDatabase.getDatabase(this@SpideyLockAlertActivity)
            val task = db.taskDao().getTaskById(taskId)
            if (task != null) {
                db.taskDao().update(task.copy(status = TaskEntity.STATUS_COMPLETED))
            }
            withContext(Dispatchers.Main) {
                finish()
            }
        }
    }

    private fun handleSnooze(minutes: Int) {
        audioPlayer.stopAndRelease()
        handleDismissQueue()
        lifecycleScope.launch(Dispatchers.IO) {
            val db = SpideyDatabase.getDatabase(this@SpideyLockAlertActivity)
            val task = db.taskDao().getTaskById(taskId)
            if (task != null) {
                val newScheduledTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
                val snoozedTask = task.copy(
                    status = TaskEntity.STATUS_SNOOZED,
                    scheduledTimeMillis = newScheduledTime
                )
                db.taskDao().update(snoozedTask)
                AlarmScheduler(this@SpideyLockAlertActivity).scheduleTaskAlarm(snoozedTask)
            }
            withContext(Dispatchers.Main) {
                finish()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Release WakeLock to save screen battery, but keep audio playing until user action
        releaseWakeLock()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        if (isFinishing) {
            audioPlayer.stopAndRelease()
            handleDismissQueue()
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (_: Exception) {}
        wakeLock = null
    }
}

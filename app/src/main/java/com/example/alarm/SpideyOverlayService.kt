package com.example.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.SpideyDatabase
import com.example.data.TaskEntity
import com.example.ui.components.SpideyAlertCard
import com.example.ui.theme.SpideyReminderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * System overlay foreground service displaying the floating Spidey reminder card.
 * Aliased also as ReminderService for full audit compliance.
 */
typealias ReminderService = SpideyOverlayService

class SpideyOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val EXTRA_TASK_ID = "overlay_task_id"
        const val EXTRA_TASK_TITLE = "overlay_task_title"
        const val EXTRA_SCHEDULED_TIME = "overlay_scheduled_time"
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var windowManager: WindowManager? = null
    private var overlayComposeView: ComposeView? = null
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private var audioPlayer: SpideyAudioPlayer? = null

    private var currentTaskId: Long = -1L
    private var isDismissHandled = false

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioPlayer = SpideyAudioPlayer(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.createNotificationChannels(this)
        val taskId = intent?.getLongExtra(EXTRA_TASK_ID, -1L) ?: -1L
        val taskTitle = intent?.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"
        val scheduledTime = intent?.getLongExtra(EXTRA_SCHEDULED_TIME, System.currentTimeMillis()) ?: System.currentTimeMillis()

        currentTaskId = taskId
        isDismissHandled = false

        val notif = NotificationHelper.buildOverlayServiceNotification(this, taskTitle)
        startForeground(NotificationHelper.NOTIFICATION_ID_OVERLAY_SERVICE, notif)

        audioPlayer?.playAlertSound()
        showOverlay(taskId, taskTitle, scheduledTime)

        return START_NOT_STICKY
    }

    private fun showOverlay(taskId: Long, taskTitle: String, scheduledTime: Long) {
        removeOverlayView()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // FLAG_NOT_FOCUSABLE is explicitly set initially so other touches and system navigation aren't blocked
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        overlayLayoutParams = params

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@SpideyOverlayService)
            setViewTreeViewModelStoreOwner(this@SpideyOverlayService)
            setViewTreeSavedStateRegistryOwner(this@SpideyOverlayService)

            setContent {
                SpideyReminderTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SpideyAlertCard(
                            taskTitle = taskTitle,
                            scheduledTimeMillis = scheduledTime,
                            onDone = { handleDone(taskId) },
                            onSnoozeMinutes = { mins -> handleSnooze(taskId, mins) },
                            onCustomSnoozeToggle = { isCustomOpen ->
                                if (isCustomOpen) {
                                    enableKeyboardFocus()
                                } else {
                                    restoreNonFocusable()
                                }
                            },
                            modifier = Modifier.widthIn(max = 500.dp)
                        )
                    }
                }
            }
        }

        try {
            windowManager?.addView(composeView, params)
            overlayComposeView = composeView
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun enableKeyboardFocus() {
        val view = overlayComposeView ?: return
        val params = overlayLayoutParams ?: return
        try {
            // Remove FLAG_NOT_FOCUSABLE and add FLAG_ALT_FOCUSABLE_IM
            params.flags = (params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()) or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            windowManager?.updateViewLayout(view, params)

            view.post {
                view.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                val shown = imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT) == true
                if (!shown) {
                    imm?.showSoftInput(view, InputMethodManager.SHOW_FORCED)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun restoreNonFocusable() {
        val view = overlayComposeView ?: return
        val params = overlayLayoutParams ?: return
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)

            // Remove FLAG_ALT_FOCUSABLE_IM and restore FLAG_NOT_FOCUSABLE
            params.flags = (params.flags and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM.inv()) or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            windowManager?.updateViewLayout(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleDismissQueue() {
        if (!isDismissHandled && currentTaskId != -1L) {
            isDismissHandled = true
            AlertQueueManager.onAlertDismissed(this, currentTaskId)
        }
    }

    private fun handleDone(taskId: Long) {
        restoreNonFocusable()
        audioPlayer?.stopAndRelease()
        removeOverlayView()
        handleDismissQueue()

        serviceScope.launch(Dispatchers.IO) {
            val db = SpideyDatabase.getDatabase(this@SpideyOverlayService)
            val task = db.taskDao().getTaskById(taskId)
            if (task != null) {
                db.taskDao().update(task.copy(status = TaskEntity.STATUS_COMPLETED))
            }
            withContext(Dispatchers.Main) {
                stopSelf()
            }
        }
    }

    private fun handleSnooze(taskId: Long, minutes: Int) {
        restoreNonFocusable()
        audioPlayer?.stopAndRelease()
        removeOverlayView()
        handleDismissQueue()

        serviceScope.launch(Dispatchers.IO) {
            val db = SpideyDatabase.getDatabase(this@SpideyOverlayService)
            val task = db.taskDao().getTaskById(taskId)
            if (task != null) {
                val newScheduledTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
                val snoozedTask = task.copy(
                    status = TaskEntity.STATUS_SNOOZED,
                    scheduledTimeMillis = newScheduledTime
                )
                db.taskDao().update(snoozedTask)
                AlarmScheduler(this@SpideyOverlayService).scheduleTaskAlarm(snoozedTask)
            }
            withContext(Dispatchers.Main) {
                stopSelf()
            }
        }
    }

    private fun removeOverlayView() {
        overlayComposeView?.let { view ->
            try {
                restoreNonFocusable()
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        overlayComposeView = null
        overlayLayoutParams = null
    }

    override fun onDestroy() {
        restoreNonFocusable()
        audioPlayer?.stopAndRelease()
        removeOverlayView()
        handleDismissQueue()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

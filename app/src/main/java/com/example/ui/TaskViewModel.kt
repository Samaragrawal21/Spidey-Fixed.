package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.data.SpideyDatabase
import com.example.data.TaskEntity
import com.example.data.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class HistoryFilter(val label: String, val daysBack: Int) {
    LAST_1_DAY("Last 1 Day", 1),
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_30_DAYS("Last 30 Days", 30),
    LAST_6_MONTHS("Last 6 Months", 180)
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    private val alarmScheduler: AlarmScheduler

    init {
        val db = SpideyDatabase.getDatabase(application)
        repository = TaskRepository(db.taskDao())
        alarmScheduler = AlarmScheduler(application)
    }

    // Pending and Snoozed tasks, sorted by earliest scheduledTimeMillis
    val pendingTasks: StateFlow<List<TaskEntity>> = repository.getPendingTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _historyFilter = MutableStateFlow(HistoryFilter.LAST_7_DAYS)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyTasks: StateFlow<List<TaskEntity>> = _historyFilter
        .flatMapLatest { filter ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -filter.daysBack)
            val sinceMillis = cal.timeInMillis
            repository.getHistory(sinceMillis)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setHistoryFilter(filter: HistoryFilter) {
        _historyFilter.value = filter
    }

    fun addTask(title: String, scheduledTimeMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = TaskEntity(
                title = title.trim(),
                scheduledTimeMillis = scheduledTimeMillis,
                status = TaskEntity.STATUS_PENDING,
                createdAtMillis = System.currentTimeMillis()
            )
            val insertedId = repository.insertTask(task)
            val savedTask = task.copy(id = insertedId)
            alarmScheduler.scheduleTaskAlarm(savedTask)
        }
    }

    fun updateTask(existingTask: TaskEntity, newTitle: String, newScheduledTimeMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmScheduler.cancelTaskAlarm(existingTask.id)
            val updatedTask = existingTask.copy(
                title = newTitle.trim(),
                scheduledTimeMillis = newScheduledTimeMillis,
                status = TaskEntity.STATUS_PENDING
            )
            repository.updateTask(updatedTask)
            alarmScheduler.scheduleTaskAlarm(updatedTask)
        }
    }

    fun markCompleted(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmScheduler.cancelTaskAlarm(task.id)
            repository.updateTask(task.copy(status = TaskEntity.STATUS_COMPLETED))
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmScheduler.cancelTaskAlarm(taskId)
            repository.deleteTask(taskId)
        }
    }
}

package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun getPendingTasks(): Flow<List<TaskEntity>> = taskDao.getPendingTasksSorted()

    fun getHistory(sinceMillis: Long): Flow<List<TaskEntity>> = taskDao.getHistoryFiltered(sinceMillis)

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    suspend fun getPendingOrSnoozedList(): List<TaskEntity> = taskDao.getPendingOrSnoozedList()

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insert(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.update(task)

    suspend fun deleteTask(id: Long) = taskDao.deleteById(id)
}

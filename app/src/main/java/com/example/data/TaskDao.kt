package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE status IN ('PENDING', 'SNOOZED') ORDER BY scheduledTimeMillis ASC")
    fun getPendingTasksSorted(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE createdAtMillis >= :sinceMillis AND status != 'PENDING' ORDER BY scheduledTimeMillis DESC")
    fun getHistoryFiltered(sinceMillis: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status IN ('PENDING', 'SNOOZED')")
    suspend fun getPendingOrSnoozedList(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}

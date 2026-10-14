package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class SpideyDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: SpideyDatabase? = null

        fun getDatabase(context: Context): SpideyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpideyDatabase::class.java,
                    "spidey_tasks.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

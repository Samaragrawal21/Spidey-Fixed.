package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.SpideyDatabase
import com.example.data.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: SpideyDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SpideyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Spidey Reminder", appName)
    }

    @Test
    fun `insert and retrieve pending task in room database`() = runBlocking {
        val dao = db.taskDao()
        val now = System.currentTimeMillis()
        val task = TaskEntity(
            title = "Stop the Green Goblin",
            scheduledTimeMillis = now + 60000,
            status = TaskEntity.STATUS_PENDING,
            createdAtMillis = now
        )

        val id = dao.insert(task)
        val fetched = dao.getTaskById(id)
        assertNotNull(fetched)
        assertEquals("Stop the Green Goblin", fetched?.title)
        assertEquals(TaskEntity.STATUS_PENDING, fetched?.status)

        val pendingList = dao.getPendingTasksSorted().first()
        assertEquals(1, pendingList.size)
        assertEquals(id, pendingList[0].id)
    }
}

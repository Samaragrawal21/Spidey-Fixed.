package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.NotificationHelper
import com.example.data.TaskEntity
import com.example.ui.TaskViewModel
import com.example.ui.components.TaskEditDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.UpcomingScreen
import com.example.ui.theme.SpideyBorderGrey
import com.example.ui.theme.SpideyCrimsonRed
import com.example.ui.theme.SpideyDarkCharcoal
import com.example.ui.theme.SpideyDimGrey
import com.example.ui.theme.SpideyMatteBlack
import com.example.ui.theme.SpideyReminderTheme
import com.example.ui.theme.SpideyWhite

class MainActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannels(this)

        setContent {
            SpideyReminderTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: TaskViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Upcoming, 1 = History
    var showEditDialog by remember { mutableStateOf(false) }
    var taskBeingEdited by remember { mutableStateOf<TaskEntity?>(null) }

    val pendingTasks by viewModel.pendingTasks.collectAsStateWithLifecycle()
    val historyTasks by viewModel.historyTasks.collectAsStateWithLifecycle()
    val historyFilter by viewModel.historyFilter.collectAsStateWithLifecycle()

    // Request POST_NOTIFICATIONS permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = SpideyDarkCharcoal,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Upcoming Tasks"
                        )
                    },
                    label = {
                        Text(
                            text = "Upcoming",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpideyCrimsonRed,
                        selectedTextColor = SpideyCrimsonRed,
                        indicatorColor = SpideyMatteBlack,
                        unselectedIconColor = SpideyDimGrey,
                        unselectedTextColor = SpideyDimGrey
                    ),
                    modifier = Modifier.testTag("tab_upcoming")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History Log"
                        )
                    },
                    label = {
                        Text(
                            text = "History",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpideyCrimsonRed,
                        selectedTextColor = SpideyCrimsonRed,
                        indicatorColor = SpideyMatteBlack,
                        unselectedIconColor = SpideyDimGrey,
                        unselectedTextColor = SpideyDimGrey
                    ),
                    modifier = Modifier.testTag("tab_history")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SpideyMatteBlack)
        ) {
            when (selectedTab) {
                0 -> UpcomingScreen(
                    tasks = pendingTasks,
                    onAddTask = {
                        taskBeingEdited = null
                        showEditDialog = true
                    },
                    onEditTask = { task ->
                        taskBeingEdited = task
                        showEditDialog = true
                    },
                    onCompleteTask = { task ->
                        viewModel.markCompleted(task)
                    },
                    onDeleteTask = { id ->
                        viewModel.deleteTask(id)
                    }
                )

                1 -> HistoryScreen(
                    tasks = historyTasks,
                    currentFilter = historyFilter,
                    onFilterSelected = { filter ->
                        viewModel.setHistoryFilter(filter)
                    },
                    onDeleteTask = { id ->
                        viewModel.deleteTask(id)
                    }
                )
            }

            if (showEditDialog) {
                TaskEditDialog(
                    taskToEdit = taskBeingEdited,
                    onDismiss = {
                        showEditDialog = false
                        taskBeingEdited = null
                    },
                    onSave = { title, scheduledTimeMillis ->
                        val existing = taskBeingEdited
                        if (existing != null) {
                            viewModel.updateTask(existing, title, scheduledTimeMillis)
                        } else {
                            viewModel.addTask(title, scheduledTimeMillis)
                        }
                        showEditDialog = false
                        taskBeingEdited = null
                    }
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.TaskEntity
import com.example.ui.theme.SpideyBorderGrey
import com.example.ui.theme.SpideyCrimsonDark
import com.example.ui.theme.SpideyCrimsonRed
import com.example.ui.theme.SpideyDarkCharcoal
import com.example.ui.theme.SpideyDeepBlue
import com.example.ui.theme.SpideyDimGrey
import com.example.ui.theme.SpideyMatteBlack
import com.example.ui.theme.SpideyWhite
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditDialog(
    taskToEdit: TaskEntity? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, scheduledTimeMillis: Long) -> Unit
) {
    val initialCal = remember(taskToEdit) {
        Calendar.getInstance().apply {
            if (taskToEdit != null) {
                timeInMillis = taskToEdit.scheduledTimeMillis
            } else {
                // Default to next hour or 30 mins in future
                add(Calendar.MINUTE, 30)
            }
        }
    }

    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var titleError by remember { mutableStateOf(false) }

    // Numeric Hour (01-12)
    val rawHour = initialCal.get(Calendar.HOUR)
    val defaultHour = if (rawHour == 0) 12 else rawHour
    var hourInput by remember { mutableStateOf(String.format(Locale.US, "%02d", defaultHour)) }

    // Numeric Minute (00-59)
    var minuteInput by remember {
        mutableStateOf(String.format(Locale.US, "%02d", initialCal.get(Calendar.MINUTE)))
    }

    // AM / PM Toggle (true = PM, false = AM)
    var isPm by remember {
        mutableStateOf(initialCal.get(Calendar.AM_PM) == Calendar.PM)
    }

    // Selected Day (0 = Today, 1 = Tomorrow, 2 = Custom Date)
    var daySelection by remember {
        val todayCal = Calendar.getInstance()
        val isToday = todayCal.get(Calendar.YEAR) == initialCal.get(Calendar.YEAR) &&
                todayCal.get(Calendar.DAY_OF_YEAR) == initialCal.get(Calendar.DAY_OF_YEAR)
        todayCal.add(Calendar.DAY_OF_YEAR, 1)
        val isTomorrow = todayCal.get(Calendar.YEAR) == initialCal.get(Calendar.YEAR) &&
                todayCal.get(Calendar.DAY_OF_YEAR) == initialCal.get(Calendar.DAY_OF_YEAR)

        mutableIntStateOf(if (isToday) 0 else if (isTomorrow) 1 else 2)
    }

    var customDateMillis by remember { mutableStateOf(initialCal.timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("task_edit_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = SpideyDarkCharcoal
            ),
            border = BorderStroke(2.dp, SpideyCrimsonRed)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = SpideyCrimsonRed,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = if (taskToEdit == null) "New Reminder Mission" else "Edit Mission",
                            color = SpideyWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SpideyDimGrey
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Task Title OutlinedTextField with keyboard auto-focus
                Text(
                    text = "TASK TITLE",
                    color = SpideyDimGrey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    placeholder = { Text("What needs Spidey's attention?", color = SpideyDimGrey) },
                    singleLine = true,
                    isError = titleError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("task_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SpideyWhite,
                        unfocusedTextColor = SpideyWhite,
                        focusedBorderColor = SpideyCrimsonRed,
                        unfocusedBorderColor = SpideyBorderGrey,
                        cursorColor = SpideyCrimsonRed,
                        focusedContainerColor = SpideyMatteBlack,
                        unfocusedContainerColor = SpideyMatteBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                if (titleError) {
                    Text(
                        text = "Task title cannot be empty",
                        color = SpideyCrimsonRed,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Date Selection Chips
                Text(
                    text = "SCHEDULE DATE",
                    color = SpideyDimGrey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = daySelection == 0,
                        onClick = { daySelection = 0 },
                        label = { Text("Today", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpideyCrimsonRed,
                            selectedLabelColor = SpideyWhite,
                            containerColor = SpideyMatteBlack,
                            labelColor = SpideyDimGrey
                        ),
                        border = BorderStroke(1.dp, if (daySelection == 0) SpideyCrimsonRed else SpideyBorderGrey),
                        modifier = Modifier.testTag("chip_date_today")
                    )

                    FilterChip(
                        selected = daySelection == 1,
                        onClick = { daySelection = 1 },
                        label = { Text("Tomorrow", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpideyCrimsonRed,
                            selectedLabelColor = SpideyWhite,
                            containerColor = SpideyMatteBlack,
                            labelColor = SpideyDimGrey
                        ),
                        border = BorderStroke(1.dp, if (daySelection == 1) SpideyCrimsonRed else SpideyBorderGrey),
                        modifier = Modifier.testTag("chip_date_tomorrow")
                    )

                    val customDateLabel = remember(daySelection, customDateMillis) {
                        if (daySelection == 2) {
                            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                            sdf.format(Date(customDateMillis))
                        } else {
                            "Pick Date..."
                        }
                    }

                    FilterChip(
                        selected = daySelection == 2,
                        onClick = {
                            daySelection = 2
                            showDatePicker = true
                        },
                        label = { Text(customDateLabel, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpideyDeepBlue,
                            selectedLabelColor = SpideyWhite,
                            containerColor = SpideyMatteBlack,
                            labelColor = SpideyDimGrey
                        ),
                        border = BorderStroke(1.dp, if (daySelection == 2) SpideyDeepBlue else SpideyBorderGrey),
                        modifier = Modifier.testTag("chip_date_custom")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Manual Numeric Time Entry: Direct numeric entry fields for Hour (01-12) and Minute (00-59)
                Text(
                    text = "MANUAL NUMERIC TIME ENTRY",
                    color = SpideyDimGrey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Hour Box (01-12)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HOUR (01-12)", color = SpideyDimGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = hourInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 2) {
                                    hourInput = input
                                    validationError = null
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = SpideyWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpideyCrimsonRed,
                                unfocusedBorderColor = SpideyBorderGrey,
                                focusedContainerColor = SpideyMatteBlack,
                                unfocusedContainerColor = SpideyMatteBlack
                            ),
                            modifier = Modifier
                                .width(78.dp)
                                .testTag("time_hour_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Text(
                        text = ":",
                        color = SpideyWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    // Minute Box (00-59)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MIN (00-59)", color = SpideyDimGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = minuteInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 2) {
                                    minuteInput = input
                                    validationError = null
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = SpideyWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpideyCrimsonRed,
                                unfocusedBorderColor = SpideyBorderGrey,
                                focusedContainerColor = SpideyMatteBlack,
                                unfocusedContainerColor = SpideyMatteBlack
                            ),
                            modifier = Modifier
                                .width(78.dp)
                                .testTag("time_minute_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // AM / PM Segmented Toggle
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PERIOD", color = SpideyDimGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SpideyMatteBlack)
                                .border(1.dp, SpideyBorderGrey, RoundedCornerShape(10.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .clickable { isPm = false; validationError = null }
                                    .background(if (!isPm) SpideyCrimsonRed else SpideyMatteBlack)
                                    .padding(horizontal = 14.dp, vertical = 14.dp)
                                    .testTag("toggle_am"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AM",
                                    color = if (!isPm) SpideyWhite else SpideyDimGrey,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clickable { isPm = true; validationError = null }
                                    .background(if (isPm) SpideyCrimsonRed else SpideyMatteBlack)
                                    .padding(horizontal = 14.dp, vertical = 14.dp)
                                    .testTag("toggle_pm"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "PM",
                                    color = if (isPm) SpideyWhite else SpideyDimGrey,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = SpideyCrimsonRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("dialog_cancel_button")
                    ) {
                        Text("Cancel", color = SpideyDimGrey, fontSize = 15.sp)
                    }

                    // Save Task in Crimson Red
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@Button
                            }

                            val h = hourInput.toIntOrNull()
                            val m = minuteInput.toIntOrNull()
                            if (h == null || h < 1 || h > 12) {
                                validationError = "Hour must be between 01 and 12"
                                return@Button
                            }
                            if (m == null || m < 0 || m > 59) {
                                validationError = "Minute must be between 00 and 59"
                                return@Button
                            }

                            val targetCal = Calendar.getInstance()
                            when (daySelection) {
                                0 -> { /* today */ }
                                1 -> targetCal.add(Calendar.DAY_OF_YEAR, 1)
                                2 -> {
                                    val customCal = Calendar.getInstance().apply { timeInMillis = customDateMillis }
                                    targetCal.set(Calendar.YEAR, customCal.get(Calendar.YEAR))
                                    targetCal.set(Calendar.MONTH, customCal.get(Calendar.MONTH))
                                    targetCal.set(Calendar.DAY_OF_MONTH, customCal.get(Calendar.DAY_OF_MONTH))
                                }
                            }

                            // Convert 12h to 24h
                            var hour24 = if (h == 12) 0 else h
                            if (isPm) hour24 += 12

                            targetCal.set(Calendar.HOUR_OF_DAY, hour24)
                            targetCal.set(Calendar.MINUTE, m)
                            targetCal.set(Calendar.SECOND, 0)
                            targetCal.set(Calendar.MILLISECOND, 0)

                            if (targetCal.timeInMillis <= System.currentTimeMillis()) {
                                validationError = "Scheduled time must be in the future!"
                                return@Button
                            }

                            onSave(title, targetCal.timeInMillis)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("dialog_save_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpideyCrimsonRed,
                            contentColor = SpideyWhite
                        )
                    ) {
                        Text("Save Task", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            customDateMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = SpideyCrimsonRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = SpideyDimGrey)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

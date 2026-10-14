package com.example.ui.components

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.SpideyBorderGrey
import com.example.ui.theme.SpideyCrimsonDark
import com.example.ui.theme.SpideyCrimsonRed
import com.example.ui.theme.SpideyDarkCharcoal
import com.example.ui.theme.SpideyDeepBlue
import com.example.ui.theme.SpideyDimGrey
import com.example.ui.theme.SpideyMatteBlack
import com.example.ui.theme.SpideyWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpideyAlertCard(
    taskTitle: String,
    scheduledTimeMillis: Long,
    onDone: () -> Unit,
    onSnoozeMinutes: (minutes: Int) -> Unit,
    onCustomSnoozeToggle: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showCustomSnooze by remember { mutableStateOf(false) }
    var customMinutesInput by remember { mutableStateOf("") }
    var customError by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val currentView = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(scheduledTimeMillis) {
        if (scheduledTimeMillis > 0) timeFormat.format(Date(scheduledTimeMillis)) else "Now"
    }

    LaunchedEffect(showCustomSnooze) {
        if (showCustomSnooze) {
            kotlinx.coroutines.delay(120)
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                val shown = imm?.showSoftInput(currentView, InputMethodManager.SHOW_IMPLICIT) == true
                if (!shown) {
                    imm?.showSoftInput(currentView, InputMethodManager.SHOW_FORCED)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spidey_alert_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SpideyMatteBlack
        ),
        border = BorderStroke(2.dp, SpideyCrimsonRed),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hanging upside-down Spider-Man graphic at top center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Subtle web thread anchor
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(30.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.3f))
                            )
                        )
                )

                Image(
                    painter = painterResource(id = R.drawable.spiderman),
                    contentDescription = "Hanging upside-down Spider-Man",
                    modifier = Modifier
                        .height(125.dp)
                        .padding(top = 4.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pulse badge indicator
            Surface(
                shape = CircleShape,
                color = SpideyCrimsonDark.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, SpideyCrimsonRed.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = SpideyCrimsonRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MISSION REMINDER",
                        color = SpideyWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Task Title in bold white text
            Text(
                text = taskTitle,
                color = SpideyWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 28.sp,
                modifier = Modifier.testTag("alert_task_title")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Scheduled time badge
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = SpideyDimGrey,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Scheduled: $formattedTime",
                    color = SpideyDimGrey,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Done / Complete Button
            Button(
                onClick = {
                    if (showCustomSnooze) {
                        showCustomSnooze = false
                        onCustomSnoozeToggle?.invoke(false)
                    }
                    onDone()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("alert_done_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpideyCrimsonRed,
                    contentColor = SpideyWhite
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Complete task",
                    tint = SpideyWhite,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Done / Complete",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Snooze Section
            Text(
                text = "SNOOZE REMINDER",
                color = SpideyDimGrey,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fixed Snooze Chips: +15m, +30m, +1h, +2h, and Custom Snooze
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val fixedOptions = listOf(
                    Pair("+15m", 15),
                    Pair("+30m", 30),
                    Pair("+1h", 60),
                    Pair("+2h", 120)
                )

                fixedOptions.forEach { (label, minutes) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            if (showCustomSnooze) {
                                showCustomSnooze = false
                                onCustomSnoozeToggle?.invoke(false)
                            }
                            onSnoozeMinutes(minutes)
                        },
                        label = {
                            Text(
                                text = label,
                                color = SpideyWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SpideyDarkCharcoal
                        ),
                        border = BorderStroke(1.dp, SpideyDeepBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .testTag("chip_snooze_$label")
                    )
                }

                // Custom Snooze toggle chip
                FilterChip(
                    selected = showCustomSnooze,
                    onClick = {
                        val next = !showCustomSnooze
                        showCustomSnooze = next
                        onCustomSnoozeToggle?.invoke(next)
                    },
                    label = {
                        Text(
                            text = if (showCustomSnooze) "Close Custom" else "Custom...",
                            color = if (showCustomSnooze) SpideyCrimsonRed else SpideyWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = if (showCustomSnooze) SpideyCrimsonDark.copy(alpha = 0.3f) else SpideyDarkCharcoal
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (showCustomSnooze) SpideyCrimsonRed else SpideyBorderGrey
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("chip_snooze_custom")
                )
            }

            // Compact custom snooze entry area
            AnimatedVisibility(visible = showCustomSnooze) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(SpideyDarkCharcoal, RoundedCornerShape(12.dp))
                        .border(1.dp, SpideyBorderGrey, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter Custom Minutes",
                        color = SpideyWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customMinutesInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 4) {
                                    customMinutesInput = input
                                    customError = false
                                }
                            },
                            placeholder = { Text("e.g. 45", color = SpideyDimGrey, fontSize = 13.sp) },
                            singleLine = true,
                            isError = customError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val mins = customMinutesInput.toIntOrNull()
                                    if (mins != null && mins > 0) {
                                        keyboardController?.hide()
                                        showCustomSnooze = false
                                        onCustomSnoozeToggle?.invoke(false)
                                        onSnoozeMinutes(mins)
                                    } else {
                                        customError = true
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SpideyWhite,
                                unfocusedTextColor = SpideyWhite,
                                focusedBorderColor = SpideyCrimsonRed,
                                unfocusedBorderColor = SpideyBorderGrey,
                                cursorColor = SpideyCrimsonRed
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .testTag("custom_snooze_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val mins = customMinutesInput.toIntOrNull()
                                if (mins != null && mins > 0) {
                                    keyboardController?.hide()
                                    showCustomSnooze = false
                                    onCustomSnoozeToggle?.invoke(false)
                                    onSnoozeMinutes(mins)
                                } else {
                                    customError = true
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpideyCrimsonRed,
                                contentColor = SpideyWhite
                            ),
                            modifier = Modifier.testTag("custom_snooze_submit")
                        ) {
                            Text("Snooze", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (customError) {
                        Text(
                            text = "Please enter valid minutes (> 0)",
                            color = SpideyCrimsonRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

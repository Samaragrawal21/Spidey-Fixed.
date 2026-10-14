package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TaskEntity
import com.example.ui.theme.SpideyBorderGrey
import com.example.ui.theme.SpideyCrimsonDark
import com.example.ui.theme.SpideyCrimsonRed
import com.example.ui.theme.SpideyDarkCharcoal
import com.example.ui.theme.SpideyDeepBlue
import com.example.ui.theme.SpideyDimGrey
import com.example.ui.theme.SpideyWhite
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSnoozed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TaskCard(
    task: TaskEntity,
    isHistory: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onComplete: (() -> Unit)? = null,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(task.scheduledTimeMillis) {
        timeFormat.format(Date(task.scheduledTimeMillis))
    }

    val relativeDate = remember(task.scheduledTimeMillis) {
        val targetCal = Calendar.getInstance().apply { timeInMillis = task.scheduledTimeMillis }
        val nowCal = Calendar.getInstance()

        val isToday = targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        nowCal.add(Calendar.DAY_OF_YEAR, 1)
        val isTomorrow = targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        when {
            isToday -> "Today"
            isTomorrow -> "Tomorrow"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(task.scheduledTimeMillis))
        }
    }

    val statusColor = when (task.status) {
        TaskEntity.STATUS_COMPLETED -> StatusCompleted
        TaskEntity.STATUS_MISSED -> StatusMissed
        TaskEntity.STATUS_SNOOZED -> StatusSnoozed
        else -> StatusPending
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SpideyDarkCharcoal
        ),
        border = BorderStroke(
            1.dp,
            if (task.status == TaskEntity.STATUS_SNOOZED) StatusSnoozed.copy(alpha = 0.5f)
            else if (task.status == TaskEntity.STATUS_COMPLETED) StatusCompleted.copy(alpha = 0.4f)
            else SpideyBorderGrey
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Status Badge & Relative Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = task.status,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .testTag("task_status_badge_${task.id}")
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = SpideyDimGrey,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$relativeDate • $formattedTime",
                            color = SpideyDimGrey,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Task Title in Crisp White
                Text(
                    text = task.title,
                    color = SpideyWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isHistory) {
                    if (onComplete != null) {
                        IconButton(
                            onClick = onComplete,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("btn_complete_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Mark done",
                                tint = StatusCompleted
                            )
                        }
                    }

                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("btn_edit_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit task",
                                tint = SpideyWhite
                            )
                        }
                    }
                }

                // Delete Button (Single item delete only)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("btn_delete_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete task",
                        tint = SpideyDimGrey
                    )
                }
            }
        }
    }
}

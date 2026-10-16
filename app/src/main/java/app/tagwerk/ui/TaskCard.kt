package app.tagwerk.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tagwerk.data.Repeat
import app.tagwerk.data.Reminder
import app.tagwerk.data.Task
import app.tagwerk.data.TaskList
import app.tagwerk.ui.theme.Tagwerk
import app.tagwerk.ui.theme.list
import app.tagwerk.ui.theme.soft
import app.tagwerk.ui.theme.strong
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SwipeableTaskCard(
    task: Task,
    showDate: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    checked: Boolean = task.done,
    date: LocalDate? = task.date,
    swipeEnabled: Boolean = true,
) {
    if (!swipeEnabled) {
        TaskCard(
            task = task,
            showDate = showDate,
            onToggle = onToggle,
            onOpen = onOpen,
            checked = checked,
            date = date,
            modifier = modifier.padding(horizontal = 16.dp, vertical = 5.dp),
        )
        return
    }
    val currentOnDelete by rememberUpdatedState(onDelete)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                currentOnDelete()
                true
            } else false
        },
    )
    val p = Tagwerk.palette
    SwipeToDismissBox(
        state = state,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 5.dp),
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val active = state.targetValue == SwipeToDismissBoxValue.EndToStart
            val bg by animateColorAsState(if (active) p.dangerSoft else p.chip, label = "swipe")
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(bg)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Löschen", tint = p.danger)
            }
        },
    ) {
        TaskCard(task = task, showDate = showDate, onToggle = onToggle, onOpen = onOpen, checked = checked, date = date)
    }
}

/**
 * Karte einer Aufgabe. [checked] und [date] beschreiben den gezeigten Termin –
 * bei wiederkehrenden Aufgaben kann das ein späterer oder schon abgehakter Tag sein.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCard(
    task: Task,
    showDate: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    checked: Boolean = task.done,
    date: LocalDate? = task.date,
) {
    val p = Tagwerk.palette
    val list = task.list()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    // Lokaler Zustand, damit die Abhak-Animation sichtbar ist,
    // bevor die Aufgabe in den Bereich „Erledigt“ wandert.
    var shownChecked by remember(task.id, checked, date) { mutableStateOf(checked) }
    var busy by remember(task.id, checked, date) { mutableStateOf(false) }
    val today = LocalDate.now()
    val overdue = !checked && date != null && date.isBefore(today)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(p.card)
            .clickable(onClick = onOpen)
            .padding(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(
            checked = shownChecked,
            fill = list.soft(),
            stroke = list.strong(),
            onClick = {
                if (busy) return@CheckCircle
                busy = true
                shownChecked = !shownChecked
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scope.launch {
                    delay(420)
                    onToggle()
                }
            },
        )
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (shownChecked) p.muted else p.ink,
                textDecoration = if (shownChecked) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (task.note.isNotBlank()) {
                Text(
                    task.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ListTag(list)
                val metaColor = if (overdue) p.danger else p.muted
                if (showDate && date != null) {
                    Meta(Icons.Rounded.Event, formatDay(date, today), metaColor)
                }
                if (task.time != null) {
                    Meta(Icons.Rounded.Schedule, formatTime(task.time), metaColor)
                }
                if (task.reminder != Reminder.NONE && task.time != null && !checked) {
                    Meta(Icons.Rounded.NotificationsNone, null, p.muted)
                }
                if (task.repeat != Repeat.NONE) {
                    Meta(Icons.Rounded.Repeat, repeatLabel(task.repeat, task.repeatDays), p.muted)
                }
                if (task.subtasks.isNotEmpty()) {
                    val done = task.subtasks.count { it.done }
                    Meta(Icons.Rounded.Checklist, "$done/${task.subtasks.size}", p.muted)
                }
            }
        }
        if (task.important) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.Star, contentDescription = "Wichtig", tint = p.star, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ListTag(list: TaskList, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(list.strong().copy(alpha = 0.7f)),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            list.name,
            style = MaterialTheme.typography.labelMedium,
            color = list.strong(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Meta(icon: ImageVector, text: String?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        if (text != null) {
            Spacer(Modifier.width(3.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

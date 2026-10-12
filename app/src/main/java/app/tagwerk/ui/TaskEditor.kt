package app.tagwerk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.tagwerk.data.Category
import app.tagwerk.data.Reminder
import app.tagwerk.data.Repeat
import app.tagwerk.data.Subtask
import app.tagwerk.data.Task
import app.tagwerk.ui.theme.Tagwerk
import app.tagwerk.ui.theme.soft
import app.tagwerk.ui.theme.strong
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    initial: Task?,
    defaultDate: LocalDate?,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: (Task) -> Unit,
) {
    val p = Tagwerk.palette
    val today = LocalDate.now()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var date by remember { mutableStateOf(if (initial != null) initial.date else defaultDate) }
    var time by remember { mutableStateOf(initial?.time) }
    var reminder by remember { mutableStateOf(initial?.reminder ?: Reminder.NONE) }
    var repeat by remember { mutableStateOf(initial?.repeat ?: Repeat.NONE) }
    var category by remember { mutableStateOf(initial?.category ?: Category.PERSONAL) }
    var important by remember { mutableStateOf(initial?.important ?: false) }
    val subtasks = remember { mutableStateListOf<Subtask>().apply { addAll(initial?.subtasks.orEmpty()) } }
    var newSubtask by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (initial == null) runCatching { titleFocus.requestFocus() }
    }

    fun pickTime(t: LocalTime?) {
        time = t
        if (t != null) {
            if (date == null) date = today
            if (reminder == Reminder.NONE) reminder = Reminder.AT_TIME
        } else {
            reminder = Reminder.NONE
        }
    }

    fun buildTask(): Task {
        val base = initial ?: Task(id = System.currentTimeMillis(), title = "")
        val pending = newSubtask.trim()
        val allSubtasks = if (pending.isNotEmpty()) {
            subtasks.toList() + Subtask(System.nanoTime(), pending)
        } else subtasks.toList()
        return base.copy(
            title = title.trim(),
            note = note.trim(),
            date = date,
            time = if (date == null) null else time,
            reminder = if (date == null || time == null) Reminder.NONE else reminder,
            repeat = if (date == null) Repeat.NONE else repeat,
            category = category,
            important = important,
            subtasks = allSubtasks,
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = p.background,
        contentColor = p.ink,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = p.line) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (initial == null) "Neue Aufgabe" else "Aufgabe bearbeiten",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { important = !important }) {
                    Icon(
                        if (important) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "Wichtig",
                        tint = if (important) p.star else p.muted,
                    )
                }
                if (initial != null) {
                    IconButton(onClick = { onDelete(initial) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Löschen", tint = p.danger)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(22.dp), color = p.card) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    PlainField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "Was steht an?",
                        style = MaterialTheme.typography.titleMedium.copy(color = p.ink),
                        modifier = Modifier.focusRequester(titleFocus),
                    )
                    Spacer(Modifier.height(10.dp))
                    PlainField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = "Notiz hinzufügen",
                        style = MaterialTheme.typography.bodyMedium.copy(color = p.ink),
                        singleLine = false,
                    )
                }
            }

            Label("Wann")
            val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            val customDate = date?.takeIf { it != today && it != today.plusDays(1) && it != nextMonday }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Ohne Datum", selected = date == null, onClick = { date = null; time = null; reminder = Reminder.NONE; repeat = Repeat.NONE })
                Pill("Heute", selected = date == today, onClick = { date = today })
                Pill("Morgen", selected = date == today.plusDays(1), onClick = { date = today.plusDays(1) })
                Pill("Nächster Montag", selected = date == nextMonday, onClick = { date = nextMonday })
                Pill(
                    customDate?.let { formatDay(it) } ?: "Datum wählen",
                    selected = customDate != null,
                    icon = Icons.Rounded.CalendarMonth,
                    onClick = { showDatePicker = true },
                )
            }

            Label("Uhrzeit")
            val quickTimes = listOf(LocalTime.of(9, 0), LocalTime.of(12, 0), LocalTime.of(18, 0))
            val customTime = time?.takeIf { it !in quickTimes }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Ganztägig", selected = time == null, onClick = { pickTime(null) })
                quickTimes.forEach { t ->
                    Pill(formatTime(t), selected = time == t, onClick = { pickTime(t) })
                }
                Pill(
                    customTime?.let { formatTime(it) } ?: "Andere",
                    selected = customTime != null,
                    icon = Icons.Rounded.Schedule,
                    onClick = { showTimePicker = true },
                )
            }

            if (time != null) {
                Label("Erinnerung")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Reminder.entries.forEach { r ->
                        Pill(r.label, selected = reminder == r, onClick = { reminder = r })
                    }
                }
            }

            Label("Wiederholen")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Repeat.entries.forEach { r ->
                    Pill(r.label, selected = repeat == r, onClick = {
                        repeat = r
                        if (r != Repeat.NONE && date == null) date = today
                    })
                }
            }

            Label("Liste")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { c ->
                    Pill(
                        c.label,
                        selected = category == c,
                        onClick = { category = c },
                        selectedColor = c.soft(),
                        selectedContent = c.strong(),
                    )
                }
            }

            Label("Teilschritte")
            Surface(shape = RoundedCornerShape(22.dp), color = p.card) {
                Column(Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                    subtasks.forEachIndexed { index, sub ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CheckCircle(
                                checked = sub.done,
                                fill = category.soft(),
                                stroke = category.strong(),
                                onClick = { subtasks[index] = sub.copy(done = !sub.done) },
                                size = 20.dp,
                            )
                            Text(
                                sub.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (sub.done) p.muted else p.ink,
                                textDecoration = if (sub.done) TextDecoration.LineThrough else null,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { subtasks.removeAt(index) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Entfernen", tint = p.muted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = p.muted, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        PlainField(
                            value = newSubtask,
                            onValueChange = { newSubtask = it },
                            placeholder = "Schritt hinzufügen",
                            style = MaterialTheme.typography.bodyMedium.copy(color = p.ink),
                            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                            imeAction = ImeAction.Done,
                            onImeAction = {
                                val text = newSubtask.trim()
                                if (text.isNotEmpty()) {
                                    subtasks.add(Subtask(System.nanoTime(), text))
                                    newSubtask = ""
                                }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            val canSave = title.isNotBlank()
            Surface(
                onClick = { onSave(buildTask()) },
                enabled = canSave,
                shape = RoundedCornerShape(20.dp),
                color = if (canSave) p.accent else p.chip,
                contentColor = if (canSave) p.onAccent else p.muted,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(Modifier.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text("Speichern", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: today).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            },
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }

    if (showTimePicker) {
        val initialTime = time ?: LocalTime.now().plusHours(1).withMinute(0)
        val state = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = p.card,
            title = { Text("Uhrzeit wählen", style = MaterialTheme.typography.titleLarge) },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = state)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pickTime(LocalTime.of(state.hour, state.minute))
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Abbrechen") }
            },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Tagwerk.palette.muted,
        modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 10.dp),
    )
}

@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
) {
    val p = Tagwerk.palette
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = style,
        singleLine = singleLine,
        cursorBrush = SolidColor(p.accentStrong),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (singleLine) imeAction else ImeAction.Default,
        ),
        keyboardActions = if (onImeAction != null) KeyboardActions(onAny = { onImeAction() }) else KeyboardActions.Default,
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = style.copy(color = p.muted.copy(alpha = 0.8f)))
                }
                inner()
            }
        },
    )
}

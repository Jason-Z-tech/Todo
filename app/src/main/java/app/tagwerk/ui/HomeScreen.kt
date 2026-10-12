package app.tagwerk.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tagwerk.data.Category
import app.tagwerk.data.Repeat
import app.tagwerk.data.Task
import app.tagwerk.data.TaskStore
import app.tagwerk.ui.theme.Tagwerk
import app.tagwerk.ui.theme.soft
import app.tagwerk.ui.theme.strong
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

private enum class ViewMode(val label: String) { DAY("Tag"), ALL("Alle"), DONE("Erledigt") }

private val taskOrder = compareBy<Task>(
    { it.time == null },
    { it.time },
    { !it.important },
    { it.createdAt },
)

@Composable
fun TagwerkApp() {
    val context = LocalContext.current
    val store = remember { TaskStore.get(context) }
    val tasks by store.tasks.collectAsStateWithLifecycle()
    val p = Tagwerk.palette

    var mode by rememberSaveable { mutableStateOf(ViewMode.DAY) }
    var selectedDay by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var filter by rememberSaveable { mutableStateOf<Category?>(null) }
    var doneExpanded by rememberSaveable { mutableStateOf(false) }

    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    RequestNotificationPermission()

    val today = LocalDate.now()
    val visible = tasks.filter { filter == null || it.category == filter }

    fun toggle(task: Task) {
        val updated = store.setDone(task.id, !task.done) ?: return
        if (!task.done && task.repeat != Repeat.NONE && updated.date != null && !updated.done) {
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar("Weiter geht's: ${formatDay(updated.date)}", duration = SnackbarDuration.Short)
            }
        }
    }

    fun delete(task: Task) {
        store.delete(task.id)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = "„${task.title}“ gelöscht",
                actionLabel = "Rückgängig",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) store.save(task)
        }
    }

    fun open(task: Task?) {
        editing = task
        editorOpen = true
    }

    Scaffold(
        containerColor = p.background,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(18.dp),
                    containerColor = p.ink,
                    contentColor = p.background,
                    actionColor = p.accent,
                )
            }
        },
        floatingActionButton = { AddButton(onClick = { open(null) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 110.dp,
            ),
        ) {
            item(key = "header") { Header(tasks = tasks, today = today) }
            item(key = "modes") {
                ModeTabs(mode = mode, onSelect = { mode = it })
            }
            if (mode == ViewMode.DAY) {
                item(key = "week") {
                    WeekStrip(
                        selected = selectedDay,
                        today = today,
                        tasks = visible,
                        onSelect = { selectedDay = it },
                    )
                }
            }
            item(key = "filter") {
                CategoryFilter(selected = filter, onSelect = { filter = it })
            }

            val showDate = mode != ViewMode.DAY
            val card: LazyListScope.(List<Task>) -> Unit = { list ->
                items(list, key = { it.id }) { task ->
                    SwipeableTaskCard(
                        task = task,
                        showDate = showDate || task.isOverdue(today),
                        onToggle = { toggle(task) },
                        onOpen = { open(task) },
                        onDelete = { delete(task) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            when (mode) {
                ViewMode.DAY -> {
                    val isToday = selectedDay == today
                    val overdue = if (isToday) visible.filter { it.isOverdue(today) }.sortedBy { it.date } else emptyList()
                    val planned = visible.filter { !it.done && it.date == selectedDay }.sortedWith(taskOrder)
                    val someday = if (isToday) visible.filter { !it.done && it.date == null }.sortedWith(taskOrder) else emptyList()
                    val done = visible.filter { it.done && completedOn(it) == selectedDay }
                        .sortedByDescending { it.completedAt }

                    if (overdue.isEmpty() && planned.isEmpty() && someday.isEmpty()) {
                        item(key = "empty") {
                            if (done.isNotEmpty()) {
                                EmptyState("Alles erledigt.", "Schön gemacht – der Rest des Tages gehört dir.")
                            } else {
                                EmptyState(
                                    if (isToday) "Heute ist noch alles ruhig." else "Für diesen Tag ist nichts geplant.",
                                    "Tippe auf „Neue Aufgabe“, um etwas einzutragen.",
                                )
                            }
                        }
                    }
                    if (overdue.isNotEmpty()) {
                        item(key = "h-overdue") { SectionHeader("Überfällig", overdue.size, accent = p.danger) }
                        card(overdue)
                    }
                    if (planned.isNotEmpty()) {
                        item(key = "h-planned") { SectionHeader(if (isToday) "Heute" else formatDay(selectedDay, today), planned.size) }
                        card(planned)
                    }
                    if (someday.isNotEmpty()) {
                        item(key = "h-someday") { SectionHeader("Irgendwann", someday.size) }
                        card(someday)
                    }
                    if (done.isNotEmpty()) {
                        item(key = "h-done") {
                            SectionHeader(
                                "Erledigt",
                                done.size,
                                modifier = Modifier.clickable { doneExpanded = !doneExpanded },
                                trailing = {
                                    Icon(
                                        if (doneExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = null,
                                        tint = p.muted,
                                    )
                                },
                            )
                        }
                        if (doneExpanded) card(done)
                    }
                }

                ViewMode.ALL -> {
                    val open = visible.filter { !it.done }
                    val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    val groups = listOf(
                        "Überfällig" to open.filter { it.isOverdue(today) },
                        "Heute" to open.filter { it.date == today },
                        "Morgen" to open.filter { it.date == today.plusDays(1) },
                        "Diese Woche" to open.filter { it.date != null && it.date.isAfter(today.plusDays(1)) && !it.date.isAfter(endOfWeek) },
                        "Später" to open.filter { it.date != null && it.date.isAfter(endOfWeek) && it.date.isAfter(today.plusDays(1)) },
                        "Ohne Datum" to open.filter { it.date == null },
                    )
                    if (open.isEmpty()) {
                        item(key = "empty") { EmptyState("Nichts offen.", "Deine Liste ist leer – ein schönes Gefühl.") }
                    }
                    groups.forEach { (title, list) ->
                        if (list.isNotEmpty()) {
                            item(key = "h-$title") {
                                SectionHeader(title, list.size, accent = if (title == "Überfällig") p.danger else p.muted)
                            }
                            card(list.sortedWith(compareBy<Task> { it.date }.then(taskOrder)))
                        }
                    }
                }

                ViewMode.DONE -> {
                    val done = visible.filter { it.done }.sortedByDescending { it.completedAt }
                    if (done.isEmpty()) {
                        item(key = "empty") { EmptyState("Noch nichts abgehakt.", "Erledigte Aufgaben sammeln sich hier.") }
                    } else {
                        item(key = "h-done") {
                            SectionHeader("Erledigt", done.size, trailing = {
                                TextButton(onClick = { store.clearCompleted() }) {
                                    Text("Alle entfernen", style = MaterialTheme.typography.labelMedium, color = p.accentStrong)
                                }
                            })
                        }
                        card(done)
                    }
                }
            }
        }
    }

    if (editorOpen) {
        TaskEditorSheet(
            initial = editing,
            defaultDate = if (mode == ViewMode.DAY) selectedDay else null,
            onDismiss = { editorOpen = false },
            onSave = {
                store.save(it)
                editorOpen = false
                if (mode == ViewMode.DAY && it.date != null && it.date != selectedDay && editing == null) {
                    selectedDay = it.date
                }
            },
            onDelete = {
                editorOpen = false
                delete(it)
            },
        )
    }
}

private fun completedOn(task: Task): LocalDate? =
    task.completedAt?.let { java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }

@Composable
private fun Header(tasks: List<Task>, today: LocalDate) {
    val p = Tagwerk.palette
    val todays = tasks.filter { (!it.done && it.date == today) || (it.done && completedOn(it) == today) }
    val doneCount = todays.count { it.done }
    val total = todays.size
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else doneCount.toFloat() / total,
        label = "progress",
    )

    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 8.dp)) {
        Text(formatLongDate(today), style = MaterialTheme.typography.labelLarge, color = p.muted)
        Spacer(Modifier.height(4.dp))
        Text(greeting(), style = MaterialTheme.typography.displaySmall, color = p.ink)
        Spacer(Modifier.height(14.dp))
        Text(
            when {
                total == 0 -> "Für heute ist nichts eingetragen."
                doneCount == total -> "Alle $total Aufgaben für heute erledigt."
                else -> "$doneCount von $total für heute erledigt"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = p.muted,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(p.chip),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(CircleShape)
                    .background(p.accent),
            )
        }
    }
}

@Composable
private fun ModeTabs(mode: ViewMode, onSelect: (ViewMode) -> Unit) {
    val p = Tagwerk.palette
    Row(
        Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(p.chip)
            .padding(4.dp),
    ) {
        ViewMode.entries.forEach { m ->
            val selected = m == mode
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) p.card else Color.Transparent)
                    .clickable { onSelect(m) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    m.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) p.ink else p.muted,
                )
            }
        }
    }
}

@Composable
private fun WeekStrip(
    selected: LocalDate,
    today: LocalDate,
    tasks: List<Task>,
    onSelect: (LocalDate) -> Unit,
) {
    val p = Tagwerk.palette
    val monday = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Column(Modifier.padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onSelect(selected.minusWeeks(1)) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Vorige Woche", tint = p.muted)
            }
            Text(
                "KW ${monday.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)}",
                style = MaterialTheme.typography.labelLarge,
                color = p.muted,
                modifier = Modifier.weight(1f),
            )
            if (selected != today) {
                TextButton(onClick = { onSelect(today) }) {
                    Text("Heute", style = MaterialTheme.typography.labelLarge, color = p.accentStrong)
                }
            }
            IconButton(onClick = { onSelect(selected.plusWeeks(1)) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Nächste Woche", tint = p.muted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
            for (i in 0..6) {
                val day = monday.plusDays(i.toLong())
                val isSelected = day == selected
                val isToday = day == today
                val dayTasks = tasks.filter { !it.done && it.date == day }
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            when {
                                isSelected -> p.accent
                                isToday -> p.accentSoft
                                else -> Color.Transparent
                            },
                        )
                        .clickable { onSelect(day) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        weekdayShort[i],
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) p.onAccent else p.muted,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${day.dayOfMonth}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) p.onAccent else p.ink,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(5.dp)) {
                        dayTasks.map { it.category }.distinct().take(3).forEach { c ->
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) p.onAccent.copy(alpha = 0.6f) else c.strong().copy(alpha = 0.75f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilter(selected: Category?, onSelect: (Category?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Pill("Alle Listen", selected = selected == null, onClick = { onSelect(null) })
        }
        items(Category.entries) { c ->
            Pill(
                c.label,
                selected = selected == c,
                onClick = { onSelect(if (selected == c) null else c) },
                selectedColor = c.soft(),
                selectedContent = c.strong(),
            )
        }
    }
}

@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

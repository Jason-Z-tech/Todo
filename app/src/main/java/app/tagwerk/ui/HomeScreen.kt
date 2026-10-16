package app.tagwerk.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tagwerk.data.Occurrence
import app.tagwerk.data.SettingsStore
import app.tagwerk.data.Task
import app.tagwerk.data.TaskList
import app.tagwerk.data.TaskStore
import app.tagwerk.data.occurrenceOrder
import app.tagwerk.data.occurrencesBetween
import app.tagwerk.data.occurrencesOn
import app.tagwerk.data.overdueOccurrences
import app.tagwerk.ui.theme.Tagwerk
import app.tagwerk.ui.theme.TagwerkTheme
import app.tagwerk.ui.theme.soft
import app.tagwerk.ui.theme.strong
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private enum class AppTab(val label: String, val icon: ImageVector) {
    TASKS("Aufgaben", Icons.Rounded.Home),
    CALENDAR("Kalender", Icons.Rounded.CalendarMonth),
    CHECKLIST("Checkliste", Icons.Rounded.CheckBox),
    SETTINGS("Einstellungen", Icons.Rounded.Settings),
}

private enum class ViewMode(val label: String) { DAY("Tag"), ALL("Alle"), DONE("Erledigt") }

private val taskOrder = compareBy<Task>(
    { it.time == null },
    { it.time },
    { !it.important },
    { it.createdAt },
)

/** Einstieg: liest die Einstellungen und legt das passende Farbthema um die App. */
@Composable
fun TagwerkRoot(dark: Boolean = isSystemInDarkTheme()) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val settings by settingsStore.settings.collectAsStateWithLifecycle()
    TagwerkTheme(dark = dark, settings = settings) {
        TagwerkApp()
    }
}

@Composable
fun TagwerkApp() {
    val context = LocalContext.current
    val store = remember { TaskStore.get(context) }
    val settingsStore = remember { SettingsStore.get(context) }
    val tasks by store.tasks.collectAsStateWithLifecycle()
    val settings = Tagwerk.settings
    val p = Tagwerk.palette

    var tab by rememberSaveable { mutableStateOf(AppTab.TASKS) }
    var mode by rememberSaveable { mutableStateOf(ViewMode.DAY) }
    var selectedDay by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var calendarDay by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var checklistDay by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var filterId by rememberSaveable { mutableStateOf<String?>(null) }
    var doneExpanded by rememberSaveable { mutableStateOf(false) }

    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    RequestNotificationPermission()

    // Zurück führt von den anderen Bereichen erst zur Startseite.
    BackHandler(enabled = tab != AppTab.TASKS) { tab = AppTab.TASKS }

    val today = LocalDate.now()
    val filter = settings.lists.firstOrNull { it.id == filterId }
    val visible = tasks.filter { filter == null || it.listId == filter.id }

    fun toast(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    fun toggle(occurrence: Occurrence) {
        val before = occurrence.task
        val updated = store.toggle(occurrence) ?: return
        val advanced = occurrence.kind == Occurrence.Kind.OPEN && before.isRepeating && updated.date != before.date
        if (advanced && updated.date != null) toast("Weiter geht's: ${formatDay(updated.date)}")
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

    fun createList(name: String, color: Int): TaskList {
        val list = TaskList(settingsStore.newListId(), name, color)
        settingsStore.saveList(list)
        return list
    }

    fun deleteList(list: TaskList) {
        val target = settings.lists.firstOrNull { it.id != list.id } ?: return
        if (settingsStore.deleteList(list.id)) {
            if (filterId == list.id) filterId = null
            toast("„${list.name}“ gelöscht – Aufgaben sind jetzt in „${target.name}“")
        }
    }

    val showFab = tab != AppTab.SETTINGS
    // Jeder Bereich behält beim Wechseln seine Ansicht und Scrollposition.
    val tabStates = rememberSaveableStateHolder()

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
        floatingActionButton = { if (showFab) AddButton(onClick = { open(null) }) },
        bottomBar = { BottomTabs(tab = tab, onSelect = { tab = it }) },
    ) { padding ->
        val contentPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + if (showFab) 96.dp else 16.dp,
        )
        tabStates.SaveableStateProvider(tab.name) {
            when (tab) {
                AppTab.TASKS -> TasksTab(
                    tasks = tasks,
                    visible = visible,
                    today = today,
                    mode = mode,
                    onMode = { mode = it },
                    selectedDay = selectedDay,
                    onSelectDay = { selectedDay = it },
                    filter = filter,
                    onFilter = { filterId = it?.id },
                    onEditLists = { tab = AppTab.SETTINGS },
                    doneExpanded = doneExpanded,
                    onDoneExpanded = { doneExpanded = it },
                    onToggle = ::toggle,
                    onOpen = { open(it) },
                    onDelete = ::delete,
                    onClearCompleted = { store.clearCompleted() },
                    contentPadding = contentPadding,
                )

                AppTab.CALENDAR -> CalendarScreen(
                    tasks = tasks,
                    today = today,
                    selected = calendarDay,
                    onSelect = { calendarDay = it },
                    onToggle = ::toggle,
                    onOpen = { open(it) },
                    contentPadding = contentPadding,
                )

                AppTab.CHECKLIST -> ChecklistScreen(
                    tasks = tasks,
                    today = today,
                    day = checklistDay,
                    onDayChange = { checklistDay = it },
                    onToggle = ::toggle,
                    onOpen = { open(it) },
                    contentPadding = contentPadding,
                )

                AppTab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    tasks = tasks,
                    onNameChange = settingsStore::setName,
                    onAccentChange = settingsStore::setAccent,
                    onSaveList = settingsStore::saveList,
                    onDeleteList = ::deleteList,
                    newListId = settingsStore::newListId,
                    contentPadding = contentPadding,
                )
            }
        }
    }

    if (editorOpen) {
        val defaultDate = when (tab) {
            AppTab.TASKS -> if (mode == ViewMode.DAY) selectedDay else null
            AppTab.CALENDAR -> calendarDay
            AppTab.CHECKLIST -> checklistDay
            AppTab.SETTINGS -> null
        }
        TaskEditorSheet(
            initial = editing,
            defaultDate = defaultDate,
            defaultListId = filter?.id,
            onDismiss = { editorOpen = false },
            onSave = {
                store.save(it)
                editorOpen = false
                if (editing == null && it.date != null && it.date != defaultDate) {
                    when (tab) {
                        AppTab.TASKS -> if (mode == ViewMode.DAY) selectedDay = it.date
                        AppTab.CALENDAR -> calendarDay = it.date
                        AppTab.CHECKLIST -> checklistDay = it.date
                        AppTab.SETTINGS -> Unit
                    }
                }
            },
            onDelete = {
                editorOpen = false
                delete(it)
            },
            onCreateList = ::createList,
        )
    }
}

@Composable
private fun TasksTab(
    tasks: List<Task>,
    visible: List<Task>,
    today: LocalDate,
    mode: ViewMode,
    onMode: (ViewMode) -> Unit,
    selectedDay: LocalDate,
    onSelectDay: (LocalDate) -> Unit,
    filter: TaskList?,
    onFilter: (TaskList?) -> Unit,
    onEditLists: () -> Unit,
    doneExpanded: Boolean,
    onDoneExpanded: (Boolean) -> Unit,
    onToggle: (Occurrence) -> Unit,
    onOpen: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onClearCompleted: () -> Unit,
    contentPadding: PaddingValues,
) {
    val p = Tagwerk.palette
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(key = "header") { Header(tasks = tasks, today = today) }
        item(key = "modes") {
            ModeTabs(mode = mode, onSelect = onMode)
        }
        if (mode == ViewMode.DAY) {
            item(key = "week") {
                WeekStrip(
                    selected = selectedDay,
                    today = today,
                    tasks = visible,
                    onSelect = onSelectDay,
                )
            }
        }
        item(key = "filter") {
            ListFilter(selected = filter, onSelect = onFilter, onEdit = onEditLists)
        }

        val showDate = mode != ViewMode.DAY
        val card: LazyListScope.(List<Task>) -> Unit = { list ->
            items(list, key = { it.id }) { task ->
                SwipeableTaskCard(
                    task = task,
                    showDate = showDate || task.isOverdue(today),
                    onToggle = {
                        val date = task.date ?: today
                        val kind = if (task.done) Occurrence.Kind.DONE else Occurrence.Kind.OPEN
                        onToggle(Occurrence(task, date, kind, countsFor = maxOf(date, today)))
                    },
                    onOpen = { onOpen(task) },
                    onDelete = { onDelete(task) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        val entries: LazyListScope.(List<Occurrence>) -> Unit = { list ->
            items(list, key = { it.key }) { o ->
                SwipeableTaskCard(
                    task = o.task,
                    showDate = o.isOverdue(today),
                    checked = o.done,
                    date = o.date,
                    // Bei Wiederholungen löscht Wischen die ganze Serie – nur beim aktuellen Termin erlauben.
                    swipeEnabled = o.kind == Occurrence.Kind.OPEN || !o.task.isRepeating,
                    onToggle = { onToggle(o) },
                    onOpen = { onOpen(o.task) },
                    onDelete = { onDelete(o.task) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        when (mode) {
            ViewMode.DAY -> {
                val isToday = selectedDay == today
                val onDay = visible.occurrencesOn(selectedDay, today)
                val overdue = if (isToday) visible.overdueOccurrences(today) else emptyList()
                val planned = onDay.filter { !it.done }.sortedWith(occurrenceOrder)
                val someday = if (isToday) visible.filter { !it.done && it.date == null }.sortedWith(taskOrder) else emptyList()
                val done = onDay.filter { it.done }
                    .sortedWith(compareByDescending<Occurrence> { it.task.completedAt ?: 0L }.then(occurrenceOrder))

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
                    entries(overdue)
                }
                if (planned.isNotEmpty()) {
                    item(key = "h-planned") { SectionHeader(if (isToday) "Heute" else formatDay(selectedDay, today), planned.size) }
                    entries(planned)
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
                            modifier = Modifier.clickable { onDoneExpanded(!doneExpanded) },
                            trailing = {
                                Icon(
                                    if (doneExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = p.muted,
                                )
                            },
                        )
                    }
                    if (doneExpanded) entries(done)
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
                            TextButton(onClick = onClearCompleted) {
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

@Composable
private fun BottomTabs(tab: AppTab, onSelect: (AppTab) -> Unit) {
    val p = Tagwerk.palette
    Column {
        HorizontalDivider(thickness = 1.dp, color = p.line)
        NavigationBar(containerColor = p.card, tonalElevation = 0.dp) {
            AppTab.entries.forEach { t ->
                NavigationBarItem(
                    selected = t == tab,
                    onClick = { onSelect(t) },
                    icon = { Icon(t.icon, contentDescription = null) },
                    // etwas kleiner, damit „Einstellungen“ auch auf schmalen Handys ganz passt
                    label = {
                        Text(
                            t.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = p.ink,
                        selectedTextColor = p.ink,
                        indicatorColor = p.accentSoft,
                        unselectedIconColor = p.muted,
                        unselectedTextColor = p.muted,
                    ),
                )
            }
        }
    }
}

@Composable
private fun Header(tasks: List<Task>, today: LocalDate) {
    val p = Tagwerk.palette
    val todays = remember(tasks, today) { tasks.occurrencesOn(today, today) }
    val doneCount = todays.count { it.done }
    val total = todays.size
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else doneCount.toFloat() / total,
        label = "progress",
    )

    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 8.dp)) {
        Text(formatLongDate(today), style = MaterialTheme.typography.labelLarge, color = p.muted)
        Spacer(Modifier.height(4.dp))
        Text(greeting(Tagwerk.settings.name), style = MaterialTheme.typography.displaySmall, color = p.ink)
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
    val settings = Tagwerk.settings
    val monday = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val week = remember(tasks, monday, today) { tasks.occurrencesBetween(monday, monday.plusDays(6), today) }
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
                val dayLists = week[day].orEmpty().filter { !it.done }.map { settings.list(it.task.listId) }.distinctBy { it.id }
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
                        dayLists.take(3).forEach { l ->
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) p.onAccent.copy(alpha = 0.6f) else l.strong().copy(alpha = 0.75f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListFilter(selected: TaskList?, onSelect: (TaskList?) -> Unit, onEdit: () -> Unit) {
    val lists = Tagwerk.settings.lists
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            Pill("Alle Listen", selected = selected == null, onClick = { onSelect(null) })
        }
        items(lists, key = { it.id }) { l ->
            Pill(
                l.name,
                selected = selected?.id == l.id,
                onClick = { onSelect(if (selected?.id == l.id) null else l) },
                selectedColor = l.soft(),
                selectedContent = l.strong(),
            )
        }
        item(key = "edit") {
            Pill("Listen bearbeiten", selected = false, icon = Icons.Rounded.Edit, onClick = onEdit)
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

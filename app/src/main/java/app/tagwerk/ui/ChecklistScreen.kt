package app.tagwerk.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tagwerk.data.Occurrence
import app.tagwerk.data.Task
import app.tagwerk.data.occurrenceOrder
import app.tagwerk.data.occurrencesOn
import app.tagwerk.data.overdueOccurrences
import app.tagwerk.ui.theme.Tagwerk
import app.tagwerk.ui.theme.list
import app.tagwerk.ui.theme.soft
import app.tagwerk.ui.theme.strong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Schlichte Abhakliste eines Tages: nach Uhrzeit sortiert, ohne Uhrzeit ganz unten.
 * Abgehaktes bleibt an seinem Platz stehen – wie auf einem Zettel.
 */
@Composable
fun ChecklistScreen(
    tasks: List<Task>,
    today: LocalDate,
    day: LocalDate,
    onDayChange: (LocalDate) -> Unit,
    onToggle: (Occurrence) -> Unit,
    onOpen: (Task) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // Überfälliges, das hier abgehakt wurde (Aufgabe → ursprünglicher Tag). Es bleibt
    // abgehakt oben stehen, bis man den Tag wechselt.
    val kept = remember(day, today) { mutableStateMapOf<Long, LocalDate>() }
    val keptNow = kept.toMap()
    val plan = remember(tasks, day, today, keptNow) { planDay(tasks, day, today, keptNow) }
    val scope = rememberTickScope()

    val timeStyle = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val fullTimeWidth = remember(timeStyle, density) {
        with(density) { measurer.measure("00:00", timeStyle).size.width.toDp() } + 2.dp
    }
    // Hat kein Eintrag eine Uhrzeit, braucht es auch keine Spalte dafür.
    val timeWidth = if (plan.all.any { it.occurrence.task.time != null }) fullTimeWidth else 0.dp

    fun tick(entry: Entry) {
        val target = entry.target
        when {
            target.isOverdue(today) -> kept[target.task.id] = target.date
            entry.kept -> kept.remove(target.task.id)
        }
        onToggle(target)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(key = "header") {
            ChecklistHeader(
                day = day,
                today = today,
                done = plan.all.count { it.occurrence.done },
                total = plan.all.size,
                onDayChange = onDayChange,
            )
        }

        if (plan.all.isEmpty()) {
            item(key = "empty") {
                EmptyState("Nichts zum Abhaken.", "Für diesen Tag ist nichts eingetragen.")
            }
            return@LazyColumn
        }

        item(key = "gap") { Spacer(Modifier.height(10.dp)) }

        // Nur nötig, wenn darüber etwas mit Uhrzeit steht.
        val showUntimedLabel = plan.untimed.isNotEmpty() &&
            (plan.timed.isNotEmpty() || plan.top.any { it.occurrence.task.time != null })
        val rows = buildList {
            plan.top.forEach { add(Line.Item(it)) }
            plan.timed.forEach { add(Line.Item(it)) }
            if (showUntimedLabel) add(Line.UntimedLabel)
            plan.untimed.forEach { add(Line.Item(it)) }
        }
        rows.forEachIndexed { index, line ->
            val shape = cardShape(first = index == 0, last = index == rows.lastIndex)
            val divider = index > 0 && rows[index - 1] !is Line.UntimedLabel
            when (line) {
                is Line.UntimedLabel -> item(key = "untimed", contentType = "label") {
                    UntimedLabel(shape = shape, modifier = Modifier.animateItem())
                }

                is Line.Item -> item(key = line.entry.key, contentType = "row") {
                    ChecklistRow(
                        entry = line.entry,
                        day = day,
                        today = today,
                        shape = shape,
                        divider = divider,
                        timeWidth = timeWidth,
                        timeStyle = timeStyle,
                        scope = scope,
                        onTick = { tick(line.entry) },
                        onOpen = { onOpen(line.entry.occurrence.task) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        item(key = "hint") {
            Text(
                // Geschützte Leerzeichen: Bei wenig Platz wird nur nach dem Punkt umgebrochen.
                "${keepTogether("Tippen zum Abhaken")} · ${keepTogether("lange drücken zum Bearbeiten")}",
                style = MaterialTheme.typography.bodySmall,
                color = Tagwerk.palette.muted.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 14.dp),
            )
        }
    }
}

/**
 * Ein Eintrag der Checkliste. [target] ist der Termin, auf den das Abhaken wirkt –
 * bei stehengebliebenem Überfälligem ([kept]) der abgehakte Tag (bei einmaligen Aufgaben heute).
 */
private data class Entry(
    val occurrence: Occurrence,
    val target: Occurrence = occurrence,
    val kept: Boolean = false,
) {
    /** Bleibt beim Abhaken gleich, damit die Zeile an ihrem Platz bleibt. */
    val key: String get() = "${occurrence.task.id}@${occurrence.date}"
}

private class DayPlan(val top: List<Entry>, val timed: List<Entry>, val untimed: List<Entry>) {
    val all: List<Entry> = top + timed + untimed
}

private sealed interface Line {
    data class Item(val entry: Entry) : Line
    data object UntimedLabel : Line
}

private val entryOrder: Comparator<Entry> = compareBy(occurrenceOrder) { it.occurrence }

private fun planDay(tasks: List<Task>, day: LocalDate, today: LocalDate, kept: Map<Long, LocalDate>): DayPlan {
    val isToday = day == today
    val onDay = tasks.occurrencesOn(day, today)
    val overdue = if (isToday) tasks.overdueOccurrences(today) else emptyList()
    val stillOpen = (overdue + onDay).filter { it.isOverdue(today) }.map { it.task.id }.toSet()
    val byId = tasks.associateBy { it.id }

    val keptEntries = kept.mapNotNull { (id, date) ->
        val task = byId[id] ?: return@mapNotNull null
        // Serien buchen den verpassten Tag selbst, einmalige Aufgaben gelten als heute erledigt.
        val bookedOn = if (task.isRepeating) date.takeIf { it in task.doneDates } else today.takeIf { task.done && task.doneDay() == today }
        if (id in stillOpen || bookedOn == null) return@mapNotNull null
        Entry(
            occurrence = Occurrence(task, date, Occurrence.Kind.DONE),
            target = Occurrence(task, bookedOn, Occurrence.Kind.DONE),
            kept = true,
        )
    }
    val keptIds = keptEntries.map { it.occurrence.task.id }.toSet()
    // Eine heute erledigte einmalige Aufgabe zeigt schon die stehengebliebene Zeile.
    val dayEntries = onDay
        .filterNot { it.done && it.date == today && it.task.id in keptIds && !it.task.isRepeating }
        .map { Entry(it) }

    val top: List<Entry>
    val rest: List<Entry>
    if (isToday) {
        top = (overdue.map { Entry(it) } + keptEntries)
            .sortedWith(compareBy<Entry> { it.occurrence.date }.then(entryOrder))
        rest = dayEntries
    } else {
        top = emptyList()
        rest = dayEntries + keptEntries.filter { it.occurrence.date == day }
    }
    val sorted = rest.sortedWith(entryOrder)
    val seen = HashSet<String>()
    fun List<Entry>.unique() = filter { seen.add(it.key) }
    return DayPlan(
        top = top.unique(),
        timed = sorted.filter { it.occurrence.task.time != null }.unique(),
        untimed = sorted.filter { it.occurrence.task.time == null }.unique(),
    )
}

private fun cardShape(first: Boolean, last: Boolean): Shape {
    val top = if (first) 22.dp else 0.dp
    val bottom = if (last) 22.dp else 0.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun ChecklistHeader(
    day: LocalDate,
    today: LocalDate,
    done: Int,
    total: Int,
    onDayChange: (LocalDate) -> Unit,
) {
    val p = Tagwerk.palette
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else done.toFloat() / total,
        label = "progress",
    )
    // Oben 23 statt 28 dp: die Zeile ist für den „Heute“-Knopf 48 dp hoch.
    Column(Modifier.padding(start = 24.dp, end = 16.dp, top = 23.dp, bottom = 8.dp)) {
        Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Checkliste",
                style = MaterialTheme.typography.displaySmall,
                color = p.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (day != today) {
                TextButton(onClick = { onDayChange(today) }) {
                    Text("Heute", style = MaterialTheme.typography.labelLarge, color = p.accentStrong)
                }
            }
        }
        Column(Modifier.padding(end = 8.dp)) {
            Spacer(Modifier.height(12.dp))
            DaySwitcher(day = day, today = today, onDayChange = onDayChange)
            if (total > 0) {
                Spacer(Modifier.height(16.dp))
                Text(
                    if (done == total) "Alles erledigt – schön gemacht!" else "$done von $total erledigt",
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
    }
}

/** ‹ Heute › – blättert tageweise. */
@Composable
private fun DaySwitcher(day: LocalDate, today: LocalDate, onDayChange: (LocalDate) -> Unit) {
    val p = Tagwerk.palette
    // 28 dp = Radius der Pfeilknöpfe + Innenabstand, damit die Rundungen parallel laufen.
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(p.chip)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArrowButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Voriger Tag") { onDayChange(day.minusDays(1)) }
        AnimatedContent(
            targetState = day,
            transitionSpec = {
                val forward = targetState.isAfter(initialState)
                val offset = { width: Int -> (if (forward) width else -width) / 5 }
                (slideInHorizontally { offset(it) } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally { -offset(it) } + fadeOut(tween(120)))
            },
            contentAlignment = Alignment.Center,
            label = "day",
            modifier = Modifier.weight(1f),
        ) { shown ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    formatDay(shown, today),
                    style = MaterialTheme.typography.titleMedium,
                    color = p.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    dayHint(shown, today),
                    style = MaterialTheme.typography.labelMedium,
                    color = p.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        ArrowButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Nächster Tag") { onDayChange(day.plusDays(1)) }
    }
}

/**
 * Zweite Zeile im Umschalter: das ganze Datum – oder wie weit der Tag entfernt ist.
 * Liegt der Tag in einem anderen Jahr, steht die Jahreszahl dabei.
 */
private fun dayHint(day: LocalDate, today: LocalDate): String {
    val diff = ChronoUnit.DAYS.between(today, day)
    val year = if (day.year != today.year) day.year else null
    return when {
        diff in -1L..1L -> formatLongDate(day) + (year?.let { " $it" } ?: "")
        diff > 0 -> "in $diff Tagen" + (year?.let { " · $it" } ?: "")
        else -> "vor ${-diff} Tagen" + (year?.let { " · $it" } ?: "")
    }
}

/** Wie [formatDay], aber mit Jahreszahl, wenn der Tag nicht in diesem Jahr liegt (z. B. „Sa, 20. Dez. 2025“). */
private fun dayWithYear(day: LocalDate, today: LocalDate): String {
    val label = formatDay(day, today)
    val near = ChronoUnit.DAYS.between(today, day) in -1L..1L
    return if (near || day.year == today.year) label else "$label ${day.year}"
}

/** Leerzeichen, an denen nicht umgebrochen werden soll. */
private fun keepTogether(text: String): String = text.replace(' ', '\u00A0')

@Composable
private fun ArrowButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val p = Tagwerk.palette
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(p.card)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = p.ink)
    }
}

@Composable
private fun UntimedLabel(shape: Shape, modifier: Modifier = Modifier) {
    val p = Tagwerk.palette
    Row(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(p.card)
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Ohne Uhrzeit",
            style = MaterialTheme.typography.labelMedium,
            color = p.muted,
            maxLines = 1,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(p.line),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChecklistRow(
    entry: Entry,
    day: LocalDate,
    today: LocalDate,
    shape: Shape,
    divider: Boolean,
    timeWidth: Dp,
    timeStyle: androidx.compose.ui.text.TextStyle,
    scope: CoroutineScope,
    onTick: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = Tagwerk.palette
    val o = entry.occurrence
    val task = o.task
    val list = task.list()
    val checked = o.done
    val haptics = LocalHapticFeedback.current
    val currentTick by rememberUpdatedState(onTick)
    // Lokaler Zustand, damit das Häkchen sofort erscheint – die Daten folgen kurz danach.
    var shownChecked by remember(entry.key, checked) { mutableStateOf(checked) }
    var busy by remember(entry.key, checked) { mutableStateOf(false) }

    // Höhe der ersten Titelzeile: Kästchen, Stern und Punkt sitzen mittig dazu.
    val titleLine = MaterialTheme.typography.bodyLarge.lineHeight
    val lineHeight = with(LocalDensity.current) { if (titleLine.isSp) titleLine.toDp() else 22.dp }
    val firstLine = maxOf(lineHeight, 24.dp)
    val titleColor by animateColorAsState(if (shownChecked) p.muted else p.ink, label = "title")
    val overdueSince = when {
        entry.kept -> o.date
        o.isOverdue(today) -> o.date
        else -> null
    }

    Column(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(p.card),
    ) {
        if (divider) {
            Box(
                Modifier
                    .padding(start = 18.dp, end = 18.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(p.line),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    role = Role.Checkbox,
                    onLongClickLabel = "Bearbeiten",
                    onLongClick = onOpen,
                    onClick = {
                        if (busy) return@combinedClickable
                        busy = true
                        shownChecked = !shownChecked
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            delay(380)
                            currentTick()
                        }
                    },
                )
                .semantics { toggleableState = ToggleableState(shownChecked) }
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Box(Modifier.height(firstLine), contentAlignment = Alignment.Center) {
                CheckSquare(checked = shownChecked, fill = list.soft(), stroke = list.strong())
            }
            Spacer(Modifier.width(14.dp))
            if (timeWidth > 0.dp) {
                if (task.time != null) {
                    Text(
                        formatTime(task.time),
                        style = timeStyle,
                        color = if (shownChecked) p.muted.copy(alpha = 0.7f) else p.ink,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .width(timeWidth)
                            .alignByBaseline(),
                    )
                } else {
                    Spacer(Modifier.width(timeWidth))
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(
                Modifier
                    .weight(1f)
                    .alignByBaseline()
                    .padding(top = (firstLine - lineHeight) / 2),
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = titleColor,
                    textDecoration = if (shownChecked) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (overdueSince != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        // Das Datum bricht nur als Ganzes um, nicht mitten drin.
                        if (overdueSince == day) "Überfällig" else "Überfällig · ${keepTogether(dayWithYear(overdueSince, today))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (shownChecked) p.muted else p.danger,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Row(Modifier.height(firstLine), verticalAlignment = Alignment.CenterVertically) {
                if (task.important) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = "Wichtig",
                        tint = p.star.copy(alpha = if (shownChecked) 0.45f else 1f),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(list.strong().copy(alpha = if (shownChecked) 0.35f else 0.7f)),
                )
            }
        }
    }
}

/**
 * Eckiges Kästchen wie auf einem Zettel. Beim Abhaken füllt es sich mit dem
 * Pastellton der Liste und das Häkchen wird nachgezeichnet.
 */
@Composable
private fun CheckSquare(
    checked: Boolean,
    fill: Color,
    stroke: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "check",
    )
    val tick = remember { Path() }
    val segment = remember { Path() }
    val measure = remember { PathMeasure() }

    Canvas(
        modifier
            .size(size)
            .semantics {
                contentDescription = if (checked) "Erledigt" else "Abhaken"
                role = Role.Checkbox
            },
    ) {
        val s = this.size.minDimension
        val strokeWidth = 2.dp.toPx()
        val corner = 7.dp.toPx()
        if (progress > 0f) {
            // Die Füllung wächst aus der Mitte.
            val inner = s * progress
            val inset = (s - inner) / 2f
            drawRoundRect(
                color = fill,
                topLeft = Offset(inset, inset),
                size = Size(inner, inner),
                cornerRadius = CornerRadius(minOf(corner, inner / 2f)),
            )
        }
        drawRoundRect(
            color = stroke.copy(alpha = 0.62f + 0.38f * progress),
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(s - strokeWidth, s - strokeWidth),
            cornerRadius = CornerRadius(corner - strokeWidth / 2f),
            style = Stroke(width = strokeWidth),
        )
        if (progress > 0f) {
            tick.reset()
            tick.moveTo(s * 0.27f, s * 0.53f)
            tick.lineTo(s * 0.43f, s * 0.68f)
            tick.lineTo(s * 0.74f, s * 0.35f)
            measure.setPath(tick, false)
            segment.reset()
            measure.getSegment(0f, measure.length * progress, segment, true)
            drawPath(
                segment,
                color = stroke,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

package app.tagwerk.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tagwerk.data.Occurrence
import app.tagwerk.data.Task
import app.tagwerk.data.occurrenceOrder
import app.tagwerk.data.occurrencesBetween
import app.tagwerk.data.occurrencesOn
import app.tagwerk.ui.theme.Tagwerk
import app.tagwerk.ui.theme.list
import app.tagwerk.ui.theme.soft
import app.tagwerk.ui.theme.strong
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private enum class CalendarMode(val label: String) { DAY("Tag"), WEEK("Woche"), MONTH("Monat") }

private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)
private val monthShort = DateTimeFormatter.ofPattern("MMM", Locale.GERMAN)

/** Früheste Stunde der Tagesansicht – frühere Termine verschieben den Beginn nach vorn. */
private const val DAY_START_HOUR = 7

/**
 * Kalender mit Umschalter Tag / Woche / Monat.
 * [selected] ist der gewählte Tag; die Pfeile blättern je nach Ansicht um einen Tag, eine Woche oder einen Monat.
 */
@Composable
fun CalendarScreen(
    tasks: List<Task>,
    today: LocalDate,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onToggle: (Occurrence) -> Unit,
    onOpen: (Task) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var mode by rememberSaveable { mutableStateOf(CalendarMode.MONTH) }
    val monthStart = selected.withDayOfMonth(1)
    val monday = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    // Nur die gerade sichtbare Ansicht wird berechnet – einmal pro Monat, Woche bzw. Tag.
    val monthEntries = if (mode == CalendarMode.MONTH) {
        remember(tasks, monthStart, today) {
            tasks.occurrencesBetween(gridStart(monthStart), gridEnd(monthStart), today)
        }
    } else emptyMap()
    val weekEntries = if (mode == CalendarMode.WEEK) {
        remember(tasks, monday, today) { tasks.occurrencesBetween(monday, monday.plusDays(6), today) }
    } else emptyMap()
    val dayEntries = if (mode == CalendarMode.DAY) {
        remember(tasks, selected, today) { tasks.occurrencesOn(selected, today).sortedWith(occurrenceOrder) }
    } else emptyList()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(key = "title") { Title(showToday = selected != today, onToday = { onSelect(today) }) }
        item(key = "modes") { ModeSwitch(mode = mode, onSelect = { mode = it }) }
        item(key = "nav") { PeriodNav(mode = mode, selected = selected, today = today, onSelect = onSelect) }

        when (mode) {
            CalendarMode.MONTH -> {
                val entries = monthEntries[selected].orEmpty().sortedWith(occurrenceOrder)
                item(key = "grid") {
                    MonthGrid(
                        monthStart = monthStart,
                        selected = selected,
                        today = today,
                        entries = monthEntries,
                        onSelect = onSelect,
                    )
                }
                if (entries.isEmpty()) {
                    item(key = "day-header") { Caption(formatDay(selected, today)) }
                    item(key = "empty") { EmptyHint() }
                } else {
                    item(key = "day-header") { SectionHeader(formatDay(selected, today), entries.size) }
                }
                items(entries, key = { "m-${it.task.id}@${it.date}" }) { o ->
                    TaskCard(
                        task = o.task,
                        showDate = o.isOverdue(today),
                        checked = o.done,
                        date = o.date,
                        onToggle = { onToggle(o) },
                        onOpen = { onOpen(o.task) },
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 16.dp, vertical = 5.dp),
                    )
                }
            }

            CalendarMode.WEEK -> {
                item(key = "week-gap") { Spacer(Modifier.height(6.dp)) }
                for (i in 0..6) {
                    val day = monday.plusDays(i.toLong())
                    item(key = "week-$day") {
                        WeekDayBlock(
                            day = day,
                            today = today,
                            isSelected = day == selected,
                            entries = weekEntries[day].orEmpty().sortedWith(occurrenceOrder),
                            onSelect = { onSelect(day) },
                            onToggle = onToggle,
                            onOpen = onOpen,
                        )
                    }
                }
            }

            CalendarMode.DAY -> {
                val untimed = dayEntries.filter { it.task.time == null }
                val byHour = dayEntries.filter { it.task.time != null }.groupBy { it.task.time!!.hour }
                val nowHour = if (selected == today) LocalTime.now().hour else null
                val firstHour = listOfNotNull(DAY_START_HOUR, byHour.keys.minOrNull(), nowHour).min()

                if (dayEntries.isEmpty()) {
                    item(key = "empty") { EmptyHint(Modifier.padding(top = 12.dp)) }
                }
                if (untimed.isNotEmpty()) {
                    item(key = "h-untimed") { SectionHeader("Ohne Uhrzeit", untimed.size) }
                    items(untimed, key = { "u-${it.task.id}@${it.date}" }) { o ->
                        EntryBlock(
                            o = o,
                            today = today,
                            showTime = false,
                            onToggle = onToggle,
                            onOpen = onOpen,
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
                item(key = "h-timeline") {
                    if (untimed.isNotEmpty()) SectionHeader("Nach Uhrzeit", dayEntries.size - untimed.size)
                    else Spacer(Modifier.height(18.dp))
                }
                for (hour in firstHour..23) {
                    item(key = "hour-$hour") {
                        HourRow(
                            hour = hour,
                            entries = byHour[hour].orEmpty(),
                            isNow = hour == nowHour,
                            today = today,
                            onToggle = onToggle,
                            onOpen = onOpen,
                        )
                    }
                }
            }
        }
    }
}

/** Erster Tag des Monatsrasters: der Montag der ersten Woche. */
private fun gridStart(monthStart: LocalDate): LocalDate =
    monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Letzter Tag des Monatsrasters: der Sonntag der letzten Woche. */
private fun gridEnd(monthStart: LocalDate): LocalDate =
    monthStart.with(TemporalAdjusters.lastDayOfMonth()).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

/** „Oktober 2026“, „KW 42 · 12.–18. Okt.“ oder „Freitag, 16. Oktober“. */
private fun periodTitle(mode: CalendarMode, selected: LocalDate, today: LocalDate): String = when (mode) {
    CalendarMode.MONTH -> monthYear.format(selected)
    CalendarMode.WEEK -> {
        val monday = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        val range = if (monday.month == sunday.month) {
            "${monday.dayOfMonth}.–${sunday.dayOfMonth}. ${monthShort.format(sunday)}"
        } else {
            "${monday.dayOfMonth}. ${monthShort.format(monday)}–${sunday.dayOfMonth}. ${monthShort.format(sunday)}"
        }
        "KW ${monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)} · $range"
    }
    CalendarMode.DAY -> formatLongDate(selected) + if (selected.year != today.year) " ${selected.year}" else ""
}

/** Überschrift wie auf der Startseite; „Heute“ springt zurück, wenn ein anderer Tag gewählt ist. */
@Composable
private fun Title(showToday: Boolean, onToday: () -> Unit) {
    // Feste Mindesthöhe, damit nichts springt, wenn „Heute“ erscheint (Knöpfe sind 48 dp hoch).
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 20.dp, top = 23.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Kalender",
            style = MaterialTheme.typography.displaySmall,
            color = Tagwerk.palette.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (showToday) {
            Spacer(Modifier.width(12.dp))
            Pill("Heute", selected = false, onClick = onToday, icon = Icons.Rounded.Today)
        }
    }
}

/** Wie [SectionHeader], nur ohne Zahl – für Tage ohne Einträge. */
@Composable
private fun Caption(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Tagwerk.palette.muted,
        maxLines = 1,
        modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
    )
}

@Composable
private fun ModeSwitch(mode: CalendarMode, onSelect: (CalendarMode) -> Unit) {
    val p = Tagwerk.palette
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(p.chip)
            .padding(4.dp),
    ) {
        CalendarMode.entries.forEach { m ->
            val selected = m == mode
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) p.card else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(m) })
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    m.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) p.ink else p.muted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PeriodNav(mode: CalendarMode, selected: LocalDate, today: LocalDate, onSelect: (LocalDate) -> Unit) {
    val p = Tagwerk.palette
    val (previous, next) = when (mode) {
        CalendarMode.DAY -> "Voriger Tag" to "Nächster Tag"
        CalendarMode.WEEK -> "Vorige Woche" to "Nächste Woche"
        CalendarMode.MONTH -> "Voriger Monat" to "Nächster Monat"
    }
    // plusMonths behält den Tag und kürzt ihn bei kürzeren Monaten (31. → 30.).
    fun step(n: Long): LocalDate = when (mode) {
        CalendarMode.DAY -> selected.plusDays(n)
        CalendarMode.WEEK -> selected.plusWeeks(n)
        CalendarMode.MONTH -> selected.plusMonths(n)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            periodTitle(mode, selected, today),
            style = MaterialTheme.typography.titleMedium,
            color = p.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onSelect(step(-1)) }) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = previous, tint = p.muted)
        }
        IconButton(onClick = { onSelect(step(1)) }) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = next, tint = p.muted)
        }
    }
}

// ---------- Monat ----------

@Composable
private fun MonthGrid(
    monthStart: LocalDate,
    selected: LocalDate,
    today: LocalDate,
    entries: Map<LocalDate, List<Occurrence>>,
    onSelect: (LocalDate) -> Unit,
) {
    val p = Tagwerk.palette
    val start = gridStart(monthStart)
    val weeks = (ChronoUnit.DAYS.between(start, gridEnd(monthStart)) + 1).toInt() / 7
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(p.card)
            .padding(horizontal = 6.dp, vertical = 10.dp),
    ) {
        Row(Modifier.padding(bottom = 4.dp)) {
            weekdayShort.forEach { name ->
                Text(
                    name,
                    style = MaterialTheme.typography.labelMedium,
                    color = p.muted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        for (w in 0 until weeks) {
            Row {
                for (i in 0..6) {
                    val day = start.plusDays(w * 7L + i)
                    DayCell(
                        day = day,
                        inMonth = day.month == monthStart.month,
                        isSelected = day == selected,
                        isToday = day == today,
                        entries = entries[day].orEmpty(),
                        onClick = { onSelect(day) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    day: LocalDate,
    inMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    entries: List<Occurrence>,
    onClick: () -> Unit,
) {
    val p = Tagwerk.palette
    val settings = Tagwerk.settings
    // Punkte nur für Offenes – Erledigtes braucht keinen Hinweis mehr.
    val lists = entries.filter { !it.done }.map { settings.list(it.task.listId) }.distinctBy { it.id }.take(3)
    val fade = if (inMonth || isSelected) 1f else 0.45f
    Column(
        Modifier
            .weight(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isSelected -> p.accent
                    isToday -> p.accentSoft
                    else -> Color.Transparent
                },
            )
            .semantics { contentDescription = formatLongDate(day) }
            .clickable(onClick = onClick)
            .padding(top = 8.dp, bottom = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "${day.dayOfMonth}",
            style = MaterialTheme.typography.titleSmall,
            color = when {
                isSelected -> p.onAccent
                inMonth -> p.ink
                else -> p.muted.copy(alpha = 0.6f)
            },
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(5.dp)) {
            lists.forEach { l ->
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) p.onAccent.copy(alpha = 0.6f)
                            else l.strong().copy(alpha = 0.75f * fade),
                        ),
                )
            }
        }
    }
}

// ---------- Woche ----------

@Composable
private fun WeekDayBlock(
    day: LocalDate,
    today: LocalDate,
    isSelected: Boolean,
    entries: List<Occurrence>,
    onSelect: () -> Unit,
    onToggle: (Occurrence) -> Unit,
    onOpen: (Task) -> Unit,
) {
    val p = Tagwerk.palette
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(p.card)
            .then(if (isSelected) Modifier.border(1.5.dp, p.accent, shape) else Modifier),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
                .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = if (entries.isEmpty()) 12.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${weekdayShort[day.dayOfWeek.value - 1]} ${day.dayOfMonth}.",
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) p.accentStrong else p.ink,
                maxLines = 1,
            )
            if (day == today) {
                Spacer(Modifier.width(8.dp))
                TodayBadge()
            }
            Spacer(Modifier.weight(1f))
            if (entries.isEmpty()) {
                Text("Frei", style = MaterialTheme.typography.bodyMedium, color = p.muted, maxLines = 1)
            } else {
                Text(
                    "${entries.count { it.done }}/${entries.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = p.muted.copy(alpha = 0.8f),
                    maxLines = 1,
                )
            }
        }
        if (entries.isNotEmpty()) {
            val timeColumn = entries.any { it.task.time != null }
            entries.forEach { o ->
                key(o.task.id) {
                    CompactRow(o = o, today = today, timeColumn = timeColumn, onToggle = onToggle, onOpen = onOpen)
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun TodayBadge() {
    val p = Tagwerk.palette
    Text(
        "Heute",
        style = MaterialTheme.typography.labelSmall,
        color = p.ink,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(p.accentSoft)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Eine schmale Zeile in der Wochenansicht: Kreis, Uhrzeit, Titel. */
@Composable
private fun CompactRow(
    o: Occurrence,
    today: LocalDate,
    timeColumn: Boolean,
    onToggle: (Occurrence) -> Unit,
    onOpen: (Task) -> Unit,
) {
    val p = Tagwerk.palette
    val list = o.task.list()
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen(o.task) }
            .padding(start = 9.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(
            checked = o.done,
            fill = list.soft(),
            stroke = list.strong(),
            size = 20.dp,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle(o)
            },
        )
        Spacer(Modifier.width(4.dp))
        if (timeColumn) {
            Text(
                o.task.time?.let(::formatTime).orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = if (o.isOverdue(today)) p.danger else p.muted,
                maxLines = 1,
                modifier = Modifier.width(46.dp),
            )
        }
        Text(
            o.task.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (o.done) p.muted else p.ink,
            textDecoration = if (o.done) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 7.dp),
        )
        if (o.task.time == null && o.isOverdue(today)) {
            Spacer(Modifier.width(6.dp))
            Text("überfällig", style = MaterialTheme.typography.labelMedium, color = p.danger, maxLines = 1)
        }
        if (o.task.important) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.Star, contentDescription = "Wichtig", tint = p.star, modifier = Modifier.size(16.dp))
        }
    }
}

// ---------- Tag ----------

@Composable
private fun HourRow(
    hour: Int,
    entries: List<Occurrence>,
    isNow: Boolean,
    today: LocalDate,
    onToggle: (Occurrence) -> Unit,
    onOpen: (Task) -> Unit,
) {
    val p = Tagwerk.palette
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 24.dp, end = 16.dp),
    ) {
        Text(
            formatTime(LocalTime.of(hour, 0)),
            style = MaterialTheme.typography.labelMedium,
            color = if (isNow) p.accentStrong else p.muted,
            maxLines = 1,
            modifier = Modifier.width(52.dp),
        )
        Column(Modifier.weight(1f)) {
            // Haarlinie auf Höhe der Stundenzahl; die aktuelle Stunde in der Themenfarbe.
            Box(Modifier.fillMaxWidth().height(16.dp), contentAlignment = Alignment.CenterStart) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(if (isNow) 2.dp else 1.dp)
                        .background(if (isNow) p.accent else p.line),
                )
                if (isNow) {
                    Box(
                        Modifier
                            .offset(x = (-4).dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(p.accent),
                    )
                    Text(
                        "Jetzt",
                        style = MaterialTheme.typography.labelSmall,
                        color = p.accentStrong,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .background(p.background)
                            .padding(start = 6.dp),
                    )
                }
            }
            if (entries.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                entries.forEach { o ->
                    key(o.task.id) {
                        EntryBlock(
                            o = o,
                            today = today,
                            showTime = true,
                            onToggle = onToggle,
                            onOpen = onOpen,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/** Ein Termin als zarter Block in der Farbe seiner Liste. */
@Composable
private fun EntryBlock(
    o: Occurrence,
    today: LocalDate,
    showTime: Boolean,
    onToggle: (Occurrence) -> Unit,
    onOpen: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = Tagwerk.palette
    val list = o.task.list()
    val soft = list.soft()
    val haptics = LocalHapticFeedback.current
    val overdue = o.isOverdue(today)
    val meta = listOfNotNull(
        o.task.time?.takeIf { showTime }?.let(::formatTime),
        list.name,
        "überfällig".takeIf { overdue },
    ).joinToString(" · ")
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (o.done) soft.copy(alpha = soft.alpha * 0.5f) else soft)
            .clickable { onOpen(o.task) }
            .padding(start = 4.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(
            checked = o.done,
            fill = p.card,
            stroke = list.strong(),
            size = 20.dp,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle(o)
            },
        )
        Spacer(Modifier.width(2.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 9.dp),
        ) {
            Text(
                o.task.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = MaterialTheme.typography.bodyLarge.fontWeight),
                color = if (o.done) p.muted else p.ink,
                textDecoration = if (o.done) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                meta,
                style = MaterialTheme.typography.labelMedium,
                color = if (overdue) p.danger else list.strong(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (o.task.important) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.Star, contentDescription = "Wichtig", tint = p.star, modifier = Modifier.size(16.dp))
        }
    }
}

// ---------- Leer ----------

/** Kleiner, ruhiger Hinweis für Tage ohne Einträge. */
@Composable
private fun EmptyHint(modifier: Modifier = Modifier) {
    val p = Tagwerk.palette
    val shape = RoundedCornerShape(22.dp)
    val dots = Tagwerk.settings.lists.take(3).map { it.soft() }
    Column(
        modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, p.line, shape)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(width = 44.dp, height = 18.dp)) {
            val r = size.height / 2f
            dots.forEachIndexed { i, c ->
                drawCircle(c, radius = r * (1f - 0.18f * i), center = Offset(r + i * r * 1.25f, r))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Nichts geplant.", style = MaterialTheme.typography.titleMedium, color = p.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(
            "Tippe auf „Neue Aufgabe“, um etwas einzutragen.",
            style = MaterialTheme.typography.bodyMedium,
            color = p.muted,
            textAlign = TextAlign.Center,
        )
    }
}

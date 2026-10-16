package app.tagwerk.ui

import app.tagwerk.data.Repeat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val german = Locale.GERMAN
private val longDate = DateTimeFormatter.ofPattern("EEEE, d. MMMM", german)
private val shortDate = DateTimeFormatter.ofPattern("d. MMM", german)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", german)

val weekdayShort = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
val weekdayLong = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")

fun formatLongDate(date: LocalDate): String = longDate.format(date)

fun formatTime(time: LocalTime): String = timeFormat.format(time)

/** „Heute“, „Morgen“, „Gestern“ oder z. B. „Mi, 14. Okt.“ */
fun formatDay(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Heute"
    today.plusDays(1) -> "Morgen"
    today.minusDays(1) -> "Gestern"
    else -> "${weekdayShort[date.dayOfWeek.value - 1]}, ${shortDate.format(date)}"
}

/** Begrüßung oben in der App, z. B. „Hallo Queen“. */
fun greeting(name: String): String = name.trim().let { if (it.isEmpty()) "Hallo" else "Hallo $it" }

/** „Täglich“, „Werktags“ … – bei bestimmten Tagen z. B. „Mo, Do“. */
fun repeatLabel(repeat: Repeat, days: Set<DayOfWeek>): String = when {
    repeat != Repeat.DAYS -> repeat.label
    days.isEmpty() -> Repeat.WEEKLY.label
    days.size == 7 -> Repeat.DAILY.label
    else -> days.sorted().joinToString(", ") { weekdayShort[it.value - 1] }
}

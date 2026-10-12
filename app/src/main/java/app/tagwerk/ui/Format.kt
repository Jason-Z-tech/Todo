package app.tagwerk.ui

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val german = Locale.GERMAN
private val longDate = DateTimeFormatter.ofPattern("EEEE, d. MMMM", german)
private val shortDate = DateTimeFormatter.ofPattern("d. MMM", german)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", german)

val weekdayShort = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

fun formatLongDate(date: LocalDate): String = longDate.format(date)

fun formatTime(time: LocalTime): String = timeFormat.format(time)

/** „Heute“, „Morgen“, „Gestern“ oder z. B. „Mi, 14. Okt.“ */
fun formatDay(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Heute"
    today.plusDays(1) -> "Morgen"
    today.minusDays(1) -> "Gestern"
    else -> "${weekdayShort[date.dayOfWeek.value - 1]}, ${shortDate.format(date)}"
}

fun greeting(time: LocalTime = LocalTime.now()): String = when (time.hour) {
    in 5..10 -> "Guten Morgen"
    in 11..17 -> "Hallo du"
    in 18..22 -> "Guten Abend"
    else -> "Gute Nacht"
}

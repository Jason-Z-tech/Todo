package app.tagwerk.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class Category(val label: String) {
    PERSONAL("Persönlich"),
    WORK("Arbeit"),
    SHOPPING("Einkauf"),
    HEALTH("Gesundheit"),
    HOME("Zuhause"),
    IDEAS("Ideen"),
}

enum class Repeat(val label: String) {
    NONE("Nie"),
    DAILY("Täglich"),
    WEEKDAYS("Werktags"),
    WEEKLY("Wöchentlich"),
    MONTHLY("Monatlich"),
    YEARLY("Jährlich");

    fun next(from: LocalDate): LocalDate = when (this) {
        NONE -> from
        DAILY -> from.plusDays(1)
        WEEKDAYS -> {
            var d = from.plusDays(1)
            while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) d = d.plusDays(1)
            d
        }
        WEEKLY -> from.plusWeeks(1)
        MONTHLY -> from.plusMonths(1)
        YEARLY -> from.plusYears(1)
    }
}

enum class Reminder(val label: String, val minutesBefore: Long?) {
    NONE("Keine", null),
    AT_TIME("Zur Uhrzeit", 0),
    MIN_5("5 Min vorher", 5),
    MIN_15("15 Min vorher", 15),
    MIN_30("30 Min vorher", 30),
    HOUR_1("1 Std vorher", 60),
    DAY_1("1 Tag vorher", 24 * 60),
}

data class Subtask(
    val id: Long,
    val text: String,
    val done: Boolean = false,
)

data class Task(
    val id: Long,
    val title: String,
    val note: String = "",
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val reminder: Reminder = Reminder.NONE,
    val repeat: Repeat = Repeat.NONE,
    val category: Category = Category.PERSONAL,
    val important: Boolean = false,
    val done: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val subtasks: List<Subtask> = emptyList(),
) {
    val dateTime: LocalDateTime?
        get() = if (date != null && time != null) date.atTime(time) else null

    /** Zeitpunkt, zu dem die Erinnerung ausgelöst werden soll – oder null. */
    fun reminderAt(): LocalDateTime? {
        val minutes = reminder.minutesBefore ?: return null
        return dateTime?.minusMinutes(minutes)
    }

    fun isOverdue(today: LocalDate = LocalDate.now()): Boolean =
        !done && date != null && date.isBefore(today)
}

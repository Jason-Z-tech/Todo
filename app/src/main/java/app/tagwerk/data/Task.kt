package app.tagwerk.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Eine Liste, z. B. „Arbeit“. Name und Farbe kann man selbst festlegen. */
data class TaskList(
    val id: String,
    val name: String,
    /** Pastellton als ARGB, z. B. 0xFFFBD3C2. */
    val color: Int,
)

object DefaultLists {
    // Die IDs entsprechen den früheren festen Kategorien, damit alte Aufgaben ihre Liste behalten.
    const val PERSONAL = "PERSONAL"
    const val WORK = "WORK"
    const val SHOPPING = "SHOPPING"
    const val HEALTH = "HEALTH"
    const val HOME = "HOME"
    const val IDEAS = "IDEAS"

    val all = listOf(
        TaskList(PERSONAL, "Persönlich", 0xFFFBD3C2.toInt()),
        TaskList(WORK, "Arbeit", 0xFFCFE0F0.toInt()),
        TaskList(SHOPPING, "Einkauf", 0xFFD4E6CC.toInt()),
        TaskList(HEALTH, "Gesundheit", 0xFFF6D0D8.toInt()),
        TaskList(HOME, "Zuhause", 0xFFE0D6F0.toInt()),
        TaskList(IDEAS, "Ideen", 0xFFF8E8B0.toInt()),
    )
}

enum class Repeat(val label: String) {
    NONE("Nie"),
    DAILY("Täglich"),
    WEEKDAYS("Werktags"),
    WEEKLY("Wöchentlich"),
    /** An frei gewählten Wochentagen, z. B. Mo + Do. */
    DAYS("Bestimmte Tage"),
    MONTHLY("Monatlich"),
    YEARLY("Jährlich");

    /** Nächster Termin nach [from]. [days] gilt nur für [DAYS]. */
    fun next(from: LocalDate, days: Set<DayOfWeek> = emptySet()): LocalDate = when (this) {
        NONE -> from
        DAILY -> from.plusDays(1)
        WEEKDAYS -> {
            var d = from.plusDays(1)
            while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) d = d.plusDays(1)
            d
        }
        WEEKLY -> from.plusWeeks(1)
        DAYS -> if (days.isEmpty()) from.plusWeeks(1) else {
            var d = from.plusDays(1)
            while (d.dayOfWeek !in days) d = d.plusDays(1)
            d
        }
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
    /** Wochentage für [Repeat.DAYS]. */
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val listId: String = DefaultLists.PERSONAL,
    val important: Boolean = false,
    val done: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val subtasks: List<Subtask> = emptyList(),
    /** Bei wiederkehrenden Aufgaben: Tage, an denen sie abgehakt wurde. */
    val doneDates: Set<LocalDate> = emptySet(),
) {
    val dateTime: LocalDateTime?
        get() = if (date != null && time != null) date.atTime(time) else null

    val isRepeating: Boolean
        get() = repeat != Repeat.NONE && date != null

    /** Zeitpunkt, zu dem die Erinnerung ausgelöst werden soll – oder null. */
    fun reminderAt(): LocalDateTime? {
        val minutes = reminder.minutesBefore ?: return null
        return dateTime?.minusMinutes(minutes)
    }

    fun isOverdue(today: LocalDate = LocalDate.now()): Boolean =
        !done && date != null && date.isBefore(today)

    fun nextAfter(from: LocalDate): LocalDate = repeat.next(from, repeatDays)

    fun completedOn(zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
        completedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    /**
     * Tag, an dem eine erledigte (nicht wiederkehrende) Aufgabe als abgehakt erscheint:
     * ihr Datum – oder der Tag des Abhakens, falls sie überfällig war.
     */
    fun doneDay(): LocalDate? {
        if (!done) return null
        val completed = completedOn()
        return when {
            date == null -> completed
            completed == null -> date
            else -> maxOf(date, completed)
        }
    }
}

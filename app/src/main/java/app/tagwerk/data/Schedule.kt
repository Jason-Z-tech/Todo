package app.tagwerk.data

import java.time.LocalDate

/**
 * Wie eine Aufgabe an einem bestimmten Tag erscheint. Wiederkehrende Aufgaben
 * speichern nur ihren nächsten Termin – Kalender und Checkliste brauchen aber
 * auch die späteren Termine und die schon abgehakten Tage.
 */
data class Occurrence(
    val task: Task,
    val date: LocalDate,
    val kind: Kind,
) {
    enum class Kind {
        /** Der aktuelle, offene Termin (bei Überfälligem in der Vergangenheit). */
        OPEN,

        /** An diesem Tag abgehakt. */
        DONE,

        /** Ein späterer Termin einer wiederkehrenden Aufgabe. */
        UPCOMING,
    }

    val done: Boolean get() = kind == Kind.DONE

    /** Eindeutig innerhalb einer Liste – auch wenn eine Aufgabe an mehreren Tagen auftaucht. */
    val key: String get() = "${task.id}@$date:$kind"

    fun isOverdue(today: LocalDate): Boolean = kind == Kind.OPEN && date.isBefore(today)
}

/** Obergrenze, damit z. B. eine tägliche Aufgabe von vor Jahren nicht ewig rechnet. */
private const val MAX_STEPS = 1500

/**
 * Alle Einträge dieser Aufgabe zwischen [start] und [end] (jeweils inklusive).
 * Spätere Termine wiederkehrender Aufgaben ([Occurrence.Kind.UPCOMING]) gibt es nur
 * nach heute: Bis der aktuelle Termin abgehakt ist, bleibt die Serie dort stehen.
 */
fun Task.occurrences(start: LocalDate, end: LocalDate, today: LocalDate): List<Occurrence> {
    if (end.isBefore(start)) return emptyList()
    val out = ArrayList<Occurrence>()
    if (!isRepeating) {
        if (done) {
            doneDay()?.takeIf { it in start..end }?.let { out += Occurrence(this, it, Occurrence.Kind.DONE) }
        } else if (date != null && date in start..end) {
            out += Occurrence(this, date, Occurrence.Kind.OPEN)
        }
        return out
    }

    doneDates.filter { it in start..end }.sorted().forEach { out += Occurrence(this, it, Occurrence.Kind.DONE) }
    val first = date ?: return out
    if (done) {
        // Sonderfall: erledigt und erst danach auf „Wiederholen“ gestellt
        doneDay()?.takeIf { it in start..end && it !in doneDates }?.let { out += Occurrence(this, it, Occurrence.Kind.DONE) }
        return out
    }
    if (first in start..end && first !in doneDates) out += Occurrence(this, first, Occurrence.Kind.OPEN)

    var d = nextAfter(first)
    var steps = 0
    while (!d.isAfter(end) && steps < MAX_STEPS) {
        if (d.isAfter(today) && !d.isBefore(start) && d !in doneDates) {
            out += Occurrence(this, d, Occurrence.Kind.UPCOMING)
        }
        d = nextAfter(d)
        steps++
    }
    return out
}

/** Einträge aller Aufgaben an einem Tag (ohne Überfälliges von früheren Tagen). */
fun List<Task>.occurrencesOn(day: LocalDate, today: LocalDate): List<Occurrence> =
    flatMap { it.occurrences(day, day, today) }

/** Einträge aller Aufgaben im Zeitraum, nach Tag gruppiert. */
fun List<Task>.occurrencesBetween(start: LocalDate, end: LocalDate, today: LocalDate): Map<LocalDate, List<Occurrence>> =
    flatMap { it.occurrences(start, end, today) }.groupBy { it.date }

/** Offene Termine vor [today] – erscheinen „heute“ als überfällig. */
fun List<Task>.overdueOccurrences(today: LocalDate): List<Occurrence> =
    filter { it.isOverdue(today) }
        .map { Occurrence(it, it.date!!, Occurrence.Kind.OPEN) }
        .sortedBy { it.date }

/** Uhrzeit zuerst (früh → spät), ohne Uhrzeit ganz unten; dann Wichtiges, dann Reihenfolge des Anlegens. */
val occurrenceOrder: Comparator<Occurrence> = compareBy<Occurrence>(
    { it.task.time == null },
    { it.task.time },
    { !it.task.important },
    { it.task.createdAt },
)

package app.tagwerk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.ZoneId

class ScheduleTest {

    // Montag, 12. Oktober 2026
    private val monday = LocalDate.of(2026, 10, 12)

    @Test
    fun `bestimmte Tage springen zum naechsten gewaehlten Wochentag`() {
        val days = setOf(MONDAY, THURSDAY)
        assertEquals(monday.plusDays(3), Repeat.DAYS.next(monday, days))
        assertEquals(monday.plusDays(7), Repeat.DAYS.next(monday.plusDays(3), days))
    }

    @Test
    fun `bestimmte Tage ohne Auswahl verhalten sich woechentlich`() {
        assertEquals(monday.plusWeeks(1), Repeat.DAYS.next(monday, emptySet()))
    }

    @Test
    fun `werktags ueberspringt das Wochenende`() {
        val friday = monday.plusDays(4)
        assertEquals(monday.plusWeeks(1), Repeat.WEEKDAYS.next(friday))
    }

    @Test
    fun `wiederkehrende Aufgabe zeigt spaetere Termine erst nach heute`() {
        val task = Task(id = 1, title = "Yoga", date = monday, repeat = Repeat.DAYS, repeatDays = setOf(MONDAY, WEDNESDAY))
        val result = task.occurrences(monday, monday.plusDays(13), today = monday)
        assertEquals(
            listOf(monday, monday.plusDays(2), monday.plusDays(7), monday.plusDays(9)),
            result.map { it.date },
        )
        assertEquals(Occurrence.Kind.OPEN, result.first().kind)
        assertTrue(result.drop(1).all { it.kind == Occurrence.Kind.UPCOMING })
    }

    @Test
    fun `ueberfaellige Serie bleibt stehen bis sie abgehakt ist`() {
        val task = Task(id = 1, title = "Gießen", date = monday, repeat = Repeat.DAILY)
        val today = monday.plusDays(3)
        val result = task.occurrences(monday, today.plusDays(2), today)
        // offener Termin am Montag, dazwischen nichts, danach erst ab morgen
        assertEquals(listOf(monday, today.plusDays(1), today.plusDays(2)), result.map { it.date })
        assertTrue(result.first().isOverdue(today))
    }

    @Test
    fun `abgehakte Tage einer Serie erscheinen als erledigt`() {
        val task = Task(
            id = 1, title = "Tabletten", date = monday.plusDays(1), repeat = Repeat.DAILY,
            doneDates = setOf(monday),
        )
        val onMonday = listOf(task).occurrencesOn(monday, today = monday)
        assertEquals(1, onMonday.size)
        assertEquals(Occurrence.Kind.DONE, onMonday.single().kind)
        val onTuesday = listOf(task).occurrencesOn(monday.plusDays(1), today = monday)
        assertEquals(Occurrence.Kind.OPEN, onTuesday.single().kind)
    }

    @Test
    fun `erledigte Aufgabe erscheint an ihrem Datum oder am Tag des Abhakens`() {
        val zone = ZoneId.systemDefault()
        val wednesday = monday.plusDays(2)
        val doneWednesday = wednesday.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        // überfällig vom Montag, am Mittwoch erledigt → Mittwoch
        val late = Task(id = 1, title = "a", date = monday, done = true, completedAt = doneWednesday)
        assertEquals(wednesday, late.doneDay())
        // für Freitag geplant, schon am Mittwoch erledigt → Freitag
        val early = Task(id = 2, title = "b", date = monday.plusDays(4), done = true, completedAt = doneWednesday)
        assertEquals(monday.plusDays(4), early.doneDay())
        // ohne Datum → Tag des Abhakens
        val undated = Task(id = 3, title = "c", done = true, completedAt = doneWednesday)
        assertEquals(wednesday, undated.doneDay())
    }

    @Test
    fun `Aufgaben ohne Datum haben keine Termine`() {
        val task = Task(id = 1, title = "Irgendwann")
        assertTrue(task.occurrences(monday, monday.plusDays(30), monday).isEmpty())
    }

    @Test
    fun `Reihenfolge nach Uhrzeit, ohne Uhrzeit unten`() {
        val a = Occurrence(Task(id = 1, title = "ohne", createdAt = 1), monday, Occurrence.Kind.OPEN)
        val b = Occurrence(Task(id = 2, title = "spät", time = java.time.LocalTime.of(18, 0), createdAt = 2), monday, Occurrence.Kind.OPEN)
        val c = Occurrence(Task(id = 3, title = "früh", time = java.time.LocalTime.of(8, 30), createdAt = 3), monday, Occurrence.Kind.DONE)
        assertEquals(listOf(c, b, a), listOf(a, b, c).sortedWith(occurrenceOrder))
    }

    @Test
    fun `Schluessel bleiben eindeutig`() {
        val task = Task(id = 7, title = "x", date = monday, repeat = Repeat.DAILY, doneDates = setOf(monday.minusDays(1)))
        val all = task.occurrences(monday.minusDays(1), monday.plusDays(40), today = monday)
        assertEquals(all.size, all.map { it.key }.toSet().size)
    }

    @Test
    fun `taegliche Serie erscheint auch Jahre spaeter`() {
        val task = Task(id = 1, title = "Gießen", date = monday, repeat = Repeat.DAILY)
        val later = monday.plusYears(6)
        assertEquals(7, task.occurrences(later, later.plusDays(6), today = monday).size)
    }

    @Test
    fun `woechentliche Serie bleibt beim Vorlauf auf ihrem Wochentag`() {
        val wednesday = monday.plusDays(2)
        val task = Task(id = 1, title = "Chor", date = wednesday, repeat = Repeat.WEEKLY)
        val later = monday.plusYears(3)
        val result = task.occurrences(later, later.plusDays(13), today = monday)
        assertEquals(2, result.size)
        assertTrue(result.all { it.date.dayOfWeek == java.time.DayOfWeek.WEDNESDAY })
        // gleiche Ergebnisse wie Schritt für Schritt
        var d = wednesday
        val expected = mutableListOf<LocalDate>()
        while (!d.isAfter(later.plusDays(13))) { if (!d.isBefore(later)) expected += d; d = d.plusWeeks(1) }
        assertEquals(expected, result.map { it.date })
    }

    @Test
    fun `bestimmte Tage beim Vorlauf`() {
        val task = Task(id = 1, title = "Sport", date = monday, repeat = Repeat.DAYS, repeatDays = setOf(MONDAY, THURSDAY))
        val later = monday.plusYears(2)
        val result = task.occurrences(later, later.plusDays(6), today = monday)
        assertEquals(setOf(MONDAY, THURSDAY), result.map { it.date.dayOfWeek }.toSet())
        assertEquals(2, result.size)
    }

    @Test
    fun `Ueberfaelliges von heute aus zaehlt fuer heute`() {
        val task = Task(id = 1, title = "x", date = monday, repeat = Repeat.DAILY)
        val today = monday.plusDays(2)
        val o = listOf(task).overdueOccurrences(today).single()
        assertEquals(monday, o.date)
        assertEquals(today, o.countsFor)
        // im Kalender am ursprünglichen Tag zählt es für diesen Tag
        assertEquals(monday, task.occurrences(monday, monday, today).single().countsFor)
    }
}

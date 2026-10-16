package app.tagwerk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.TUESDAY
import java.time.LocalDate
import java.time.LocalTime

class JsonTest {

    @Test
    fun `Aufgabe uebersteht Speichern und Laden`() {
        val task = Task(
            id = 42,
            title = "Sport",
            note = "Laufschuhe",
            date = LocalDate.of(2026, 10, 13),
            time = LocalTime.of(7, 15),
            reminder = Reminder.MIN_15,
            repeat = Repeat.DAYS,
            repeatDays = setOf(TUESDAY, FRIDAY),
            listId = "L123",
            important = true,
            createdAt = 5,
            subtasks = listOf(Subtask(1, "Dehnen", done = true)),
            doneDates = setOf(LocalDate.of(2026, 10, 9)),
        )
        assertEquals(listOf(task), TaskJson.decode(TaskJson.encode(listOf(task))))
    }

    @Test
    fun `alte Daten mit fester Kategorie werden zur Liste`() {
        val old = """
            {"version":1,"tasks":[{"id":1,"title":"Milch","note":"","date":"2026-10-12",
            "reminder":"NONE","repeat":"WEEKLY","category":"SHOPPING","important":false,
            "done":false,"createdAt":3,"subtasks":[]}]}
        """.trimIndent()
        val task = TaskJson.decode(old).single()
        assertEquals(DefaultLists.SHOPPING, task.listId)
        assertEquals(Repeat.WEEKLY, task.repeat)
        assertEquals(emptySet<LocalDate>(), task.doneDates)
        assertEquals("Einkauf", Settings().list(task.listId).name)
    }

    @Test
    fun `Einstellungen ueberstehen Speichern und Laden`() {
        val settings = Settings(
            name = "Queen",
            accent = 0xFF123456.toInt(),
            lists = listOf(TaskList("WORK", "Büro", 0xFFCFE0F0.toInt()), TaskList("L1", "Reisen", 0xFFC8E8DC.toInt())),
        )
        assertEquals(settings, SettingsJson.decode(SettingsJson.encode(settings)))
    }

    @Test
    fun `leere oder kaputte Listen fallen auf die Standardlisten zurueck`() {
        val s = SettingsJson.decode("""{"name":"","accent":"kaputt","lists":[]}""")
        assertEquals("", s.name)
        assertEquals(Settings.DEFAULT_ACCENT, s.accent)
        assertEquals(DefaultLists.all, s.lists)
        assertEquals(Settings.DEFAULT_NAME, SettingsJson.decode("{}").name)
    }

    @Test
    fun `unbekannte Liste landet in der ersten`() {
        assertEquals(DefaultLists.PERSONAL, Settings().list("weg").id)
    }

    @Test
    fun `Farbcodes werden gelesen und geschrieben`() {
        assertEquals(0xFFF4B6A0.toInt(), parseColor("#F4B6A0"))
        assertEquals(0xFFF4B6A0.toInt(), parseColor(" f4b6a0 "))
        assertEquals(0xFFFFAA00.toInt(), parseColor("#FA0"))
        assertNull(parseColor("#12345"))
        assertNull(parseColor("grün"))
        assertEquals("#F4B6A0", colorToHex(0xFFF4B6A0.toInt()))
        assertEquals("#000000", colorToHex(0xFF000000.toInt()))
    }
}

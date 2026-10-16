package app.tagwerk.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

internal object TaskJson {

    fun encode(tasks: List<Task>): String {
        val array = JSONArray()
        tasks.forEach { array.put(encode(it)) }
        return JSONObject().put("version", 2).put("tasks", array).toString()
    }

    fun decode(text: String): List<Task> {
        val array = JSONObject(text).optJSONArray("tasks") ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            runCatching { decode(array.getJSONObject(i)) }.getOrNull()
        }.distinctBy { it.id } // doppelte IDs würden die Listen-Ansichten zum Absturz bringen
    }

    private fun encode(t: Task): JSONObject = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("note", t.note)
        t.date?.let { put("date", it.toString()) }
        t.time?.let { put("time", it.toString()) }
        put("reminder", t.reminder.name)
        put("repeat", t.repeat.name)
        if (t.repeatDays.isNotEmpty()) {
            put("repeatDays", JSONArray().apply { t.repeatDays.sorted().forEach { put(it.value) } })
        }
        // Schlüssel „category“ beibehalten: ältere Versionen speicherten hier die feste Kategorie,
        // deren Name jetzt die ID der Liste ist.
        put("category", t.listId)
        put("important", t.important)
        put("done", t.done)
        t.completedAt?.let { put("completedAt", it) }
        put("createdAt", t.createdAt)
        put("subtasks", JSONArray().apply {
            t.subtasks.forEach { s ->
                put(JSONObject().put("id", s.id).put("text", s.text).put("done", s.done))
            }
        })
        if (t.doneDates.isNotEmpty()) {
            put("doneDates", JSONArray().apply { t.doneDates.sorted().forEach { put(it.toString()) } })
        }
    }

    private fun decode(o: JSONObject): Task {
        val subs = o.optJSONArray("subtasks")
        val days = o.optJSONArray("repeatDays")
        val doneDates = o.optJSONArray("doneDates")
        return Task(
            id = o.getLong("id"),
            title = o.getString("title"),
            note = o.optString("note", ""),
            date = o.optString("date").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
            time = o.optString("time").takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
            reminder = enumOr(o.optString("reminder"), Reminder.NONE),
            repeat = enumOr(o.optString("repeat"), Repeat.NONE),
            repeatDays = if (days == null) emptySet() else (0 until days.length())
                .mapNotNull { i -> days.optInt(i, 0).takeIf { it in 1..7 }?.let(DayOfWeek::of) }
                .toSet(),
            listId = o.optString("category").ifEmpty { DefaultLists.PERSONAL },
            important = o.optBoolean("important", false),
            done = o.optBoolean("done", false),
            completedAt = if (o.has("completedAt")) o.getLong("completedAt") else null,
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            subtasks = if (subs == null) emptyList() else (0 until subs.length()).map { i ->
                val s = subs.getJSONObject(i)
                Subtask(s.getLong("id"), s.getString("text"), s.optBoolean("done", false))
            },
            doneDates = if (doneDates == null) emptySet() else (0 until doneDates.length())
                .mapNotNull { i -> runCatching { LocalDate.parse(doneDates.getString(i)) }.getOrNull() }
                .toSet(),
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback
}

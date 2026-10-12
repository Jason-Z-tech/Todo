package app.tagwerk.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

internal object TaskJson {

    fun encode(tasks: List<Task>): String {
        val array = JSONArray()
        tasks.forEach { array.put(encode(it)) }
        return JSONObject().put("version", 1).put("tasks", array).toString()
    }

    fun decode(text: String): List<Task> {
        val array = JSONObject(text).optJSONArray("tasks") ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            runCatching { decode(array.getJSONObject(i)) }.getOrNull()
        }
    }

    private fun encode(t: Task): JSONObject = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("note", t.note)
        t.date?.let { put("date", it.toString()) }
        t.time?.let { put("time", it.toString()) }
        put("reminder", t.reminder.name)
        put("repeat", t.repeat.name)
        put("category", t.category.name)
        put("important", t.important)
        put("done", t.done)
        t.completedAt?.let { put("completedAt", it) }
        put("createdAt", t.createdAt)
        put("subtasks", JSONArray().apply {
            t.subtasks.forEach { s ->
                put(JSONObject().put("id", s.id).put("text", s.text).put("done", s.done))
            }
        })
    }

    private fun decode(o: JSONObject): Task {
        val subs = o.optJSONArray("subtasks")
        return Task(
            id = o.getLong("id"),
            title = o.getString("title"),
            note = o.optString("note", ""),
            date = o.optString("date").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
            time = o.optString("time").takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
            reminder = enumOr(o.optString("reminder"), Reminder.NONE),
            repeat = enumOr(o.optString("repeat"), Repeat.NONE),
            category = enumOr(o.optString("category"), Category.PERSONAL),
            important = o.optBoolean("important", false),
            done = o.optBoolean("done", false),
            completedAt = if (o.has("completedAt")) o.getLong("completedAt") else null,
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            subtasks = if (subs == null) emptyList() else (0 until subs.length()).map { i ->
                val s = subs.getJSONObject(i)
                Subtask(s.getLong("id"), s.getString("text"), s.optBoolean("done", false))
            },
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback
}

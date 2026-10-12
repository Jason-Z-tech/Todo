package app.tagwerk.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import app.tagwerk.reminders.Notifications
import app.tagwerk.reminders.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * Hält alle Aufgaben im Speicher, schreibt sie als JSON-Datei und
 * sorgt dafür, dass die Erinnerungen immer zum aktuellen Stand passen.
 */
class TaskStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val file = AtomicFile(File(appContext.filesDir, "tasks.json"))
    private val _tasks = MutableStateFlow(loadInitial())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    fun find(id: Long): Task? = _tasks.value.firstOrNull { it.id == id }

    fun save(task: Task) {
        mutate { list ->
            if (list.any { it.id == task.id }) list.map { if (it.id == task.id) task else it }
            else list + task
        }
        ReminderScheduler.schedule(appContext, task)
    }

    fun delete(id: Long): Task? {
        val task = find(id) ?: return null
        mutate { list -> list.filterNot { it.id == id } }
        ReminderScheduler.cancel(appContext, id)
        Notifications.dismiss(appContext, id)
        return task
    }

    /**
     * Hakt eine Aufgabe ab (oder wieder auf). Wiederkehrende Aufgaben
     * springen dabei auf ihren nächsten Termin, statt erledigt zu bleiben.
     */
    fun setDone(id: Long, done: Boolean): Task? {
        val task = find(id) ?: return null
        val updated = if (done && task.repeat != Repeat.NONE && task.date != null) {
            val today = LocalDate.now()
            var next = task.repeat.next(task.date)
            while (next.isBefore(today)) next = task.repeat.next(next)
            task.copy(date = next, subtasks = task.subtasks.map { it.copy(done = false) })
        } else {
            task.copy(done = done, completedAt = if (done) System.currentTimeMillis() else null)
        }
        save(updated)
        if (done) Notifications.dismiss(appContext, id)
        return updated
    }

    fun toggleSubtask(taskId: Long, subtaskId: Long) {
        val task = find(taskId) ?: return
        save(task.copy(subtasks = task.subtasks.map {
            if (it.id == subtaskId) it.copy(done = !it.done) else it
        }))
    }

    fun clearCompleted() {
        mutate { list -> list.filterNot { it.done } }
    }

    fun rescheduleAll() {
        _tasks.value.forEach { ReminderScheduler.schedule(appContext, it) }
    }

    @Synchronized
    private fun mutate(transform: (List<Task>) -> List<Task>) {
        val updated = transform(_tasks.value)
        _tasks.value = updated
        persist(updated)
    }

    private fun persist(tasks: List<Task>) {
        val out = try {
            file.startWrite()
        } catch (e: Exception) {
            Log.e(TAG, "Konnte Datei nicht öffnen", e)
            return
        }
        try {
            out.write(TaskJson.encode(tasks).toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            Log.e(TAG, "Speichern fehlgeschlagen", e)
        }
    }

    private fun loadInitial(): List<Task> {
        if (!file.baseFile.exists()) {
            return sampleTasks().also { persist(it) }
        }
        return try {
            TaskJson.decode(String(file.readFully(), Charsets.UTF_8))
        } catch (e: Exception) {
            Log.e(TAG, "Laden fehlgeschlagen", e)
            emptyList()
        }
    }

    private fun sampleTasks(): List<Task> {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        return listOf(
            Task(
                id = now,
                title = "Tippe auf den Kreis, um abzuhaken",
                date = today,
                category = Category.PERSONAL,
                createdAt = now,
            ),
            Task(
                id = now + 1,
                title = "Nach links wischen zum Löschen",
                date = today,
                category = Category.IDEAS,
                createdAt = now + 1,
            ),
            Task(
                id = now + 2,
                title = "Wocheneinkauf",
                note = "Hafermilch, Äpfel, Basilikum",
                date = today.plusDays(1),
                time = LocalTime.of(17, 30),
                reminder = Reminder.MIN_15,
                category = Category.SHOPPING,
                createdAt = now + 2,
                subtasks = listOf(
                    Subtask(now + 10, "Hafermilch"),
                    Subtask(now + 11, "Äpfel"),
                    Subtask(now + 12, "Basilikum"),
                ),
            ),
        )
    }

    companion object {
        private const val TAG = "TaskStore"

        @Volatile
        private var instance: TaskStore? = null

        fun get(context: Context): TaskStore =
            instance ?: synchronized(this) {
                instance ?: TaskStore(context).also { instance = it }
            }
    }
}

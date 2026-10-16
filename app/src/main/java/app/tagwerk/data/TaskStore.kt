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
     * springen dabei auf ihren nächsten Termin, statt erledigt zu bleiben;
     * der abgehakte Tag wird gemerkt.
     */
    fun setDone(id: Long, done: Boolean): Task? {
        val task = find(id) ?: return null
        val updated = if (done && task.isRepeating) {
            completeRepeating(task, LocalDate.now())
        } else {
            task.copy(done = done, completedAt = if (done) System.currentTimeMillis() else null)
        }
        save(updated)
        if (done) Notifications.dismiss(appContext, id)
        return updated
    }

    /**
     * Hakt einen Eintrag aus Kalender, Tagesansicht oder Checkliste ab – oder wieder auf.
     * Bei wiederkehrenden Aufgaben wirkt das nur auf genau diesen Tag.
     */
    fun toggle(occurrence: Occurrence): Task? {
        val task = find(occurrence.task.id) ?: return null
        val day = occurrence.date
        return when (occurrence.kind) {
            Occurrence.Kind.OPEN -> {
                if (!task.isRepeating) return setDone(task.id, true)
                completeRepeating(task, occurrence.countsFor).also {
                    save(it)
                    Notifications.dismiss(appContext, task.id)
                }
            }
            Occurrence.Kind.UPCOMING -> {
                if (!task.isRepeating) return setDone(task.id, true)
                task.copy(doneDates = trimmed(task.doneDates + day)).also { save(it) }
            }
            Occurrence.Kind.DONE -> {
                if (!task.isRepeating || task.done) return setDone(task.id, false)
                val date = task.date
                task.copy(
                    doneDates = task.doneDates - day,
                    // Der Tag ist wieder offen – liegt er vor dem nächsten Termin, geht es dort weiter.
                    date = if (date == null || day.isBefore(date)) day else date,
                ).also { save(it) }
            }
        }
    }

    /**
     * Hakt den aktuellen (ggf. überfälligen) Termin ab und springt zum nächsten offenen
     * Termin ab [on]. Wird Verpasstes erst heute abgehakt, bleibt der heutige Termin
     * samt Erinnerung also offen.
     */
    private fun completeRepeating(task: Task, on: LocalDate): Task {
        val date = task.date ?: return task
        val doneDates = task.doneDates + date
        var next = task.nextAfter(date)
        var steps = 0
        while ((next.isBefore(on) || next in doneDates) && steps < 5000) {
            next = task.nextAfter(next)
            steps++
        }
        return task.copy(
            date = next,
            doneDates = trimmed(doneDates),
            subtasks = task.subtasks.map { it.copy(done = false) },
        )
    }

    /** Nur die letzten Tage behalten, damit die Datei nicht endlos wächst. */
    private fun trimmed(dates: Set<LocalDate>): Set<LocalDate> =
        if (dates.size <= MAX_DONE_DATES) dates else dates.sortedDescending().take(MAX_DONE_DATES).toSet()

    /** Verschiebt alle Aufgaben einer Liste in eine andere (z. B. beim Löschen der Liste). */
    fun moveToList(fromId: String, toId: String) {
        mutate { list -> list.map { if (it.listId == fromId) it.copy(listId = toId) else it } }
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
                listId = DefaultLists.PERSONAL,
                createdAt = now,
            ),
            Task(
                id = now + 1,
                title = "Nach links wischen zum Löschen",
                date = today,
                listId = DefaultLists.IDEAS,
                createdAt = now + 1,
            ),
            Task(
                id = now + 2,
                title = "Wocheneinkauf",
                note = "Hafermilch, Äpfel, Basilikum",
                date = today.plusDays(1),
                time = LocalTime.of(17, 30),
                reminder = Reminder.MIN_15,
                listId = DefaultLists.SHOPPING,
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
        private const val MAX_DONE_DATES = 400

        @Volatile
        private var instance: TaskStore? = null

        fun get(context: Context): TaskStore =
            instance ?: synchronized(this) {
                instance ?: TaskStore(context).also { instance = it }
            }
    }
}

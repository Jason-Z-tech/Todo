package app.tagwerk.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.tagwerk.data.TaskStore

/** Wird vom AlarmManager geweckt und zeigt die Erinnerung an. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1)
        val task = TaskStore.get(context).find(id) ?: return
        if (!task.done) Notifications.show(context, task)
    }
}

/** Knöpfe in der Benachrichtigung: „Erledigt“ und „Später“. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1)
        if (id < 0) return
        Notifications.dismiss(context, id)
        when (intent.action) {
            Notifications.ACTION_DONE -> TaskStore.get(context).setDone(id, true)
            Notifications.ACTION_SNOOZE -> ReminderScheduler.snooze(context, id, Notifications.SNOOZE_MINUTES)
        }
    }
}

/** Nach Neustart, Update oder Zeitumstellung alle Erinnerungen neu planen. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TaskStore.get(context).rescheduleAll()
    }
}

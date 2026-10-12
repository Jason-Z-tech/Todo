package app.tagwerk.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import app.tagwerk.data.Task
import java.time.ZoneId

object ReminderScheduler {

    const val ACTION_REMIND = "app.tagwerk.action.REMIND"
    const val EXTRA_TASK_ID = "task_id"

    fun schedule(context: Context, task: Task) {
        cancel(context, task.id)
        if (task.done) return
        val at = task.reminderAt() ?: return
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (millis <= System.currentTimeMillis()) return
        setAlarm(context, task.id, millis)
    }

    fun snooze(context: Context, taskId: Long, minutes: Long) {
        setAlarm(context, taskId, System.currentTimeMillis() + minutes * 60_000)
    }

    fun cancel(context: Context, taskId: Long) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context, taskId))
    }

    private fun setAlarm(context: Context, taskId: Long, triggerAt: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context, taskId)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun pendingIntent(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
            .setData(taskUri(taskId))
            .putExtra(EXTRA_TASK_ID, taskId)
        return PendingIntent.getBroadcast(
            context,
            requestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    internal fun taskUri(taskId: Long): Uri = Uri.parse("tagwerk://task/$taskId")

    internal fun requestCode(taskId: Long): Int = (taskId xor (taskId ushr 32)).toInt()
}

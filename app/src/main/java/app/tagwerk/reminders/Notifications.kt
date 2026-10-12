package app.tagwerk.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.tagwerk.MainActivity
import app.tagwerk.R
import app.tagwerk.data.Task
import app.tagwerk.ui.formatDay
import app.tagwerk.ui.formatTime

object Notifications {

    private const val CHANNEL_ID = "reminders"
    const val ACTION_DONE = "app.tagwerk.action.DONE"
    const val ACTION_SNOOZE = "app.tagwerk.action.SNOOZE"
    const val SNOOZE_MINUTES = 10L

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Erinnerungen",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Erinnerungen an deine Aufgaben"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, task: Task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        createChannel(context)
        val code = ReminderScheduler.requestCode(task.id)

        val open = PendingIntent.getActivity(
            context,
            code,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val text = buildString {
            val day = task.date
            val time = task.time
            if (day != null && time != null) append("${formatDay(day)} um ${formatTime(time)}")
            if (task.note.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append(task.note)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tagwerk)
            .setColor(0xFFE0907A.toInt())
            .setContentTitle(task.title)
            .setContentText(text.ifEmpty { "Erinnerung" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.ifEmpty { "Erinnerung" }))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, "Erledigt", actionIntent(context, ACTION_DONE, task.id))
            .addAction(0, "$SNOOZE_MINUTES Min später", actionIntent(context, ACTION_SNOOZE, task.id))
            .build()

        NotificationManagerCompat.from(context).notify(code, notification)
    }

    fun dismiss(context: Context, taskId: Long) {
        NotificationManagerCompat.from(context).cancel(ReminderScheduler.requestCode(taskId))
    }

    private fun actionIntent(context: Context, action: String, taskId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            ReminderScheduler.requestCode(taskId),
            Intent(context, ActionReceiver::class.java)
                .setAction(action)
                .setData(ReminderScheduler.taskUri(taskId))
                .putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

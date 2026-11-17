package com.example.myapplication.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.SpecialReminder
import com.example.myapplication.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val CHANNEL_ID = "todo_deadlines"
const val EXTRA_TYPE = "notif_type"
const val TYPE_TASK = "task"
const val TYPE_SPECIAL_REMINDER = "special_reminder"

const val EXTRA_TASK_TITLE = "task_title"
const val EXTRA_MINUTES_BEFORE = "minutes_before"
const val EXTRA_DEADLINE_MS = "deadline_ms"

const val EXTRA_REMINDER_ID = "reminder_id"
const val EXTRA_EMOJI = "reminder_emoji"
const val EXTRA_ADVANCE_DAYS = "advance_days"
const val EXTRA_DAY = "reminder_day"
const val EXTRA_MONTH = "reminder_month"
const val EXTRA_YEAR = "reminder_year"
const val EXTRA_NOTES = "reminder_notes"

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notifType = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_TASK

        if (notifType == TYPE_SPECIAL_REMINDER) {
            handleSpecialReminder(context, intent)
        } else {
            handleTaskDeadline(context, intent)
        }
    }

    private fun handleSpecialReminder(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, 0L)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: return
        val emoji = intent.getStringExtra(EXTRA_EMOJI) ?: "🎂"
        val advanceDays = intent.getIntExtra(EXTRA_ADVANCE_DAYS, 0)
        val day = intent.getIntExtra(EXTRA_DAY, 1)
        val month = intent.getIntExtra(EXTRA_MONTH, 1)
        val year = intent.getIntExtra(EXTRA_YEAR, -1)
        val notes = intent.getStringExtra(EXTRA_NOTES) ?: ""

        val monthNames = listOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
        )
        val monthStr = monthNames.getOrElse(month - 1) { "" }
        val dateStr = "$day $monthStr"

        val notifTitle = when (advanceDays) {
            0 -> "$emoji Сегодня: $title!"
            1 -> "$emoji Завтра: $title!"
            3 -> "$emoji Через 3 дня: $title!"
            7 -> "$emoji Через неделю: $title!"
            else -> "$emoji Через $advanceDays дн.: $title"
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val ageStr = if (year > 0) " (исполняется ${currentYear - year} лет)" else ""
        val notesStr = if (notes.isNotBlank()) " • $notes" else ""
        val notifText = "$title • $dateStr$ageStr$notesStr"

        showNotification(context, notifTitle, notifText, ("special_$reminderId").hashCode())

        // Записываем уведомление в историю и перепланируем на следующий год
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = TaskRepository(context.applicationContext)
                repo.addNotification(notifTitle, notifText)
                val appData = repo.appData.first()
                appData.specialReminders.find { it.id == reminderId }?.let { reminder ->
                    scheduleSpecialReminderNotification(context, reminder)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleTaskDeadline(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: return
        val minutesBefore = intent.getIntExtra(EXTRA_MINUTES_BEFORE, 0)
        val deadlineMs = intent.getLongExtra(EXTRA_DEADLINE_MS, 0L)

        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val deadlineTimeStr = if (deadlineMs > 0) " (${timeFormatter.format(Date(deadlineMs))})" else ""

        val (notifTitle, notifText) = when (minutesBefore) {
            0 -> "⏰ Время задачи$deadlineTimeStr" to "«$title» нужно сделать прямо сейчас!"
            15 -> "⏳ Через 15 минут$deadlineTimeStr" to "Дедлайн задачи «$title» через 15 минут"
            30 -> "⏳ Через 30 минут$deadlineTimeStr" to "Дедлайн задачи «$title» через 30 минут"
            60 -> "⏳ Через 1 час$deadlineTimeStr" to "Дедлайн задачи «$title» через 1 час"
            1440 -> "📅 Завтра дедлайн" to "«$title» нужно выполнить завтра$deadlineTimeStr"
            else -> "⏰ Напоминание о задаче" to "Дедлайн «$title» приближается$deadlineTimeStr"
        }

        showNotification(context, notifTitle, notifText, title.hashCode())

        // Сохраняем уведомление в историю внутри приложения
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TaskRepository(context.applicationContext).addNotification(notifTitle, notifText)
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}

fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Дедлайны и напоминания",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Напоминания о сроках выполнения задач и днях рождения"
        enableVibration(true)
    }
    val manager = context.getSystemService(NotificationManager::class.java)
    manager?.createNotificationChannel(channel)
}

fun showNotification(context: Context, title: String, text: String, id: Int) {
    val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    val manager = context.getSystemService(NotificationManager::class.java)
    manager?.notify(id, notification)
}

fun scheduleTaskNotification(
    context: Context,
    taskId: Long,
    taskTitle: String,
    deadlineMs: Long,
    reminderMinutesBefore: Int?
) {
    if (reminderMinutesBefore == null) return

    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    val triggerTimeMs = deadlineMs - (reminderMinutesBefore * 60_000L)
    val nowMs = System.currentTimeMillis()

    if (triggerTimeMs <= nowMs) return

    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra(EXTRA_TYPE, TYPE_TASK)
        putExtra(EXTRA_TASK_TITLE, taskTitle)
        putExtra(EXTRA_MINUTES_BEFORE, reminderMinutesBefore)
        putExtra(EXTRA_DEADLINE_MS, deadlineMs)
    }

    val reqCode = (taskId % 100_000).toInt()
    val pendingIntent = PendingIntent.getBroadcast(
        context, reqCode, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        }
    } catch (_: SecurityException) {
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
    }
}

fun cancelTaskNotification(context: Context, taskId: Long) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    val reqCode = (taskId % 100_000).toInt()
    val intent = Intent(context, NotificationReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context, reqCode, intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    pi?.let { alarmManager.cancel(it) }
}

fun calculateSpecialReminderTriggerMs(reminder: SpecialReminder): Long {
    val now = Calendar.getInstance()
    val thisYear = now.get(Calendar.YEAR)

    // Проверяем для текущего года
    val eventCalThisYear = Calendar.getInstance().apply {
        set(Calendar.YEAR, thisYear)
        set(Calendar.MONTH, reminder.month - 1)
        val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
        set(Calendar.DAY_OF_MONTH, reminder.day.coerceAtMost(maxDay))
        set(Calendar.HOUR_OF_DAY, reminder.notifyHour)
        set(Calendar.MINUTE, reminder.notifyMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val triggerThisYear = (eventCalThisYear.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -reminder.advanceDays)
    }

    return if (triggerThisYear.timeInMillis > now.timeInMillis) {
        triggerThisYear.timeInMillis
    } else {
        // Если в текущем году дата напоминания уже прошла, ставим на следующий год
        val eventCalNextYear = Calendar.getInstance().apply {
            set(Calendar.YEAR, thisYear + 1)
            set(Calendar.MONTH, reminder.month - 1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, reminder.day.coerceAtMost(maxDay))
            set(Calendar.HOUR_OF_DAY, reminder.notifyHour)
            set(Calendar.MINUTE, reminder.notifyMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerNextYear = (eventCalNextYear.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -reminder.advanceDays)
        }
        triggerNextYear.timeInMillis
    }
}

fun scheduleSpecialReminderNotification(context: Context, reminder: SpecialReminder) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    val triggerTimeMs = calculateSpecialReminderTriggerMs(reminder)
    val nowMs = System.currentTimeMillis()
    if (triggerTimeMs <= nowMs) return

    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra(EXTRA_TYPE, TYPE_SPECIAL_REMINDER)
        putExtra(EXTRA_REMINDER_ID, reminder.id)
        putExtra(EXTRA_TASK_TITLE, reminder.title)
        putExtra(EXTRA_EMOJI, reminder.emoji)
        putExtra(EXTRA_ADVANCE_DAYS, reminder.advanceDays)
        putExtra(EXTRA_DAY, reminder.day)
        putExtra(EXTRA_MONTH, reminder.month)
        putExtra(EXTRA_YEAR, reminder.year ?: -1)
        putExtra(EXTRA_NOTES, reminder.notes)
    }

    val reqCode = (reminder.id % 100_000).toInt() + 200_000
    val pendingIntent = PendingIntent.getBroadcast(
        context, reqCode, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        }
    } catch (_: SecurityException) {
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
    }
}

fun cancelSpecialReminderNotification(context: Context, reminderId: Long) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    val reqCode = (reminderId % 100_000).toInt() + 200_000
    val intent = Intent(context, NotificationReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context, reqCode, intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    pi?.let { alarmManager.cancel(it) }
}

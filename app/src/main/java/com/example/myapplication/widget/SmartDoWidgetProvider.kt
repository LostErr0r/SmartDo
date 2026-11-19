package com.example.myapplication.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.TaskRepository
import com.example.myapplication.ui.getDaysUntilEvent
import com.example.myapplication.ui.isDueToday
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmartDoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val repo = TaskRepository(context)
        val dateFormatter = SimpleDateFormat("d MMMM", Locale("ru"))
        val todayStr = dateFormatter.format(Date())

        CoroutineScope(Dispatchers.IO).launch {
            val appData = try {
                repo.appData.first()
            } catch (e: Exception) {
                null
            }

            val todayCount = appData?.categories?.sumOf { cat ->
                cat.tasks.count { it.isDueToday() && !it.isDone }
            } ?: 0

            val upcomingReminder = appData?.specialReminders?.minByOrNull {
                getDaysUntilEvent(it.day, it.month)
            }

            val upcomingText = if (upcomingReminder != null) {
                val days = getDaysUntilEvent(upcomingReminder.day, upcomingReminder.month)
                val dayStr = when (days) {
                    0 -> "Сегодня! 🎉"
                    1 -> "Завтра"
                    else -> "через $days дн."
                }
                "${upcomingReminder.emoji} $dayStr: ${upcomingReminder.title}"
            } else {
                "Нет ближайших праздников"
            }

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_smartdo).apply {
                    setTextViewText(R.id.widget_date, todayStr)
                    setTextViewText(R.id.widget_tasks_count, "$todayCount")
                    setTextViewText(
                        R.id.widget_tasks_subtext,
                        if (todayCount == 0) "Все дела выполнены ✨" else "Осталось выполнить"
                    )
                    setTextViewText(R.id.widget_events_text, upcomingText)

                    // Клик по виджету открывает SmartDo
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, SmartDoWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val widgetManager = AppWidgetManager.getInstance(context)
                val ids = widgetManager.getAppWidgetIds(
                    ComponentName(context, SmartDoWidgetProvider::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}

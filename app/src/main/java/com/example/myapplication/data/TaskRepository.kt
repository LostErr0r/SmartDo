package com.example.myapplication.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.widget.SmartDoWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "todo_data")
private val DATA_KEY = stringPreferencesKey("categories_json")

val appJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}

@Serializable
data class Subtask(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val isDone: Boolean = false
)

@Serializable
data class Task(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val description: String = "",
    val priority: Int = 0, // 0 = обычный, 1 = средний 🟡, 2 = высокий 🔴
    val isDone: Boolean = false,
    val deadlineMs: Long? = null,
    val reminderMinutesBefore: Int? = 0,
    val subtasks: List<Subtask> = emptyList()
)

@Serializable
data class Category(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val emoji: String = "📁",
    val tasks: List<Task> = emptyList()
)

@Serializable
data class ArchivedTask(
    val id: Long = System.currentTimeMillis(),
    val originalTaskId: Long = 0L,
    val title: String,
    val description: String = "",
    val categoryId: Long = 0L,
    val categoryName: String = "",
    val categoryEmoji: String = "📁",
    val reason: String = "deleted", // "deleted" или "completed"
    val archivedAt: Long = System.currentTimeMillis(),
    val priority: Int = 0,
    val subtasks: List<Subtask> = emptyList()
)

@Serializable
data class AppNotification(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Serializable
data class SpecialReminder(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val day: Int,          // 1..31
    val month: Int,        // 1..12
    val year: Int? = null, // Опциональный год рождения/начала события
    val emoji: String = "🎂",
    val advanceDays: Int = 1, // 0 = в день события, 1 = за 1 день, 3 = за 3 дня, 7 = за неделю
    val notifyHour: Int = 9,  // Час напоминания (по умолчанию 09:00)
    val notifyMinute: Int = 0,
    val notes: String = ""    // Заметки (идеи подарков, адрес и т.д.)
)

@Serializable
data class AppData(
    val categories: List<Category> = emptyList(),
    val archivedTasks: List<ArchivedTask> = emptyList(),
    val notifications: List<AppNotification> = emptyList(),
    val specialReminders: List<SpecialReminder> = emptyList(),
    val themeMode: String = "system",
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val confirmDeleteEnabled: Boolean = false,
    val taskSortMode: String = "manual", // manual | deadline | priority | alphabetical
    val hideCompleted: Boolean = false
)

enum class SmartListType(val title: String, val emoji: String) {
    TODAY("Сегодня", "📅"),
    SCHEDULED("Запланировано", "⏰"),
    OVERDUE("Просрочено", "🔴"),
    ALL("Все задачи", "📦")
}

class TaskRepository(private val context: Context) {

    fun getContext(): Context = context

    val appData: Flow<AppData> = context.dataStore.data.map { prefs ->
        val json = prefs[DATA_KEY] ?: "{}"
        try {
            appJson.decodeFromString<AppData>(json)
        } catch (e: Exception) {
            AppData()
        }
    }

    suspend fun saveData(data: AppData) {
        context.dataStore.edit { prefs ->
            prefs[DATA_KEY] = appJson.encodeToString(data)
        }
        try {
            SmartDoWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {}
    }

    suspend fun addNotification(title: String, message: String) {
        val current = appData.first()
        val newNotif = AppNotification(title = title, message = message)
        val updated = current.copy(
            notifications = listOf(newNotif) + current.notifications.take(49)
        )
        saveData(updated)
    }
}

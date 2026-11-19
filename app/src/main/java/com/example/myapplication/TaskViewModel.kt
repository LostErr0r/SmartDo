package com.example.myapplication

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppData
import com.example.myapplication.data.AppNotification
import com.example.myapplication.data.ArchivedTask
import com.example.myapplication.data.Category
import com.example.myapplication.data.SpecialReminder
import com.example.myapplication.data.Subtask
import com.example.myapplication.data.Task
import com.example.myapplication.data.TaskRepository
import com.example.myapplication.data.appJson
import com.example.myapplication.notifications.cancelSpecialReminderNotification
import com.example.myapplication.notifications.cancelTaskNotification
import com.example.myapplication.notifications.scheduleSpecialReminderNotification
import com.example.myapplication.notifications.scheduleTaskNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

/** Снимок последней удалённой задачи — для отмены удаления (Undo). */
data class DeletedTaskSnapshot(
    val categoryId: Long,
    val task: Task,
    val archiveId: Long
)

/** Следующая дата повторения относительно дедлайна, либо null если повтор выключен. */
private fun nextRepeatDeadline(deadlineMs: Long, rule: String): Long? {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = deadlineMs }
    when (rule) {
        "daily" -> cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        "weekly" -> cal.add(java.util.Calendar.DAY_OF_MONTH, 7)
        "monthly" -> cal.add(java.util.Calendar.MONTH, 1)
        "yearly" -> cal.add(java.util.Calendar.YEAR, 1)
        else -> return null
    }
    return cal.timeInMillis
}

/** Создаёт следующий экземпляр повторяющейся задачи, либо null. */
private fun Task.nextOccurrence(): Task? {
    if (repeatRule == "none" || deadlineMs == null) return null
    val nd = nextRepeatDeadline(deadlineMs, repeatRule) ?: return null
    return Task(
        title = title,
        description = description,
        priority = priority,
        isDone = false,
        deadlineMs = nd,
        reminderMinutesBefore = reminderMinutesBefore,
        subtasks = subtasks.map { it.copy(isDone = false) },
        repeatRule = repeatRule
    )
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository(application.applicationContext)

    private val _appData = MutableStateFlow(AppData())
    val appData: StateFlow<AppData> = _appData.asStateFlow()

    private var lastDeleted: DeletedTaskSnapshot? = null

    init {
        viewModelScope.launch {
            repository.appData.collect { data ->
                _appData.value = data
            }
        }
    }

    // ── Тема ──────────────────────────────────────────────

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.saveData(appData.value.copy(themeMode = mode))
        }
    }

    // ── Настройки приложения (звук / тактильный отклик) ───

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveData(appData.value.copy(soundEnabled = enabled))
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveData(appData.value.copy(hapticsEnabled = enabled))
        }
    }

    fun setConfirmDeleteEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveData(appData.value.copy(confirmDeleteEnabled = enabled))
        }
    }

    // ── Сортировка и фильтр списков ───────────────────────

    fun setTaskSortMode(mode: String) {
        viewModelScope.launch {
            repository.saveData(appData.value.copy(taskSortMode = mode))
        }
    }

    fun setHideCompleted(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveData(appData.value.copy(hideCompleted = enabled))
        }
    }

    // ── Категории ─────────────────────────────────────────

    fun addCategory(name: String, emoji: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val cat = Category(name = name.trim(), emoji = emoji)
            val updated = appData.value.categories + cat
            repository.saveData(appData.value.copy(categories = updated))
        }
    }

    fun updateCategory(id: Long, name: String, emoji: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val updated = appData.value.categories.map { cat ->
                if (cat.id == id) cat.copy(name = name.trim(), emoji = emoji)
                else cat
            }
            repository.saveData(appData.value.copy(categories = updated))
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            val updated = appData.value.categories.filter { it.id != id }
            repository.saveData(appData.value.copy(categories = updated))
        }
    }

    // ── Задачи ────────────────────────────────────────────

    fun addTask(
        categoryId: Long,
        title: String,
        description: String = "",
        priority: Int = 0,
        deadlineMs: Long? = null,
        reminderMinutesBefore: Int? = null,
        subtasks: List<Subtask> = emptyList(),
        repeatRule: String = "none"
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = Task(
                title = title.trim(),
                description = description.trim(),
                priority = priority,
                deadlineMs = deadlineMs,
                reminderMinutesBefore = reminderMinutesBefore,
                subtasks = subtasks,
                repeatRule = repeatRule
            )
            val updated = appData.value.categories.map { cat ->
                if (cat.id == categoryId) cat.copy(tasks = cat.tasks + task)
                else cat
            }
            repository.saveData(appData.value.copy(categories = updated))
            if (deadlineMs != null) {
                scheduleTaskNotification(getApplication(), task.id, task.title, deadlineMs, reminderMinutesBefore)
            }
        }
    }

    fun updateTask(
        categoryId: Long,
        taskId: Long,
        title: String,
        description: String,
        priority: Int,
        deadlineMs: Long?,
        reminderMinutesBefore: Int?,
        subtasks: List<Subtask> = emptyList(),
        repeatRule: String = "none"
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            cancelTaskNotification(getApplication(), taskId)
            val updated = appData.value.categories.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(tasks = cat.tasks.map { task ->
                        if (task.id == taskId) task.copy(
                            title = title.trim(),
                            description = description.trim(),
                            priority = priority,
                            deadlineMs = deadlineMs,
                            reminderMinutesBefore = reminderMinutesBefore,
                            subtasks = subtasks,
                            repeatRule = repeatRule
                        ) else task
                    })
                } else cat
            }
            repository.saveData(appData.value.copy(categories = updated))
            if (deadlineMs != null) {
                scheduleTaskNotification(getApplication(), taskId, title.trim(), deadlineMs, reminderMinutesBefore)
            }
        }
    }

    fun toggleTask(categoryId: Long, taskId: Long) {
        viewModelScope.launch {
            var toggledTask: Task? = null
            var catName = ""
            var catEmoji = ""
            val updated = appData.value.categories.map { cat ->
                if (cat.id == categoryId) {
                    catName = cat.name
                    catEmoji = cat.emoji
                    cat.copy(tasks = cat.tasks.map { task ->
                        if (task.id == taskId) {
                            val newTask = task.copy(isDone = !task.isDone)
                            toggledTask = newTask
                            newTask
                        } else task
                    })
                } else cat
            }
            if (toggledTask?.isDone == true) {
                cancelTaskNotification(getApplication(), taskId)
                val notif = AppNotification(
                    title = "✅ Задача выполнена",
                    message = "«${toggledTask!!.title}» в «$catEmoji $catName»"
                )
                val notifs = listOf(notif) + appData.value.notifications.take(99)
                // Повтор: создаём следующий экземпляр в той же категории.
                val spawn = toggledTask!!.nextOccurrence()
                val cats = if (spawn != null) {
                    updated.map { if (it.id == categoryId) it.copy(tasks = it.tasks + spawn) else it }
                } else updated
                repository.saveData(appData.value.copy(categories = cats, notifications = notifs))
                if (spawn?.deadlineMs != null) {
                    scheduleTaskNotification(
                        getApplication(), spawn.id, spawn.title, spawn.deadlineMs, spawn.reminderMinutesBefore
                    )
                }
            } else {
                repository.saveData(appData.value.copy(categories = updated))
            }
        }
    }

    fun deleteTask(categoryId: Long, taskId: Long) {
        viewModelScope.launch {
            cancelTaskNotification(getApplication(), taskId)
            var deletedEntry: ArchivedTask? = null
            var deletedTask: Task? = null
            val updated = appData.value.categories.map { cat ->
                if (cat.id == categoryId) {
                    val task = cat.tasks.find { it.id == taskId }
                    deletedTask = task
                    deletedEntry = task?.let {
                        ArchivedTask(
                            originalTaskId = task.id,
                            title = task.title,
                            description = task.description,
                            categoryId = cat.id,
                            categoryName = cat.name,
                            categoryEmoji = cat.emoji,
                            reason = "deleted",
                            priority = task.priority,
                            subtasks = task.subtasks
                        )
                    }
                    cat.copy(tasks = cat.tasks.filter { it.id != taskId })
                } else cat
            }
            val newArchive = if (deletedEntry != null) {
                listOf(deletedEntry!!) + appData.value.archivedTasks
            } else appData.value.archivedTasks
            repository.saveData(appData.value.copy(categories = updated, archivedTasks = newArchive))
            val task = deletedTask
            val entry = deletedEntry
            if (task != null && entry != null) {
                lastDeleted = DeletedTaskSnapshot(categoryId, task, entry.id)
            }
        }
    }

    /** Отменить последнее удаление: вернуть задачу в её категорию и убрать запись из архива. */
    fun undoDelete() {
        val snap = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch {
            val cats = appData.value.categories.map { cat ->
                if (cat.id == snap.categoryId) cat.copy(tasks = cat.tasks + snap.task) else cat
            }
            val archive = appData.value.archivedTasks.filter { it.id != snap.archiveId }
            repository.saveData(appData.value.copy(categories = cats, archivedTasks = archive))
            if (!snap.task.isDone && snap.task.deadlineMs != null) {
                scheduleTaskNotification(
                    getApplication(), snap.task.id, snap.task.title,
                    snap.task.deadlineMs, snap.task.reminderMinutesBefore
                )
            }
        }
    }

    fun toggleTaskGlobal(taskId: Long) {
        viewModelScope.launch {
            var toggledTask: Task? = null
            var toggledCatId = 0L
            var catName = ""
            var catEmoji = ""
            val updated = appData.value.categories.map { cat ->
                cat.copy(tasks = cat.tasks.map { task ->
                    if (task.id == taskId) {
                        catName = cat.name
                        catEmoji = cat.emoji
                        toggledCatId = cat.id
                        val newTask = task.copy(isDone = !task.isDone)
                        toggledTask = newTask
                        newTask
                    } else task
                })
            }
            if (toggledTask?.isDone == true) {
                cancelTaskNotification(getApplication(), taskId)
                val notif = AppNotification(
                    title = "✅ Задача выполнена",
                    message = "«${toggledTask!!.title}» в «$catEmoji $catName»"
                )
                val notifs = listOf(notif) + appData.value.notifications.take(99)
                val spawn = toggledTask!!.nextOccurrence()
                val cats = if (spawn != null) {
                    updated.map { if (it.id == toggledCatId) it.copy(tasks = it.tasks + spawn) else it }
                } else updated
                repository.saveData(appData.value.copy(categories = cats, notifications = notifs))
                if (spawn?.deadlineMs != null) {
                    scheduleTaskNotification(
                        getApplication(), spawn.id, spawn.title, spawn.deadlineMs, spawn.reminderMinutesBefore
                    )
                }
            } else {
                repository.saveData(appData.value.copy(categories = updated))
            }
        }
    }

    fun deleteTaskGlobal(taskId: Long) {
        viewModelScope.launch {
            cancelTaskNotification(getApplication(), taskId)
            var deletedEntry: ArchivedTask? = null
            var deletedTask: Task? = null
            var fromCategoryId: Long = 0L
            val updated = appData.value.categories.map { cat ->
                val task = cat.tasks.find { it.id == taskId }
                if (task != null) {
                    deletedTask = task
                    fromCategoryId = cat.id
                    deletedEntry = ArchivedTask(
                        originalTaskId = task.id,
                        title = task.title,
                        description = task.description,
                        categoryId = cat.id,
                        categoryName = cat.name,
                        categoryEmoji = cat.emoji,
                        reason = "deleted",
                        priority = task.priority,
                        subtasks = task.subtasks
                    )
                    cat.copy(tasks = cat.tasks.filter { it.id != taskId })
                } else cat
            }
            val newArchive = if (deletedEntry != null) {
                listOf(deletedEntry!!) + appData.value.archivedTasks
            } else appData.value.archivedTasks
            repository.saveData(appData.value.copy(categories = updated, archivedTasks = newArchive))
            val task = deletedTask
            val entry = deletedEntry
            if (task != null && entry != null) {
                lastDeleted = DeletedTaskSnapshot(fromCategoryId, task, entry.id)
            }
        }
    }

    // ── Подзадачи (Subtasks / Checklist) ─────────────────

    fun addSubtask(categoryId: Long, taskId: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val updated = appData.value.categories.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(tasks = cat.tasks.map { task ->
                        if (task.id == taskId) {
                            val sub = Subtask(title = title.trim())
                            task.copy(subtasks = task.subtasks + sub)
                        } else task
                    })
                } else cat
            }
            repository.saveData(appData.value.copy(categories = updated))
        }
    }

    fun toggleSubtask(categoryId: Long, taskId: Long, subtaskId: Long) {
        viewModelScope.launch {
            val updated = appData.value.categories.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(tasks = cat.tasks.map { task ->
                        if (task.id == taskId) {
                            task.copy(subtasks = task.subtasks.map { sub ->
                                if (sub.id == subtaskId) sub.copy(isDone = !sub.isDone) else sub
                            })
                        } else task
                    })
                } else cat
            }
            repository.saveData(appData.value.copy(categories = updated))
        }
    }

    fun toggleSubtaskGlobal(taskId: Long, subtaskId: Long) {
        viewModelScope.launch {
            val updated = appData.value.categories.map { cat ->
                cat.copy(tasks = cat.tasks.map { task ->
                    if (task.id == taskId) {
                        task.copy(subtasks = task.subtasks.map { sub ->
                            if (sub.id == subtaskId) sub.copy(isDone = !sub.isDone) else sub
                        })
                    } else task
                })
            }
            repository.saveData(appData.value.copy(categories = updated))
        }
    }

    fun updateSubtasks(categoryId: Long, taskId: Long, subtasks: List<Subtask>) {
        viewModelScope.launch {
            val updated = appData.value.categories.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(tasks = cat.tasks.map { task ->
                        if (task.id == taskId) task.copy(subtasks = subtasks) else task
                    })
                } else cat
            }
            repository.saveData(appData.value.copy(categories = updated))
        }
    }

    // ── Архив ──────────────────────────────────────────────

    fun archiveDoneTasksInCategory(categoryId: Long) {
        viewModelScope.launch {
            var newEntries = listOf<ArchivedTask>()
            val updated = appData.value.categories.map { cat ->
                if (cat.id == categoryId) {
                    val doneTasks = cat.tasks.filter { it.isDone }
                    newEntries = doneTasks.map { task ->
                        ArchivedTask(
                            originalTaskId = task.id,
                            title = task.title,
                            description = task.description,
                            categoryId = cat.id,
                            categoryName = cat.name,
                            categoryEmoji = cat.emoji,
                            reason = "completed",
                            priority = task.priority,
                            subtasks = task.subtasks
                        )
                    }
                    cat.copy(tasks = cat.tasks.filter { !it.isDone })
                } else cat
            }
            val newArchive = newEntries + appData.value.archivedTasks
            repository.saveData(appData.value.copy(categories = updated, archivedTasks = newArchive))
        }
    }

    fun restoreArchivedTask(archivedId: Long) {
        viewModelScope.launch {
            val entry = appData.value.archivedTasks.find { it.id == archivedId } ?: return@launch
            val task = Task(
                id = entry.originalTaskId,
                title = entry.title,
                description = entry.description,
                priority = entry.priority,
                subtasks = entry.subtasks
            )
            val updated = appData.value.categories.map { cat ->
                if (cat.id == entry.categoryId) cat.copy(tasks = cat.tasks + task)
                else cat
            }
            val newArchive = appData.value.archivedTasks.filter { it.id != archivedId }
            repository.saveData(appData.value.copy(categories = updated, archivedTasks = newArchive))
        }
    }

    fun deleteArchivedTask(archivedId: Long) {
        viewModelScope.launch {
            val newArchive = appData.value.archivedTasks.filter { it.id != archivedId }
            repository.saveData(appData.value.copy(archivedTasks = newArchive))
        }
    }

    fun clearAllArchive() {
        viewModelScope.launch {
            repository.saveData(appData.value.copy(archivedTasks = emptyList()))
        }
    }

    // ── Уведомления ─────────────────────────────────────────

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            val updated = appData.value.notifications.map { it.copy(isRead = true) }
            repository.saveData(appData.value.copy(notifications = updated))
        }
    }

    /** Alias for markAllNotificationsRead() — called by NotificationsScreen */
    fun markNotificationsAsRead() = markAllNotificationsRead()

    fun clearNotifications() {
        viewModelScope.launch {
            repository.saveData(appData.value.copy(notifications = emptyList()))
        }
    }

    /** Alias for clearNotifications() — called by NotificationsScreen */
    fun clearAllNotifications() = clearNotifications()

    // ── Памятные даты и дни рождения ───────────────────────

    fun addSpecialReminder(
        title: String,
        day: Int,
        month: Int,
        year: Int?,
        emoji: String,
        advanceDays: Int,
        notifyHour: Int,
        notifyMinute: Int,
        notes: String
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val reminder = SpecialReminder(
                title = title.trim(),
                day = day,
                month = month,
                year = year,
                emoji = emoji,
                advanceDays = advanceDays,
                notifyHour = notifyHour,
                notifyMinute = notifyMinute,
                notes = notes.trim()
            )
            val updated = appData.value.specialReminders + reminder
            repository.saveData(appData.value.copy(specialReminders = updated))
            scheduleSpecialReminderNotification(getApplication(), reminder)
        }
    }

    fun updateSpecialReminder(
        id: Long,
        title: String,
        day: Int,
        month: Int,
        year: Int?,
        emoji: String,
        advanceDays: Int,
        notifyHour: Int,
        notifyMinute: Int,
        notes: String
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            cancelSpecialReminderNotification(getApplication(), id)
            val updatedList = appData.value.specialReminders.map { rem ->
                if (rem.id == id) {
                    rem.copy(
                        title = title.trim(),
                        day = day,
                        month = month,
                        year = year,
                        emoji = emoji,
                        advanceDays = advanceDays,
                        notifyHour = notifyHour,
                        notifyMinute = notifyMinute,
                        notes = notes.trim()
                    )
                } else rem
            }
            repository.saveData(appData.value.copy(specialReminders = updatedList))
            updatedList.find { it.id == id }?.let {
                scheduleSpecialReminderNotification(getApplication(), it)
            }
        }
    }

    fun deleteSpecialReminder(id: Long) {
        viewModelScope.launch {
            cancelSpecialReminderNotification(getApplication(), id)
            val updated = appData.value.specialReminders.filter { it.id != id }
            repository.saveData(appData.value.copy(specialReminders = updated))
        }
    }

    // ── Резервное копирование и экспорт (Backup & Restore) ──

    fun exportDataJson(): String {
        return appJson.encodeToString(appData.value)
    }

    fun importDataJson(json: String): Boolean {
        return try {
            val imported = appJson.decodeFromString<AppData>(json)
            viewModelScope.launch {
                repository.saveData(imported)
                imported.categories.forEach { cat ->
                    cat.tasks.forEach { task ->
                        if (!task.isDone && task.deadlineMs != null) {
                            scheduleTaskNotification(getApplication(), task.id, task.title, task.deadlineMs, task.reminderMinutesBefore)
                        }
                    }
                }
                imported.specialReminders.forEach { reminder ->
                    scheduleSpecialReminderNotification(getApplication(), reminder)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // ── Аудио / тактильная обратная связь ────────────────────

    fun playAppClickSound() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (_: Exception) {}
    }

    fun performHapticFeedback(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

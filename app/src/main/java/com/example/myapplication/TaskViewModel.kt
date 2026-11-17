package com.example.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppData
import com.example.myapplication.data.ArchivedTask
import com.example.myapplication.data.Category
import com.example.myapplication.data.SpecialReminder
import com.example.myapplication.data.Task
import com.example.myapplication.data.TaskRepository
import com.example.myapplication.notifications.cancelSpecialReminderNotification
import com.example.myapplication.notifications.cancelTaskNotification
import com.example.myapplication.notifications.scheduleSpecialReminderNotification
import com.example.myapplication.notifications.scheduleTaskNotification
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository(application)
    private val ctx = application.applicationContext

    val appData: StateFlow<AppData> = repository.appData
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppData())

    // ── Тема ──────────────────────────────────────────

    fun toggleTheme(currentlyDark: Boolean) {
        viewModelScope.launch {
            val nextMode = if (currentlyDark) "light" else "dark"
            repository.saveData(appData.value.copy(themeMode = nextMode))
        }
    }

    // ── Категории ──────────────────────────────────────

    fun addCategory(name: String, emoji: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val updated = appData.value.categories + Category(
                name = name.trim(),
                emoji = emoji
            )
            repository.saveData(appData.value.copy(categories = updated))
        }
    }

    fun updateCategory(categoryId: Long, newName: String, newEmoji: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val updated = appData.value.categories.map { cat ->
                if (cat.id == categoryId) cat.copy(name = newName.trim(), emoji = newEmoji)
                else cat
            }
            repository.saveData(appData.value.copy(categories = updated))
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            val catToDelete = appData.value.categories.find { it.id == categoryId }
            catToDelete?.tasks?.forEach { task ->
                cancelTaskNotification(ctx, task.id)
                // Отправляем задачи удаленной категории в архив
                archiveTaskInternal(task, categoryId, catToDelete.name, catToDelete.emoji, "deleted")
            }

            val updated = appData.value.categories.filter { it.id != categoryId }
            repository.saveData(appData.value.copy(categories = updated))
        }
    }

    // ── Задачи ─────────────────────────────────────────

    fun addTask(
        categoryId: Long,
        title: String,
        description: String = "",
        priority: Int = 0,
        deadlineMs: Long? = null,
        reminderMinutesBefore: Int? = 0
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = Task(
                title = title.trim(),
                description = description.trim(),
                priority = priority,
                deadlineMs = deadlineMs,
                reminderMinutesBefore = reminderMinutesBefore
            )
            val updated = appData.value.categories.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(tasks = listOf(task) + cat.tasks)
                } else cat
            }
            repository.saveData(appData.value.copy(categories = updated))

            if (deadlineMs != null && reminderMinutesBefore != null) {
                scheduleTaskNotification(
                    ctx,
                    task.id,
                    task.title,
                    deadlineMs,
                    reminderMinutesBefore
                )
            }
        }
    }

    fun updateTask(
        categoryId: Long,
        taskId: Long,
        newTitle: String,
        newDescription: String,
        newPriority: Int,
        newDeadlineMs: Long?,
        newReminderMinutes: Int?
    ) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            cancelTaskNotification(ctx, taskId)

            val updated = appData.value.categories.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(tasks = cat.tasks.map { task ->
                        if (task.id == taskId) {
                            task.copy(
                                title = newTitle.trim(),
                                description = newDescription.trim(),
                                priority = newPriority,
                                deadlineMs = newDeadlineMs,
                                reminderMinutesBefore = newReminderMinutes
                            )
                        } else task
                    })
                } else cat
            }
            repository.saveData(appData.value.copy(categories = updated))

            val currentTask = appData.value.categories.find { it.id == categoryId }?.tasks?.find { it.id == taskId }
            val isDone = currentTask?.isDone ?: false
            if (!isDone && newDeadlineMs != null && newReminderMinutes != null) {
                scheduleTaskNotification(
                    ctx,
                    taskId,
                    newTitle.trim(),
                    newDeadlineMs,
                    newReminderMinutes
                )
            }
        }
    }

    fun toggleTask(categoryId: Long, taskId: Long) {
        viewModelScope.launch {
            var archivedEntry: ArchivedTask? = null

            val updated = appData.value.categories.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(tasks = cat.tasks.map { task ->
                        if (task.id == taskId) {
                            val nextDone = !task.isDone
                            if (nextDone) {
                                cancelTaskNotification(ctx, task.id)
                                // Добавляем в архив как выполненную
                                archivedEntry = ArchivedTask(
                                    originalTaskId = task.id,
                                    title = task.title,
                                    description = task.description,
                                    categoryId = cat.id,
                                    categoryName = cat.name,
                                    categoryEmoji = cat.emoji,
                                    reason = "completed"
                                )
                            } else if (task.deadlineMs != null && task.reminderMinutesBefore != null) {
                                scheduleTaskNotification(
                                    ctx,
                                    task.id,
                                    task.title,
                                    task.deadlineMs,
                                    task.reminderMinutesBefore
                                )
                            }
                            task.copy(isDone = nextDone)
                        } else task
                    })
                } else cat
            }

            val newArchive = if (archivedEntry != null) {
                listOf(archivedEntry!!) + appData.value.archivedTasks
            } else {
                appData.value.archivedTasks
            }

            repository.saveData(appData.value.copy(categories = updated, archivedTasks = newArchive))
        }
    }

    fun toggleTaskGlobal(taskId: Long) {
        viewModelScope.launch {
            var archivedEntry: ArchivedTask? = null

            val updated = appData.value.categories.map { cat ->
                cat.copy(tasks = cat.tasks.map { task ->
                    if (task.id == taskId) {
                        val nextDone = !task.isDone
                        if (nextDone) {
                            cancelTaskNotification(ctx, task.id)
                            archivedEntry = ArchivedTask(
                                originalTaskId = task.id,
                                title = task.title,
                                description = task.description,
                                categoryId = cat.id,
                                categoryName = cat.name,
                                categoryEmoji = cat.emoji,
                                reason = "completed"
                            )
                        } else if (task.deadlineMs != null && task.reminderMinutesBefore != null) {
                            scheduleTaskNotification(
                                ctx,
                                task.id,
                                task.title,
                                task.deadlineMs,
                                task.reminderMinutesBefore
                            )
                        }
                        task.copy(isDone = nextDone)
                    } else task
                })
            }

            val newArchive = if (archivedEntry != null) {
                listOf(archivedEntry!!) + appData.value.archivedTasks
            } else {
                appData.value.archivedTasks
            }

            repository.saveData(appData.value.copy(categories = updated, archivedTasks = newArchive))
        }
    }

    fun deleteTask(categoryId: Long, taskId: Long) {
        viewModelScope.launch {
            cancelTaskNotification(ctx, taskId)
            val currentCat = appData.value.categories.find { it.id == categoryId }
            val taskToDelete = currentCat?.tasks?.find { it.id == taskId }

            val newArchive = if (taskToDelete != null) {
                val entry = ArchivedTask(
                    originalTaskId = taskToDelete.id,
                    title = taskToDelete.title,
                    description = taskToDelete.description,
                    categoryId = categoryId,
                    categoryName = currentCat.name,
                    categoryEmoji = currentCat.emoji,
                    reason = "deleted"
                )
                listOf(entry) + appData.value.archivedTasks
            } else appData.value.archivedTasks

            val updated = appData.value.categories.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(tasks = cat.tasks.filter { it.id != taskId })
                } else cat
            }
            repository.saveData(appData.value.copy(categories = updated, archivedTasks = newArchive))
        }
    }

    fun deleteTaskGlobal(taskId: Long) {
        viewModelScope.launch {
            cancelTaskNotification(ctx, taskId)
            var deletedEntry: ArchivedTask? = null

            val updated = appData.value.categories.map { cat ->
                val task = cat.tasks.find { it.id == taskId }
                if (task != null) {
                    deletedEntry = ArchivedTask(
                        originalTaskId = task.id,
                        title = task.title,
                        description = task.description,
                        categoryId = cat.id,
                        categoryName = cat.name,
                        categoryEmoji = cat.emoji,
                        reason = "deleted"
                    )
                }
                cat.copy(tasks = cat.tasks.filter { it.id != taskId })
            }

            val newArchive = if (deletedEntry != null) {
                listOf(deletedEntry!!) + appData.value.archivedTasks
            } else appData.value.archivedTasks

            repository.saveData(appData.value.copy(categories = updated, archivedTasks = newArchive))
        }
    }

    // ── Архив ──────────────────────────────────────────

    private fun archiveTaskInternal(
        task: Task,
        categoryId: Long,
        categoryName: String,
        categoryEmoji: String,
        reason: String
    ) {
        val entry = ArchivedTask(
            originalTaskId = task.id,
            title = task.title,
            description = task.description,
            categoryId = categoryId,
            categoryName = categoryName,
            categoryEmoji = categoryEmoji,
            reason = reason
        )
        viewModelScope.launch {
            val newArchive = listOf(entry) + appData.value.archivedTasks
            repository.saveData(appData.value.copy(archivedTasks = newArchive))
        }
    }

    fun restoreArchivedTask(archivedTask: ArchivedTask) {
        viewModelScope.launch {
            val restoredTask = Task(
                title = archivedTask.title,
                description = archivedTask.description,
                isDone = false
            )

            // Если исходная категория еще существует — вернем туда, иначе в первую попавшуюся или создадим
            val categories = appData.value.categories
            val targetCat = categories.find { it.id == archivedTask.categoryId }
                ?: categories.firstOrNull()

            val updatedCategories = if (targetCat != null) {
                categories.map { cat ->
                    if (cat.id == targetCat.id) {
                        cat.copy(tasks = listOf(restoredTask) + cat.tasks)
                    } else cat
                }
            } else {
                listOf(Category(name = "Восстановленные", emoji = "📥", tasks = listOf(restoredTask)))
            }

            val updatedArchive = appData.value.archivedTasks.filter { it.id != archivedTask.id }
            repository.saveData(appData.value.copy(categories = updatedCategories, archivedTasks = updatedArchive))
        }
    }

    fun deleteArchivedTask(archivedTaskId: Long) {
        viewModelScope.launch {
            val updated = appData.value.archivedTasks.filter { it.id != archivedTaskId }
            repository.saveData(appData.value.copy(archivedTasks = updated))
        }
    }

    fun clearAllArchive() {
        viewModelScope.launch {
            repository.saveData(appData.value.copy(archivedTasks = emptyList()))
        }
    }

    // ── Уведомления ────────────────────────────────────

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.saveData(appData.value.copy(notifications = emptyList()))
        }
    }

    fun markNotificationsAsRead() {
        viewModelScope.launch {
            val updated = appData.value.notifications.map { it.copy(isRead = true) }
            repository.saveData(appData.value.copy(notifications = updated))
        }
    }

    // ── Напоминания (Дни рождения, важные даты) ─────────

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
            scheduleSpecialReminderNotification(ctx, reminder)
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
            cancelSpecialReminderNotification(ctx, id)
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
                scheduleSpecialReminderNotification(ctx, it)
            }
        }
    }

    fun deleteSpecialReminder(id: Long) {
        viewModelScope.launch {
            cancelSpecialReminderNotification(ctx, id)
            val updated = appData.value.specialReminders.filter { it.id != id }
            repository.saveData(appData.value.copy(specialReminders = updated))
        }
    }
}

package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.TaskViewModel
import com.example.myapplication.data.Category
import com.example.myapplication.data.SmartListType
import com.example.myapplication.data.Task

data class TaskWithCategory(
    val task: Task,
    val category: Category
)

/** Сортировка задач-с-категорией согласно выбранному режиму. */
fun List<TaskWithCategory>.sortedByMode(mode: String): List<TaskWithCategory> = when (mode) {
    "deadline" -> sortedBy { it.task.deadlineMs ?: Long.MAX_VALUE }
    "priority" -> sortedByDescending { it.task.priority }
    "alphabetical" -> sortedBy { it.task.title.lowercase() }
    else -> this
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartListScreen(
    type: SmartListType,
    viewModel: TaskViewModel = viewModel(),
    onBack: () -> Unit
) {
    val appData by viewModel.appData.collectAsStateWithLifecycle()
    var editingTaskWithCategory by remember { mutableStateOf<TaskWithCategory?>(null) }

    var sortMenuExpanded by remember { mutableStateOf(false) }
    val sortMode = appData.taskSortMode
    val hideCompletedTasks = appData.hideCompleted

    // Собираем задачи по типу умного списка
    val allTasksWithCategory = remember(appData, type) {
        val list = mutableListOf<TaskWithCategory>()
        appData.categories.forEach { cat ->
            cat.tasks.forEach { task ->
                val matches = when (type) {
                    SmartListType.TODAY -> task.isDueToday()
                    SmartListType.SCHEDULED -> task.deadlineMs != null
                    SmartListType.OVERDUE -> task.isOverdue()
                    SmartListType.ALL -> true
                }
                if (matches) {
                    list.add(TaskWithCategory(task, cat))
                }
            }
        }
        when (type) {
            SmartListType.SCHEDULED -> list.sortedBy { it.task.deadlineMs ?: Long.MAX_VALUE }
            SmartListType.OVERDUE -> list.sortedBy { it.task.deadlineMs ?: 0L }
            else -> list
        }
    }

    val activeTasks = allTasksWithCategory.filter { !it.task.isDone }.sortedByMode(sortMode)
    val completedTasks = allTasksWithCategory.filter { it.task.isDone }.sortedByMode(sortMode)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(type.emoji, fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            type.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.3).sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Сортировка",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        if (allTasksWithCategory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(type.emoji, fontSize = 52.sp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Задач в этом разделе нет",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Все задачи выполнены или не назначены",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
            ) {
                if (activeTasks.isNotEmpty()) {
                    items(activeTasks, key = { it.task.id }) { item ->
                        SmartTaskItemCard(
                            item = item,
                            onToggle = { viewModel.toggleTaskGlobal(item.task.id) },
                            onToggleSubtask = { subId -> viewModel.toggleSubtaskGlobal(item.task.id, subId) },
                            onEdit = { editingTaskWithCategory = item },
                            onDelete = { viewModel.deleteTaskGlobal(item.task.id) }
                        )
                    }
                }

                if (!hideCompletedTasks && completedTasks.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Завершённые (${completedTasks.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    items(completedTasks, key = { it.task.id }) { item ->
                        SmartTaskItemCard(
                            item = item,
                            onToggle = { viewModel.toggleTaskGlobal(item.task.id) },
                            onToggleSubtask = { subId -> viewModel.toggleSubtaskGlobal(item.task.id, subId) },
                            onEdit = { editingTaskWithCategory = item },
                            onDelete = { viewModel.deleteTaskGlobal(item.task.id) }
                        )
                    }
                }
            }
        }
    }

    editingTaskWithCategory?.let { item ->
        TaskDialog(
            dialogTitle = "Редактировать задачу",
            initialTitle = item.task.title,
            initialDescription = item.task.description,
            initialPriority = item.task.priority,
            initialDeadlineMs = item.task.deadlineMs,
            initialReminderMinutes = item.task.reminderMinutesBefore,
            initialSubtasks = item.task.subtasks,
            initialRepeatRule = item.task.repeatRule,
            confirmButtonText = "Сохранить",
            onConfirm = { title, desc, priority, deadline, reminder, subtasks, repeat ->
                viewModel.updateTask(
                    item.category.id,
                    item.task.id,
                    title,
                    desc,
                    priority,
                    deadline,
                    reminder,
                    subtasks,
                    repeat
                )
                editingTaskWithCategory = null
            },
            onDismiss = { editingTaskWithCategory = null }
        )
    }

    if (sortMenuExpanded) {
        SortFilterSheet(
            sortMode = sortMode,
            hideCompleted = hideCompletedTasks,
            onSortModeChange = { viewModel.setTaskSortMode(it) },
            onHideCompletedChange = { viewModel.setHideCompleted(it) },
            onDismiss = { sortMenuExpanded = false }
        )
    }
}

@Composable
fun SmartTaskItemCard(
    item: TaskWithCategory,
    onToggle: () -> Unit,
    onToggleSubtask: (Long) -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        // Метка родительской категории
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.category.emoji, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    item.category.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SwipeableTaskCard(
            task = item.task,
            onToggle = onToggle,
            onToggleSubtask = onToggleSubtask,
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

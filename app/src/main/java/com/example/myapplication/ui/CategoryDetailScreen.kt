package com.example.myapplication.ui

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.TaskViewModel
import com.example.myapplication.data.Category
import com.example.myapplication.data.Subtask
import com.example.myapplication.data.Task
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val dateTimeFormatter = SimpleDateFormat("d MMM, HH:mm", Locale("ru"))
private val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale("ru"))

fun playAppClickSound() {
    try {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
    } catch (_: Exception) {}
}

fun Task.deadlineLabel(): String? =
    deadlineMs?.let { dateTimeFormatter.format(Date(it)) }

fun Task.isOverdue(): Boolean {
    val dl = deadlineMs ?: return false
    return dl < System.currentTimeMillis() && !isDone
}

fun Task.isDueToday(): Boolean {
    val dl = deadlineMs ?: return false
    val cal = Calendar.getInstance()
    val todayStart = cal.apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val todayEnd = todayStart + 86_400_000L
    return dl in todayStart until todayEnd && !isDone
}

fun reminderLabel(minutes: Int?): String = when {
    minutes == null -> "Без уведомления"
    minutes == 0 -> "Вовремя"
    minutes % 1440 == 0 -> "За ${minutes / 1440} дн."
    minutes % 60 == 0 -> "За ${minutes / 60} ч."
    else -> "За $minutes мин."
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryId: Long,
    viewModel: TaskViewModel = viewModel(),
    onBack: () -> Unit
) {
    val appData by viewModel.appData.collectAsStateWithLifecycle()
    val category = appData.categories.find { it.id == categoryId } ?: return

    CategoryDetailContent(
        category = category,
        sortMode = appData.taskSortMode,
        hideCompleted = appData.hideCompleted,
        onBack = onBack,
        onSortModeChange = { viewModel.setTaskSortMode(it) },
        onHideCompletedChange = { viewModel.setHideCompleted(it) },
        onEditCategory = { newName, newEmoji ->
            viewModel.updateCategory(categoryId, newName, newEmoji)
        },
        onAddTask = { title, desc, priority, deadline, reminder, subtasks ->
            viewModel.addTask(categoryId, title, desc, priority, deadline, reminder, subtasks)
        },
        onEditTask = { taskId, newTitle, newDesc, newPriority, newDeadline, newReminder, newSubtasks ->
            viewModel.updateTask(categoryId, taskId, newTitle, newDesc, newPriority, newDeadline, newReminder, newSubtasks)
        },
        onToggleTask = { taskId -> viewModel.toggleTask(categoryId, taskId) },
        onToggleSubtask = { taskId, subtaskId -> viewModel.toggleSubtask(categoryId, taskId, subtaskId) },
        onDeleteTask = { taskId -> viewModel.deleteTask(categoryId, taskId) },
        onUndoDelete = { viewModel.undoDelete() }
    )
}

/** Сортировка списка задач согласно выбранному режиму. */
fun List<Task>.sortedByMode(mode: String): List<Task> = when (mode) {
    "deadline" -> sortedBy { it.deadlineMs ?: Long.MAX_VALUE }
    "priority" -> sortedByDescending { it.priority }
    "alphabetical" -> sortedBy { it.title.lowercase() }
    else -> this
}

/** Человекочитаемое название режима сортировки. */
fun sortModeLabel(mode: String): String = when (mode) {
    "deadline" -> "По сроку"
    "priority" -> "По приоритету"
    "alphabetical" -> "По алфавиту"
    else -> "Вручную"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailContent(
    category: Category,
    sortMode: String = "manual",
    hideCompleted: Boolean = false,
    onBack: () -> Unit,
    onSortModeChange: (String) -> Unit = {},
    onHideCompletedChange: (Boolean) -> Unit = {},
    onEditCategory: (newName: String, newEmoji: String) -> Unit,
    onAddTask: (String, String, Int, Long?, Int?, List<Subtask>) -> Unit,
    onEditTask: (taskId: Long, String, String, Int, Long?, Int?, List<Subtask>) -> Unit,
    onToggleTask: (Long) -> Unit,
    onToggleSubtask: (Long, Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onUndoDelete: () -> Unit = {}
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var showCompleted by remember { mutableStateOf(true) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val activeTasks = remember(category.tasks, sortMode) {
        category.tasks.filter { !it.isDone }.sortedByMode(sortMode)
    }
    val completedTasks = remember(category.tasks, sortMode) {
        category.tasks.filter { it.isDone }.sortedByMode(sortMode)
    }

    // Удаление с возможностью отмены через Snackbar.
    val deleteWithUndo: (Long) -> Unit = { taskId ->
        onDeleteTask(taskId)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Задача удалена",
                actionLabel = "Отменить",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onUndoDelete()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showEditCategoryDialog = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxSize()
                            ) {}
                            Text(category.emoji, fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            category.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Изменить список",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
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
                    // Меню сортировки / фильтра
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Сортировка",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            listOf("manual", "deadline", "priority", "alphabetical").forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(sortModeLabel(mode)) },
                                    trailingIcon = {
                                        if (sortMode == mode) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSortModeChange(mode)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Скрывать выполненные") },
                                trailingIcon = {
                                    Switch(
                                        checked = hideCompleted,
                                        onCheckedChange = { onHideCompletedChange(it) }
                                    )
                                },
                                onClick = { onHideCompletedChange(!hideCompleted) }
                            )
                        }
                    }
                    // Кнопка «Поделиться списком»
                    IconButton(onClick = {
                        val shareText = buildString {
                            appendLine("${category.emoji} ${category.name}")
                            appendLine()
                            if (activeTasks.isNotEmpty()) {
                                appendLine("Активные:")
                                activeTasks.forEach { task ->
                                    val deadline = task.deadlineLabel()?.let { " ($it)" } ?: ""
                                    appendLine("☐ ${task.title}$deadline")
                                    if (task.subtasks.isNotEmpty()) {
                                        task.subtasks.forEach { sub ->
                                            val mark = if (sub.isDone) "  ✓" else "  •"
                                            appendLine("$mark ${sub.title}")
                                        }
                                    }
                                }
                            }
                            if (completedTasks.isNotEmpty()) {
                                appendLine()
                                appendLine("Завершённые:")
                                completedTasks.forEach { task ->
                                    appendLine("☑ ${task.title}")
                                }
                            }
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, category.name)
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Поделиться списком"))
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Поделиться",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Новая задача")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (category.tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨", fontSize = 48.sp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Список пуст",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Нажмите «+», чтобы добавить задачу",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val done = completedTasks.size
                val total = category.tasks.size

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { done.toFloat() / total },
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "$done из $total",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(activeTasks, key = { it.id }) { task ->
                        SwipeableTaskCard(
                            task = task,
                            onToggle = { onToggleTask(task.id) },
                            onToggleSubtask = { subId -> onToggleSubtask(task.id, subId) },
                            onEdit = { editingTask = task },
                            onDelete = { deleteWithUndo(task.id) }
                        )
                    }

                    if (!hideCompleted && completedTasks.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showCompleted = !showCompleted }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Завершённые (${completedTasks.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    if (showCompleted) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (showCompleted) {
                            items(completedTasks, key = { it.id }) { task ->
                                SwipeableTaskCard(
                                    task = task,
                                    onToggle = { onToggleTask(task.id) },
                                    onToggleSubtask = { subId -> onToggleSubtask(task.id, subId) },
                                    onEdit = { editingTask = task },
                                    onDelete = { onDeleteTask(task.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TaskDialog(
            dialogTitle = "Новая задача",
            initialTitle = "",
            initialDescription = "",
            initialPriority = 0,
            initialDeadlineMs = null,
            initialReminderMinutes = 15,
            initialSubtasks = emptyList(),
            confirmButtonText = "Добавить",
            onConfirm = { title, desc, priority, deadline, reminder, subtasks ->
                onAddTask(title, desc, priority, deadline, reminder, subtasks)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingTask?.let { task ->
        TaskDialog(
            dialogTitle = "Редактировать задачу",
            initialTitle = task.title,
            initialDescription = task.description,
            initialPriority = task.priority,
            initialDeadlineMs = task.deadlineMs,
            initialReminderMinutes = task.reminderMinutesBefore,
            initialSubtasks = task.subtasks,
            confirmButtonText = "Сохранить",
            onConfirm = { title, desc, priority, deadline, reminder, subtasks ->
                onEditTask(task.id, title, desc, priority, deadline, reminder, subtasks)
                editingTask = null
            },
            onDismiss = { editingTask = null }
        )
    }

    if (showEditCategoryDialog) {
        CategoryDialog(
            title = "Изменить список",
            initialName = category.name,
            initialEmoji = category.emoji,
            confirmText = "Сохранить",
            onConfirm = { newName, newEmoji ->
                onEditCategory(newName, newEmoji)
                showEditCategoryDialog = false
            },
            onDismiss = { showEditCategoryDialog = false }
        )
    }
}

// Карточка задачи с поддержкой свайпа влево (удаление) и вправо (завершение)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskCard(
    task: Task,
    onToggle: () -> Unit,
    onToggleSubtask: (Long) -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val soundOn = LocalSoundEnabled.current
    val hapticsOn = LocalHapticsEnabled.current
    val confirmDelete = LocalConfirmDelete.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Смахнули вправо -> завершить / возобновить
                    if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (soundOn) playAppClickSound()
                    onToggle()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Смахнули влево -> удалить
                    if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (confirmDelete) {
                        // Показываем подтверждение и не убираем карточку.
                        showDeleteConfirm = true
                        false
                    } else {
                        onDelete()
                        true
                    }
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("Удалить задачу?", fontWeight = FontWeight.Bold) },
            text = { Text("«${task.title}» будет перемещена в архив.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Удалить", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF34C759) // Apple Green
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF3B30) // Apple Red
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                label = "swipeBg"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd)
                    Alignment.CenterStart
                else
                    Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = "Завершить",
                        tint = Color.White
                    )
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        AppleTaskCard(
            task = task,
            onToggle = onToggle,
            onToggleSubtask = onToggleSubtask,
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

@Composable
fun AppleTaskCard(
    task: Task,
    onToggle: () -> Unit,
    onToggleSubtask: (Long) -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val overdue = task.isOverdue()
    val dueToday = task.isDueToday()
    val haptic = LocalHapticFeedback.current
    val soundOn = LocalSoundEnabled.current
    val hapticsOn = LocalHapticsEnabled.current

    val cardBg = when {
        task.isDone -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        overdue     -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        dueToday    -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else        -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(
            1.dp,
            if (overdue) MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppleCircularCheckbox(
                    checked = task.isDone,
                    onCheckedChange = {
                        if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (soundOn) playAppClickSound()
                        onToggle()
                    }
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEdit() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Индикатор приоритета
                        if (task.priority > 0 && !task.isDone) {
                            val prioColor = if (task.priority == 2) Color(0xFFFF3B30) else Color(0xFFFF9500)
                            val prioText = if (task.priority == 2) "!!!" else "!!"
                            Text(
                                text = prioText,
                                color = prioColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (task.isDone) FontWeight.Normal else FontWeight.Medium,
                            textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (task.isDone)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Описание / Заметка задачи (если есть)
                    if (task.description.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    task.deadlineLabel()?.let { label ->
                        Spacer(Modifier.height(3.dp))
                        val labelColor = when {
                            task.isDone -> MaterialTheme.colorScheme.onSurfaceVariant
                            overdue     -> MaterialTheme.colorScheme.error
                            dueToday    -> MaterialTheme.colorScheme.primary
                            else        -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val prefix = when {
                            task.isDone -> "Завершено: "
                            overdue     -> "Просрочено: "
                            dueToday    -> "Сегодня: "
                            else        -> ""
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$prefix$label",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (overdue) FontWeight.Bold else FontWeight.Normal,
                                color = labelColor
                            )

                            if (!task.isDone && task.reminderMinutesBefore != null) {
                                Text(
                                    text = "• 🔔 ${reminderLabel(task.reminderMinutesBefore)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // Блок чек-листа (подзадач) внутри карточки
            if (task.subtasks.isNotEmpty()) {
                val doneSub = task.subtasks.count { it.isDone }
                val totalSub = task.subtasks.size
                val subProgress = if (totalSub > 0) doneSub.toFloat() / totalSub else 0f

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 36.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { subProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (doneSub == totalSub) Color(0xFF34C759) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "$doneSub/$totalSub",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    task.subtasks.forEach { sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (soundOn) playAppClickSound()
                                    onToggleSubtask(sub.id)
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (sub.isDone) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .border(1.5.dp, if (sub.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sub.isDone) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (sub.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppleCircularCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "chkBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        label = "chkBorder"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable { onCheckedChange() },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    dialogTitle: String,
    initialTitle: String,
    initialDescription: String = "",
    initialPriority: Int = 0,
    initialDeadlineMs: Long?,
    initialReminderMinutes: Int?,
    initialSubtasks: List<Subtask> = emptyList(),
    confirmButtonText: String,
    onConfirm: (title: String, description: String, priority: Int, deadlineMs: Long?, reminderMinutes: Int?, subtasks: List<Subtask>) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var priority by remember { mutableIntStateOf(initialPriority) }
    var subtasks by remember { mutableStateOf(initialSubtasks) }
    var newSubtaskText by remember { mutableStateOf("") }
    var hasDeadline by remember { mutableStateOf(initialDeadlineMs != null) }

    val cal = remember(initialDeadlineMs) {
        Calendar.getInstance().apply {
            if (initialDeadlineMs != null) {
                timeInMillis = initialDeadlineMs
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
            }
        }
    }

    var selectedHour by remember { mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(cal.get(Calendar.MINUTE)) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(cal.timeInMillis) }
    var selectedReminder by remember { mutableStateOf<Int?>(initialReminderMinutes ?: 15) }

    // Своё время напоминания: распознаём, не совпадает ли начальное значение с пресетами
    val presetReminders = listOf(0, 15, 30, 60, 1440)
    var customReminderText by remember {
        mutableStateOf(
            if (initialReminderMinutes != null && initialReminderMinutes !in presetReminders) {
                when {
                    initialReminderMinutes % 1440 == 0 -> (initialReminderMinutes / 1440).toString()
                    initialReminderMinutes % 60 == 0 -> (initialReminderMinutes / 60).toString()
                    else -> initialReminderMinutes.toString()
                }
            } else ""
        )
    }
    var customUnit by remember {
        mutableIntStateOf(
            if (initialReminderMinutes != null && initialReminderMinutes !in presetReminders) {
                when {
                    initialReminderMinutes % 1440 == 0 -> 1440
                    initialReminderMinutes % 60 == 0 -> 60
                    else -> 1
                }
            } else 1
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis ?: System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = selectedHour,
        initialMinute = selectedMinute,
        is24Hour = true
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(24.dp),
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    hasDeadline = true
                    showDatePicker = false
                }) { Text("Выбрать", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Время дедлайна", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    hasDeadline = true
                    showTimePicker = false
                }) { Text("Готово", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(dialogTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Что нужно сделать?") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Заметка (ссылка, адрес, детали...)") },
                        minLines = 2,
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Приоритеты: 0 (Обычный), 1 (Средний !!), 2 (Высокий !!!)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Приоритет:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple(0, "Обычный", MaterialTheme.colorScheme.onSurfaceVariant),
                                Triple(1, "!!", Color(0xFFFF9500)),
                                Triple(2, "!!!", Color(0xFFFF3B30))
                            ).forEach { (level, label, color) ->
                                val isSelected = priority == level
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { priority = level },
                                    modifier = Modifier.weight(1f),
                                    label = {
                                        Text(
                                            label,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // Чек-лист (Подзадачи)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Чек-лист / Подзадачи (${subtasks.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        subtasks.forEach { sub ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = sub.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = { subtasks = subtasks.filter { it.id != sub.id } },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Удалить пункт",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newSubtaskText,
                                onValueChange = { newSubtaskText = it },
                                placeholder = { Text("Добавить пункт...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    if (newSubtaskText.isNotBlank()) {
                                        subtasks = subtasks + Subtask(title = newSubtaskText.trim())
                                        newSubtaskText = ""
                                    }
                                },
                                enabled = newSubtaskText.isNotBlank()
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Добавить",
                                    tint = if (newSubtaskText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    // Срок и напоминание
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (hasDeadline && selectedDateMillis != null)
                                    dateFormatter.format(Date(selectedDateMillis!!))
                                else
                                    "Выбрать дату",
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute),
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }

                    if (hasDeadline) {
                        Text(
                            "Напомнить",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val isCustomReminder = customReminderText.isNotBlank()
                        val reminderPresets: List<Pair<Int?, String>> = listOf(
                            0 to "Вовремя",
                            15 to "15 мин",
                            30 to "30 мин",
                            60 to "1 час",
                            1440 to "1 день",
                            null to "Выкл"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Пресеты — две ровные строки по три чипа
                            reminderPresets.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowItems.forEach { (minutes, label) ->
                                        val isSelected = !isCustomReminder && selectedReminder == minutes
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                customReminderText = ""
                                                selectedReminder = minutes
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            label = {
                                                Text(
                                                    label,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // Своё время: число + единица измерения
                            OutlinedTextField(
                                value = customReminderText,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(4)
                                    customReminderText = digits
                                    val number = digits.toIntOrNull()
                                    if (number != null && number > 0) {
                                        selectedReminder = number * customUnit
                                    }
                                },
                                placeholder = { Text("Своё время (число)", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Минуты" to 1,
                                    "Часы" to 60,
                                    "Дни" to 1440
                                ).forEach { (unitLabel, mult) ->
                                    val isSelected = isCustomReminder && customUnit == mult
                                    FilterChip(
                                        selected = isSelected,
                                        enabled = isCustomReminder,
                                        onClick = {
                                            customUnit = mult
                                            val number = customReminderText.toIntOrNull()
                                            if (number != null && number > 0) {
                                                selectedReminder = number * mult
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        label = {
                                            Text(
                                                unitLabel,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = { hasDeadline = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("✕ Без срока", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val computedDeadlineMs = if (hasDeadline && selectedDateMillis != null) {
                                val deadlineCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateMillis!!
                                    set(Calendar.HOUR_OF_DAY, selectedHour)
                                    set(Calendar.MINUTE, selectedMinute)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                deadlineCal.timeInMillis
                            } else null

                            onConfirm(
                                title,
                                description,
                                priority,
                                computedDeadlineMs,
                                if (hasDeadline) selectedReminder else null,
                                subtasks
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(confirmButtonText, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

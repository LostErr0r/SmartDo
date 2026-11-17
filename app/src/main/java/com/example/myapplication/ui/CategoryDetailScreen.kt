package com.example.myapplication.ui

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LightMode
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.TaskViewModel
import com.example.myapplication.data.Category
import com.example.myapplication.data.Task
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

private val dateTimeFormatter = SimpleDateFormat("d MMM, HH:mm", Locale("ru"))
private val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale("ru"))

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

fun reminderLabel(minutes: Int?): String = when (minutes) {
    null -> "Без уведомления"
    0 -> "Вовремя"
    15 -> "За 15 мин"
    30 -> "За 30 мин"
    60 -> "За 1 час"
    1440 -> "За 1 день"
    else -> "За $minutes мин."
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryId: Long,
    viewModel: TaskViewModel = viewModel(),
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit
) {
    val appData by viewModel.appData.collectAsStateWithLifecycle()
    val category = appData.categories.find { it.id == categoryId } ?: return

    CategoryDetailContent(
        category = category,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        onBack = onBack,
        onEditCategory = { newName, newEmoji ->
            viewModel.updateCategory(categoryId, newName, newEmoji)
        },
        onAddTask = { title, desc, priority, deadline, reminder ->
            viewModel.addTask(categoryId, title, desc, priority, deadline, reminder)
        },
        onEditTask = { taskId, newTitle, newDesc, newPriority, newDeadline, newReminder ->
            viewModel.updateTask(categoryId, taskId, newTitle, newDesc, newPriority, newDeadline, newReminder)
        },
        onToggleTask = { taskId -> viewModel.toggleTask(categoryId, taskId) },
        onDeleteTask = { taskId -> viewModel.deleteTask(categoryId, taskId) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailContent(
    category: Category,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit,
    onEditCategory: (newName: String, newEmoji: String) -> Unit,
    onAddTask: (String, String, Int, Long?, Int?) -> Unit,
    onEditTask: (taskId: Long, String, String, Int, Long?, Int?) -> Unit,
    onToggleTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var showCompleted by remember { mutableStateOf(true) }

    val activeTasks = remember(category.tasks) { category.tasks.filter { !it.isDone } }
    val completedTasks = remember(category.tasks) { category.tasks.filter { it.isDone } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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

                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Сменить тему",
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
                            onEdit = { editingTask = task },
                            onDelete = { onDeleteTask(task.id) }
                        )
                    }

                    if (completedTasks.isNotEmpty()) {
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
            confirmButtonText = "Добавить",
            onConfirm = { title, desc, priority, deadline, reminder ->
                onAddTask(title, desc, priority, deadline, reminder)
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
            confirmButtonText = "Сохранить",
            onConfirm = { title, desc, priority, deadline, reminder ->
                onEditTask(task.id, title, desc, priority, deadline, reminder)
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
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Смахнули вправо -> завершить / возобновить
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Смахнули влево -> удалить
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

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
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

@Composable
fun AppleTaskCard(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val overdue = task.isOverdue()
    val dueToday = task.isDueToday()
    val haptic = LocalHapticFeedback.current

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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppleCircularCheckbox(
                checked = task.isDone,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
    confirmButtonText: String,
    onConfirm: (title: String, description: String, priority: Int, deadlineMs: Long?, reminderMinutes: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var priority by remember { mutableIntStateOf(initialPriority) }
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Приоритеты: 0 (Обычный), 1 (Средний !!), 2 (Высокий !!!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Приоритет:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        listOf(
                            0 to "Обычный",
                            1 to "!! Средний",
                            2 to "!!! Высокий"
                        ).forEach { (prio, label) ->
                            FilterChip(
                                selected = priority == prio,
                                onClick = { priority = prio },
                                shape = RoundedCornerShape(8.dp),
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    Text(
                        "Срок выполнения",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (hasDeadline && selectedDateMillis != null)
                                    dateFormatter.format(Date(selectedDateMillis!!))
                                else
                                    "Выбрать дату",
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute),
                                fontSize = 13.sp
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

                        val chipsRow1 = listOf(0, 15, 30)
                        val chipsRow2 = listOf(60, 1440, null)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                chipsRow1.forEach { minutes ->
                                    val isSelected = selectedReminder == minutes
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedReminder = minutes },
                                        shape = RoundedCornerShape(10.dp),
                                        label = { Text(reminderLabel(minutes), fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                chipsRow2.forEach { minutes ->
                                    val isSelected = selectedReminder == minutes
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedReminder = minutes },
                                        shape = RoundedCornerShape(10.dp),
                                        label = { Text(reminderLabel(minutes), fontSize = 12.sp) },
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
                                if (hasDeadline) selectedReminder else null
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

// ── Превью ────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun CategoryDetailPreview() {
    MyApplicationTheme {
        val now = System.currentTimeMillis()
        CategoryDetailContent(
            category = Category(
                id = 1, name = "Покупки", emoji = "🛒",
                tasks = listOf(
                    Task(id = 1, title = "Молоко", description = "2.5% жирности", priority = 2, isDone = true, deadlineMs = now, reminderMinutesBefore = 0),
                    Task(id = 2, title = "Хлеб", priority = 1, isDone = false, deadlineMs = now - 86_400_000, reminderMinutesBefore = 15),
                    Task(id = 3, title = "Яблоки", isDone = false, deadlineMs = now + 2 * 86_400_000, reminderMinutesBefore = 60),
                    Task(id = 4, title = "Без дедлайна", isDone = false)
                )
            ),
            isDarkTheme = false,
            onToggleTheme = {},
            onBack = {},
            onEditCategory = { _, _ -> },
            onAddTask = { _, _, _, _, _ -> },
            onEditTask = { _, _, _, _, _, _ -> },
            onToggleTask = {},
            onDeleteTask = {}
        )
    }
}

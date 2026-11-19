package com.example.myapplication.ui

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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.TaskViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

private val monthNames = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)
private val weekDays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

private fun startOfDay(millis: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = millis
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private fun dayStart(year: Int, month: Int, day: Int): Long {
    val c = Calendar.getInstance()
    c.set(year, month, day, 0, 0, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val appData by viewModel.appData.collectAsStateWithLifecycle()
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDay by remember { mutableLongStateOf(startOfDay(System.currentTimeMillis())) }
    var editingTaskWithCategory by remember { mutableStateOf<TaskWithCategory?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val todayStart = remember { startOfDay(System.currentTimeMillis()) }

    val tasksByDay = remember(appData) {
        val map = mutableMapOf<Long, MutableList<TaskWithCategory>>()
        appData.categories.forEach { cat ->
            cat.tasks.forEach { task ->
                task.deadlineMs?.let { dm ->
                    map.getOrPut(startOfDay(dm)) { mutableListOf() }.add(TaskWithCategory(task, cat))
                }
            }
        }
        map
    }

    // Якорь отображаемого месяца
    val anchor = remember(monthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val year = anchor.get(Calendar.YEAR)
    val month = anchor.get(Calendar.MONTH)
    val daysInMonth = anchor.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDow = (anchor.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Пн = 0 … Вс = 6

    val cells: List<Int?> = buildList {
        repeat(firstDow) { add(null) }
        for (d in 1..daysInMonth) add(d)
    }

    val selectedTasks = tasksByDay[selectedDay].orEmpty().sortedBy { it.task.deadlineMs ?: 0L }

    val deleteWithUndo: (Long) -> Unit = { taskId ->
        viewModel.deleteTaskGlobal(taskId)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Задача удалена",
                actionLabel = "Отменить",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Календарь",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.3).sp
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Шапка месяца с навигацией
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${monthNames[month]} $year",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { monthOffset-- }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущий месяц")
                }
                if (monthOffset != 0) {
                    TextButton(onClick = { monthOffset = 0 }) { Text("Сегодня") }
                }
                IconButton(onClick = { monthOffset++ }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Следующий месяц")
                }
            }

            // Заголовки дней недели
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { d ->
                    Text(
                        d,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Сетка дней (по 7 в строке)
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (day != null) {
                                val dayMillis = dayStart(year, month, day)
                                val isSelected = dayMillis == selectedDay
                                val isToday = dayMillis == todayStart
                                val hasTasks = tasksByDay.containsKey(dayMillis)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .size(width = 44.dp, height = 46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .then(
                                            if (isSelected) {
                                                Modifier.background(MaterialTheme.colorScheme.primary)
                                            } else if (isToday) {
                                                Modifier.border(
                                                    1.5.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(12.dp)
                                                )
                                            } else Modifier
                                        )
                                        .clickable { selectedDay = dayMillis },
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        day.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 3.dp)
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (hasTasks) {
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.primary
                                                } else Color.Transparent
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            // Список задач выбранного дня
            if (selectedTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗓️", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "На этот день задач нет",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 30.dp)
                ) {
                    items(selectedTasks, key = { it.task.id }) { item ->
                        SmartTaskItemCard(
                            item = item,
                            onToggle = { viewModel.toggleTaskGlobal(item.task.id) },
                            onToggleSubtask = { subId -> viewModel.toggleSubtaskGlobal(item.task.id, subId) },
                            onEdit = { editingTaskWithCategory = item },
                            onDelete = { deleteWithUndo(item.task.id) }
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
                    item.category.id, item.task.id, title, desc, priority, deadline, reminder, subtasks, repeat
                )
                editingTaskWithCategory = null
            },
            onDismiss = { editingTaskWithCategory = null }
        )
    }
}

package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.R
import com.example.myapplication.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val appData by viewModel.appData.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var editingTaskWithCategory by remember { mutableStateOf<TaskWithCategory?>(null) }

    val results = remember(appData, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            emptyList()
        } else {
            val list = mutableListOf<TaskWithCategory>()
            appData.categories.forEach { cat ->
                cat.tasks.forEach { task ->
                    val hit = task.title.lowercase().contains(q) ||
                        task.description.lowercase().contains(q) ||
                        cat.name.lowercase().contains(q) ||
                        task.subtasks.any { it.title.lowercase().contains(q) }
                    if (hit) list.add(TaskWithCategory(task, cat))
                }
            }
            list.sortedByMode(appData.taskSortMode)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.search_tasks_hint)) },
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
        when {
            query.isBlank() -> {
                SearchPlaceholder(
                    emoji = "🔍",
                    title = stringResource(R.string.search_placeholder_title),
                    subtitle = stringResource(R.string.search_placeholder_sub),
                    padding = padding
                )
            }
            results.isEmpty() -> {
                SearchPlaceholder(
                    emoji = "🤷",
                    title = stringResource(R.string.nothing_found),
                    subtitle = stringResource(R.string.search_empty_sub),
                    padding = padding
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
                ) {
                    item {
                        Text(
                            stringResource(R.string.search_found, results.size),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(results, key = { it.task.id }) { item ->
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
            dialogTitle = stringResource(R.string.task_edit),
            initialTitle = item.task.title,
            initialDescription = item.task.description,
            initialPriority = item.task.priority,
            initialDeadlineMs = item.task.deadlineMs,
            initialReminderMinutes = item.task.reminderMinutesBefore,
            initialSubtasks = item.task.subtasks,
            initialRepeatRule = item.task.repeatRule,
            confirmButtonText = stringResource(R.string.save),
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

@Composable
private fun SearchPlaceholder(
    emoji: String,
    title: String,
    subtitle: String,
    padding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 52.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

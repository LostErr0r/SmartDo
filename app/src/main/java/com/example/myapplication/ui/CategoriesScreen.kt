package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.TaskViewModel
import com.example.myapplication.data.AppData
import com.example.myapplication.data.Category
import com.example.myapplication.data.SmartListType
import com.example.myapplication.ui.theme.MyApplicationTheme

val CATEGORY_EMOJIS = listOf(
    "📁", "🛒", "📋", "💼", "🏠", "🎯", "📚", "💪",
    "🎮", "✈️", "🍕", "💡", "🔧", "🎨", "🏃", "💰"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: TaskViewModel = viewModel(),
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onRemindersClick: () -> Unit = {},
    onSmartListClick: (SmartListType) -> Unit = {},
    onCategoryClick: (Long) -> Unit
) {
    val appData by viewModel.appData.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAboutDialog by remember { mutableStateOf(false) }

    CategoriesContent(
        appData = appData,
        isDarkTheme = isDarkTheme,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        onToggleTheme = onToggleTheme,
        onNotificationsClick = onNotificationsClick,
        onArchiveClick = onArchiveClick,
        onRemindersClick = onRemindersClick,
        onAboutClick = { showAboutDialog = true },
        onSmartListClick = onSmartListClick,
        onCategoryClick = onCategoryClick,
        onEditCategory = { category -> editingCategory = category },
        onDeleteCategory = { viewModel.deleteCategory(it) },
        onAddClick = { showAddDialog = true }
    )

    if (showAddDialog) {
        CategoryDialog(
            title = "Новый список",
            initialName = "",
            initialEmoji = "📁",
            confirmText = "Создать",
            onConfirm = { name, emoji ->
                viewModel.addCategory(name, emoji)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingCategory?.let { cat ->
        CategoryDialog(
            title = "Изменить список",
            initialName = cat.name,
            initialEmoji = cat.emoji,
            confirmText = "Сохранить",
            onConfirm = { name, emoji ->
                viewModel.updateCategory(cat.id, name, emoji)
                editingCategory = null
            },
            onDismiss = { editingCategory = null }
        )
    }

    if (showAboutDialog) {
        AboutAppDialog(onDismiss = { showAboutDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesContent(
    appData: AppData,
    isDarkTheme: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onRemindersClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onSmartListClick: (SmartListType) -> Unit = {},
    onCategoryClick: (Long) -> Unit,
    onEditCategory: (Category) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onAddClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val todayCount = remember(appData) {
        appData.categories.sumOf { cat -> cat.tasks.count { it.isDueToday() && !it.isDone } }
    }
    val scheduledCount = remember(appData) {
        appData.categories.sumOf { cat -> cat.tasks.count { it.deadlineMs != null && !it.isDone } }
    }
    val overdueCount = remember(appData) {
        appData.categories.sumOf { cat -> cat.tasks.count { it.isOverdue() && !it.isDone } }
    }
    val allActiveCount = remember(appData) {
        appData.categories.sumOf { cat -> cat.tasks.count { !it.isDone } }
    }

    val unreadNotifsCount = remember(appData.notifications) {
        appData.notifications.count { !it.isRead }
    }

    val filteredCategories = remember(appData.categories, searchQuery) {
        if (searchQuery.isBlank()) {
            appData.categories
        } else {
            val q = searchQuery.trim().lowercase()
            appData.categories.filter { cat ->
                cat.name.lowercase().contains(q) ||
                        cat.tasks.any { it.title.lowercase().contains(q) || it.description.lowercase().contains(q) }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { menuExpanded = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Списки",
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Меню",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Выпадающее меню Apple (без текущего раздела «Списки»)
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Напоминания", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Cake, contentDescription = null, tint = Color(0xFFFF2D55))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onRemindersClick()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Архив", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Archive, contentDescription = null, tint = Color(0xFFFF9500))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onArchiveClick()
                                }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            DropdownMenuItem(
                                text = { Text("О приложении") },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onAboutClick()
                                }
                            )
                        }
                    }
                },
                actions = {
                    // Иконка уведомлений (с бейджем)
                    IconButton(onClick = onNotificationsClick) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifsCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("$unreadNotifsCount", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (unreadNotifsCount > 0) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                                contentDescription = "Уведомления",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    // Переключатель темы
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Сменить тему",
                            tint = if (isDarkTheme) Color(0xFFFFD60A) else MaterialTheme.colorScheme.onBackground
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
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Добавить список",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
        ) {
            // Поисковая строка Apple Search
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Поиск задач и списков...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Очистить",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Смарт-папки Apple Reminders
            if (searchQuery.isBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppleSmartCard(
                                title = "Сегодня",
                                emoji = "📅",
                                count = todayCount,
                                accentColor = Color(0xFF007AFF),
                                modifier = Modifier.weight(1f),
                                onClick = { onSmartListClick(SmartListType.TODAY) }
                            )

                            AppleSmartCard(
                                title = "Запланировано",
                                emoji = "⏰",
                                count = scheduledCount,
                                accentColor = Color(0xFFFF9500),
                                modifier = Modifier.weight(1f),
                                onClick = { onSmartListClick(SmartListType.SCHEDULED) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppleSmartCard(
                                title = "Просрочено",
                                emoji = "🔴",
                                count = overdueCount,
                                accentColor = Color(0xFFFF3B30),
                                modifier = Modifier.weight(1f),
                                onClick = { onSmartListClick(SmartListType.OVERDUE) }
                            )

                            AppleSmartCard(
                                title = "Все задачи",
                                emoji = "📦",
                                count = allActiveCount,
                                accentColor = Color(0xFF5856D6),
                                modifier = Modifier.weight(1f),
                                onClick = { onSmartListClick(SmartListType.ALL) }
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Мои категории",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (filteredCategories.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 44.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                if (searchQuery.isNotBlank()) "Ничего не найдено" else "Нет списков",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (searchQuery.isNotBlank()) "Попробуйте изменить запрос" else "Нажмите «+», чтобы создать категорию",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredCategories, key = { it.id }) { category ->
                    CategoryCard(
                        category = category,
                        onClick = { onCategoryClick(category.id) },
                        onEdit = { onEditCategory(category) },
                        onDelete = { onDeleteCategory(category.id) }
                    )
                }
            }
        }
    }
}

// Карточка смарт-папки Apple Reminders
@Composable
fun AppleSmartCard(
    title: String,
    emoji: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxSize()
                    ) {}
                    Text(emoji, fontSize = 18.sp)
                }

                Text(
                    text = "$count",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategoryCard(
    category: Category,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val doneCount = category.tasks.count { it.isDone }
    val totalCount = category.tasks.size
    val overdueCount = category.tasks.count { it.isOverdue() }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Text(category.emoji, fontSize = 24.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (overdueCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text(
                                "! $overdueCount",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                if (totalCount > 0) {
                    Text(
                        text = "$doneCount из $totalCount выполнено",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { doneCount.toFloat() / totalCount },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    Text(
                        text = "Задач нет",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Изменить список",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Удалить список",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDialog(
    title: String,
    initialName: String,
    initialEmoji: String,
    confirmText: String,
    onConfirm: (name: String, emoji: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedEmoji by remember { mutableStateOf(initialEmoji) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название списка") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Иконка",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                val rows = CATEGORY_EMOJIS.chunked(8)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { emoji ->
                                val isSelected = emoji == selectedEmoji
                                OutlinedButton(
                                    onClick = { selectedEmoji = emoji },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else
                                            MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Text(emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedEmoji) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(confirmText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

// ── Превью ────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun CategoriesPreview() {
    MyApplicationTheme {
        CategoriesContent(
            appData = AppData(
                categories = listOf(
                    Category(id = 1, name = "Покупки", emoji = "🛒",
                        tasks = listOf(
                            com.example.myapplication.data.Task(title = "Молоко", isDone = true),
                            com.example.myapplication.data.Task(title = "Хлеб", isDone = false)
                        )
                    ),
                    Category(id = 2, name = "Задачи", emoji = "📋")
                )
            ),
            isDarkTheme = false,
            searchQuery = "",
            onSearchQueryChange = {},
            onToggleTheme = {},
            onNotificationsClick = {},
            onArchiveClick = {},
            onRemindersClick = {},
            onAboutClick = {},
            onSmartListClick = {},
            onCategoryClick = {},
            onEditCategory = {},
            onDeleteCategory = {},
            onAddClick = {}
        )
    }
}

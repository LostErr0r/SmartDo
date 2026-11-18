package com.example.myapplication.ui

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
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
    onNotificationsClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onRemindersClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSmartListClick: (SmartListType) -> Unit = {},
    onCategoryClick: (Long) -> Unit
) {
    val appData by viewModel.appData.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    CategoriesContent(
        appData = appData,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        onNotificationsClick = onNotificationsClick,
        onArchiveClick = onArchiveClick,
        onRemindersClick = onRemindersClick,
        onStatsClick = onStatsClick,
        onSettingsClick = onSettingsClick,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesContent(
    appData: AppData,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onRemindersClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
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
                                text = { Text("Статистика", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF34C759))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onStatsClick()
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
                                text = { Text("Настройки") },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSettingsClick()
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
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Новый список")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            // Поисковая строка Apple Search
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Поиск списков и задач...") },
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

            // Блок смарт-списков 2x2 (Только при отсутствии поиска)
            if (searchQuery.isBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SmartListCard(
                                modifier = Modifier.weight(1f),
                                type = SmartListType.TODAY,
                                count = todayCount,
                                accentColor = Color(0xFF007AFF),
                                onClick = { onSmartListClick(SmartListType.TODAY) }
                            )
                            SmartListCard(
                                modifier = Modifier.weight(1f),
                                type = SmartListType.SCHEDULED,
                                count = scheduledCount,
                                accentColor = Color(0xFFFF3B30),
                                onClick = { onSmartListClick(SmartListType.SCHEDULED) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SmartListCard(
                                modifier = Modifier.weight(1f),
                                type = SmartListType.ALL,
                                count = allActiveCount,
                                accentColor = Color(0xFF8E8E93),
                                onClick = { onSmartListClick(SmartListType.ALL) }
                            )
                            SmartListCard(
                                modifier = Modifier.weight(1f),
                                type = SmartListType.OVERDUE,
                                count = overdueCount,
                                accentColor = Color(0xFFFF9500),
                                onClick = { onSmartListClick(SmartListType.OVERDUE) }
                            )
                        }
                    }
                }
            }

            // Заголовок раздела «Мои списки»
            item {
                Text(
                    text = "МОИ СПИСКИ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
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
                            Text("📁", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (searchQuery.isNotBlank()) "Ничего не найдено" else "Нет списков",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (searchQuery.isNotBlank()) "Попробуйте изменить поисковый запрос" else "Создайте первый список кнопкой «+»",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredCategories, key = { it.id }) { category ->
                    CategoryItemCard(
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

@Composable
fun SmartListCard(
    modifier: Modifier = Modifier,
    type: SmartListType,
    count: Int,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(type.emoji, fontSize = 20.sp)
                }

                Text(
                    text = "$count",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = type.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategoryItemCard(
    category: Category,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val activeTasks = category.tasks.count { !it.isDone }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(category.emoji, fontSize = 24.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (activeTasks > 0) "$activeTasks задач(и)" else "Нет задач",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Редактировать список",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Удалить список",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
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
            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название списка") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Выберите иконку:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Сетка эмодзи 4x4
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CATEGORY_EMOJIS.chunked(4).forEach { rowEmojis ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowEmojis.forEach { emoji ->
                                val isSelected = emoji == selectedEmoji
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedEmoji = emoji },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), selectedEmoji)
                    }
                },
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

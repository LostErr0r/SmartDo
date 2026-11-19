package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.TaskViewModel
import com.example.myapplication.data.SpecialReminder
import java.util.Calendar
import java.util.TimeZone

val REMINDER_EMOJIS = listOf("🎂", "🎁", "🎉", "💍", "🎈", "🌟", "❤️", "📅", "🌸", "🍰")

/** Знак зодиака: индекс в массиве zodiac_signs (Овен=0 … Рыбы=11) и символ. */
data class ZodiacSign(val index: Int, val symbol: String)

fun getZodiacSign(day: Int, month: Int): ZodiacSign {
    return when (month) {
        1 -> if (day < 20) ZodiacSign(9, "♑") else ZodiacSign(10, "♒")
        2 -> if (day < 19) ZodiacSign(10, "♒") else ZodiacSign(11, "♓")
        3 -> if (day < 21) ZodiacSign(11, "♓") else ZodiacSign(0, "♈")
        4 -> if (day < 20) ZodiacSign(0, "♈") else ZodiacSign(1, "♉")
        5 -> if (day < 21) ZodiacSign(1, "♉") else ZodiacSign(2, "♊")
        6 -> if (day < 21) ZodiacSign(2, "♊") else ZodiacSign(3, "♋")
        7 -> if (day < 23) ZodiacSign(3, "♋") else ZodiacSign(4, "♌")
        8 -> if (day < 23) ZodiacSign(4, "♌") else ZodiacSign(5, "♍")
        9 -> if (day < 23) ZodiacSign(5, "♍") else ZodiacSign(6, "♎")
        10 -> if (day < 23) ZodiacSign(6, "♎") else ZodiacSign(7, "♏")
        11 -> if (day < 22) ZodiacSign(7, "♏") else ZodiacSign(8, "♐")
        12 -> if (day < 22) ZodiacSign(8, "♐") else ZodiacSign(9, "♑")
        else -> ZodiacSign(0, "♈")
    }
}

enum class ReminderFilter(val emoji: String) {
    ALL("✨"),
    UPCOMING("⭐"),
    BIRTHDAY("🎂"),
    ANNIVERSARY("💍"),
    CELEBRATION("🎉")
}

/** Локализованная подпись фильтра напоминаний. */
@Composable
fun ReminderFilter.localizedLabel(): String = when (this) {
    ReminderFilter.ALL -> stringResource(R.string.filter_all)
    ReminderFilter.UPCOMING -> stringResource(R.string.filter_upcoming)
    ReminderFilter.BIRTHDAY -> stringResource(R.string.filter_birthday)
    ReminderFilter.ANNIVERSARY -> stringResource(R.string.filter_anniversary)
    ReminderFilter.CELEBRATION -> stringResource(R.string.filter_celebration)
}

fun SpecialReminder.matchesFilter(filter: ReminderFilter): Boolean {
    return when (filter) {
        ReminderFilter.ALL -> true
        ReminderFilter.UPCOMING -> getDaysUntilEvent(day, month) <= 30
        ReminderFilter.BIRTHDAY -> emoji in listOf("🎂", "🍰", "🌸") ||
                title.lowercase().contains("рожд") ||
                title.lowercase().contains("др") ||
                title.lowercase().contains("birth") ||
                title.lowercase().contains("bday") ||
                year != null
        ReminderFilter.ANNIVERSARY -> emoji in listOf("💍", "❤️") ||
                title.lowercase().contains("годовщин") ||
                title.lowercase().contains("свадьб") ||
                title.lowercase().contains("anniversary") ||
                title.lowercase().contains("wedding")
        ReminderFilter.CELEBRATION -> emoji in listOf("🎉", "🎁", "🎈", "🌟", "📅") ||
                title.lowercase().contains("новый год") ||
                title.lowercase().contains("праздник") ||
                title.lowercase().contains("new year") ||
                title.lowercase().contains("holiday") ||
                title.lowercase().contains("party")
    }
}

fun getDaysUntilEvent(day: Int, month: Int): Int {
    val now = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val thisYear = now.get(Calendar.YEAR)
    val eventCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, thisYear)
        set(Calendar.MONTH, month - 1)
        val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
        set(Calendar.DAY_OF_MONTH, day.coerceAtMost(maxDay))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (eventCal.before(now)) {
        eventCal.set(Calendar.YEAR, thisYear + 1)
    }
    val diffMs = eventCal.timeInMillis - now.timeInMillis
    return (diffMs / (1000 * 60 * 60 * 24)).toInt()
}

fun calculateAge(year: Int?, day: Int, month: Int): Int? {
    if (year == null || year <= 0) return null
    val now = Calendar.getInstance()
    val thisYear = now.get(Calendar.YEAR)
    val eventCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, thisYear)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
    }
    val targetYear = if (eventCal.before(now)) thisYear + 1 else thisYear
    return targetYear - year
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: TaskViewModel = viewModel(),
    onNotificationsClick: () -> Unit = {},
    onListsClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val appData by viewModel.appData.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ReminderFilter.ALL) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<SpecialReminder?>(null) }
    var deletingReminder by remember { mutableStateOf<SpecialReminder?>(null) }

    val unreadNotifsCount = remember(appData.notifications) {
        appData.notifications.count { !it.isRead }
    }

    val monthNames = stringArrayResource(R.array.month_names_genitive)
    val zodiacNames = stringArrayResource(R.array.zodiac_signs)

    val sortedReminders = remember(appData.specialReminders) {
        appData.specialReminders.sortedBy { getDaysUntilEvent(it.day, it.month) }
    }

    val filterCounts = remember(sortedReminders) {
        ReminderFilter.entries.associateWith { filter ->
            sortedReminders.count { it.matchesFilter(filter) }
        }
    }

    val filteredReminders = remember(sortedReminders, searchQuery, selectedFilter, monthNames, zodiacNames) {
        sortedReminders.filter { reminder ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                val zodiac = getZodiacSign(reminder.day, reminder.month)
                reminder.title.lowercase().contains(q) ||
                        reminder.notes.lowercase().contains(q) ||
                        monthNames.getOrElse(reminder.month - 1) { "" }.contains(q) ||
                        zodiacNames.getOrElse(zodiac.index) { "" }.lowercase().contains(q)
            }
            val matchesCategory = reminder.matchesFilter(selectedFilter)
            matchesSearch && matchesCategory
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
                                stringResource(R.string.nav_reminders),
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(R.string.menu),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Выпадающее меню Apple (без текущего раздела «Напоминания»)
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.nav_lists), fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onListsClick()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.nav_archive), fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Archive, contentDescription = null, tint = Color(0xFFFF9500))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onArchiveClick()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.nav_stats), fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF34C759))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onStatsClick()
                                }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_title)) },
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
                                contentDescription = stringResource(R.string.notifications),
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
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.reminder_add_date),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
        ) {
            // Поисковая строка Apple Search
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.reminders_search_hint)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.clear),
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

            // Быстрые фильтры Apple Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(ReminderFilter.entries) { filter ->
                        val isSelected = selectedFilter == filter
                        val count = filterCounts[filter] ?: 0
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    "${filter.emoji} ${filter.localizedLabel()} ($count)",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            if (filteredReminders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎂", fontSize = 38.sp)
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                if (searchQuery.isNotBlank() || selectedFilter != ReminderFilter.ALL) stringResource(R.string.nothing_found) else stringResource(R.string.reminders_empty_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                if (searchQuery.isNotBlank() || selectedFilter != ReminderFilter.ALL) stringResource(R.string.reminders_empty_filter_sub)
                                else stringResource(R.string.reminders_empty_sub),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )

                            if (searchQuery.isBlank() && selectedFilter == ReminderFilter.ALL) {
                                Spacer(Modifier.height(20.dp))
                                Button(
                                    onClick = { showAddDialog = true },
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.reminders_add_btn), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        stringResource(R.string.reminders_found, filteredReminders.size),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                items(filteredReminders, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onEdit = { editingReminder = reminder },
                        onDelete = { deletingReminder = reminder }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        SpecialReminderDialog(
            title = stringResource(R.string.reminder_new),
            initialReminder = null,
            confirmText = stringResource(R.string.create),
            onConfirm = { title, day, month, year, emoji, advanceDays, hour, minute, notes ->
                viewModel.addSpecialReminder(
                    title = title,
                    day = day,
                    month = month,
                    year = year,
                    emoji = emoji,
                    advanceDays = advanceDays,
                    notifyHour = hour,
                    notifyMinute = minute,
                    notes = notes
                )
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingReminder?.let { rem ->
        SpecialReminderDialog(
            title = stringResource(R.string.reminder_edit),
            initialReminder = rem,
            confirmText = stringResource(R.string.save),
            onConfirm = { title, day, month, year, emoji, advanceDays, hour, minute, notes ->
                viewModel.updateSpecialReminder(
                    id = rem.id,
                    title = title,
                    day = day,
                    month = month,
                    year = year,
                    emoji = emoji,
                    advanceDays = advanceDays,
                    notifyHour = hour,
                    notifyMinute = minute,
                    notes = notes
                )
                editingReminder = null
            },
            onDismiss = { editingReminder = null }
        )
    }

    deletingReminder?.let { rem ->
        AlertDialog(
            onDismissRequest = { deletingReminder = null },
            shape = RoundedCornerShape(22.dp),
            title = { Text(stringResource(R.string.reminder_delete_q)) },
            text = { Text(stringResource(R.string.reminder_delete_msg, rem.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSpecialReminder(rem.id)
                        deletingReminder = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingReminder = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ReminderCard(
    reminder: SpecialReminder,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val daysUntil = getDaysUntilEvent(reminder.day, reminder.month)
    val monthName = stringArrayResource(R.array.month_names_genitive).getOrElse(reminder.month - 1) { "" }
    val age = calculateAge(reminder.year, reminder.day, reminder.month)
    val zodiac = getZodiacSign(reminder.day, reminder.month)
    val zodiacName = stringArrayResource(R.array.zodiac_signs).getOrElse(zodiac.index) { "" }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Эмодзи значок
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(reminder.emoji, fontSize = 24.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reminder.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                // Дата, возраст и знак зодиака
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dateInfo = buildString {
                        append("${reminder.day} $monthName")
                        if (age != null) {
                            append(" • $age")
                        }
                    }
                    Text(
                        dateInfo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    // Бейдж знака зодиака
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "${zodiac.symbol} $zodiacName",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Информация о напоминании
                val advanceInfo = when (reminder.advanceDays) {
                    0 -> stringResource(R.string.advance_same_day)
                    1 -> stringResource(R.string.advance_1d)
                    3 -> stringResource(R.string.advance_3d)
                    7 -> stringResource(R.string.advance_week)
                    else -> stringResource(R.string.advance_days_fmt, reminder.advanceDays)
                }
                val timeStr = "%02d:%02d".format(reminder.notifyHour, reminder.notifyMinute)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.advance_in, advanceInfo, timeStr),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }

                if (reminder.notes.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        reminder.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Бейдж оставшихся дней
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val badgeColor = when {
                    daysUntil == 0 -> Color(0xFF34C759) // Apple Green
                    daysUntil == 1 -> Color(0xFFFF9500) // Apple Orange
                    daysUntil <= 7 -> Color(0xFF007AFF) // Apple Blue
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val badgeTextColor = when {
                    daysUntil <= 7 -> Color.White
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val badgeText = when {
                    daysUntil == 0 -> stringResource(R.string.badge_today)
                    daysUntil == 1 -> stringResource(R.string.badge_tomorrow)
                    daysUntil < 30 -> stringResource(R.string.badge_in_days, daysUntil)
                    else -> stringResource(R.string.badge_in_months, daysUntil / 30)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        badgeText,
                        color = badgeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialReminderDialog(
    title: String,
    initialReminder: SpecialReminder?,
    confirmText: String,
    onConfirm: (
        title: String,
        day: Int,
        month: Int,
        year: Int?,
        emoji: String,
        advanceDays: Int,
        notifyHour: Int,
        notifyMinute: Int,
        notes: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialReminder?.title ?: "") }
    var selectedEmoji by remember { mutableStateOf(initialReminder?.emoji ?: "🎂") }
    var selectedDay by remember { mutableIntStateOf(initialReminder?.day ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
    var selectedMonth by remember { mutableIntStateOf(initialReminder?.month ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) }
    var birthYearText by remember { mutableStateOf(initialReminder?.year?.toString() ?: "") }
    var advanceDays by remember { mutableIntStateOf(initialReminder?.advanceDays ?: 1) }
    var notifyHour by remember { mutableIntStateOf(initialReminder?.notifyHour ?: 9) }
    var notifyMinute by remember { mutableIntStateOf(initialReminder?.notifyMinute ?: 0) }
    var notes by remember { mutableStateOf(initialReminder?.notes ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val initialCal = Calendar.getInstance().apply {
            set(Calendar.MONTH, selectedMonth - 1)
            set(Calendar.DAY_OF_MONTH, selectedDay)
            birthYearText.toIntOrNull()?.let { set(Calendar.YEAR, it) }
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialCal.timeInMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = millis
                        }
                        selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                        selectedMonth = cal.get(Calendar.MONTH) + 1
                        val pickedYear = cal.get(Calendar.YEAR)
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        if (pickedYear < currentYear) {
                            birthYearText = pickedYear.toString()
                        }
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.choose), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = notifyHour,
            initialMinute = notifyMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.reminder_time_title)) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        notifyHour = timePickerState.hour
                        notifyMinute = timePickerState.minute
                        showTimePicker = false
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.choose))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.3).sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Имя / событие
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.reminder_name_label)) },
                        placeholder = { Text(stringResource(R.string.reminder_name_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Выбор эмодзи
                    Column {
                        Text(
                            stringResource(R.string.reminder_icon_label),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(REMINDER_EMOJIS) { emoji ->
                                val isSelected = emoji == selectedEmoji
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedEmoji = emoji },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }

                    // Выбор даты и года
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            val monthStr = stringArrayResource(R.array.month_names_genitive).getOrElse(selectedMonth - 1) { "" }
                            Text("$selectedDay $monthStr", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        OutlinedTextField(
                            value = birthYearText,
                            onValueChange = {
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    birthYearText = it
                                }
                            },
                            label = { Text(stringResource(R.string.year_label)) },
                            placeholder = { Text("1998") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // За сколько напомнить
                    Column {
                        Text(
                            stringResource(R.string.advance_label),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                0 to stringResource(R.string.advance_chip_same_day),
                                1 to stringResource(R.string.advance_1d),
                                3 to stringResource(R.string.advance_3d),
                                7 to stringResource(R.string.advance_week)
                            ).forEach { (days, label) ->
                                val isSelected = advanceDays == days
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { advanceDays = days },
                                    label = { Text(label, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // Время напоминания
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.notify_time_label),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("%02d:%02d".format(notifyHour, notifyMinute), fontWeight = FontWeight.Bold)
                        }
                    }

                    // Заметки (идеи подарка)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.reminder_notes_label)) },
                        placeholder = { Text(stringResource(R.string.reminder_notes_hint)) },
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val year = birthYearText.toIntOrNull()
                        onConfirm(
                            name,
                            selectedDay,
                            selectedMonth,
                            year,
                            selectedEmoji,
                            advanceDays,
                            notifyHour,
                            notifyMinute,
                            notes
                        )
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(confirmText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.CompositionLocalProvider
import com.example.myapplication.data.SmartListType
import com.example.myapplication.notifications.createNotificationChannel
import com.example.myapplication.ui.ArchiveScreen
import com.example.myapplication.ui.CategoriesScreen
import com.example.myapplication.ui.CategoryDetailScreen
import com.example.myapplication.ui.NotificationsScreen
import com.example.myapplication.ui.AppSettingsScreen
import com.example.myapplication.ui.CalendarScreen
import com.example.myapplication.ui.DataTransferScreen
import com.example.myapplication.ui.LocalHapticsEnabled
import com.example.myapplication.ui.LocalSoundEnabled
import com.example.myapplication.ui.SearchScreen
import com.example.myapplication.ui.RemindersScreen
import com.example.myapplication.ui.SettingsScreen
import com.example.myapplication.ui.SmartListScreen
import com.example.myapplication.ui.StatsScreen
import com.example.myapplication.ui.ThemeScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

sealed class Screen {
    data object Categories : Screen()
    data class CategoryDetail(val categoryId: Long) : Screen()
    data class SmartList(val type: SmartListType) : Screen()
    data object Archive : Screen()
    data object Notifications : Screen()
    data object Reminders : Screen()
    data object Stats : Screen()
    data object Settings : Screen()
    data object AppSettings : Screen()
    data object Theme : Screen()
    data object DataTransfer : Screen()
    data object Search : Screen()
    data object Calendar : Screen()
}

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel(this)

        setContent {
            val mainViewModel: TaskViewModel = viewModel()
            val appData by mainViewModel.appData.collectAsStateWithLifecycle()

            val systemDark = isSystemInDarkTheme()
            val isDark = when (appData.themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }

            MyApplicationTheme(darkTheme = isDark) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* результат */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Categories) }

                    BackHandler(enabled = currentScreen != Screen.Categories) {
                        currentScreen = when (currentScreen) {
                            is Screen.AppSettings -> Screen.Settings
                            is Screen.Theme -> Screen.Settings
                            is Screen.DataTransfer -> Screen.Settings
                            else -> Screen.Categories
                        }
                    }

                    CompositionLocalProvider(
                        LocalSoundEnabled provides appData.soundEnabled,
                        LocalHapticsEnabled provides appData.hapticsEnabled
                    ) {
                    when (val screen = currentScreen) {
                        is Screen.Categories -> {
                            CategoriesScreen(
                                viewModel = mainViewModel,
                                onNotificationsClick = { currentScreen = Screen.Notifications },
                                onArchiveClick = { currentScreen = Screen.Archive },
                                onRemindersClick = { currentScreen = Screen.Reminders },
                                onStatsClick = { currentScreen = Screen.Stats },
                                onSettingsClick = { currentScreen = Screen.Settings },
                                onSearchClick = { currentScreen = Screen.Search },
                                onCalendarClick = { currentScreen = Screen.Calendar },
                                onSmartListClick = { smartType ->
                                    currentScreen = Screen.SmartList(smartType)
                                },
                                onCategoryClick = { categoryId ->
                                    currentScreen = Screen.CategoryDetail(categoryId)
                                }
                            )
                        }
                        is Screen.CategoryDetail -> {
                            CategoryDetailScreen(
                                categoryId = screen.categoryId,
                                viewModel = mainViewModel,
                                onBack = { currentScreen = Screen.Categories }
                            )
                        }
                        is Screen.SmartList -> {
                            SmartListScreen(
                                type = screen.type,
                                viewModel = mainViewModel,
                                onBack = { currentScreen = Screen.Categories }
                            )
                        }
                        is Screen.Archive -> {
                            ArchiveScreen(
                                viewModel = mainViewModel,
                                onNotificationsClick = { currentScreen = Screen.Notifications },
                                onListsClick = { currentScreen = Screen.Categories },
                                onRemindersClick = { currentScreen = Screen.Reminders },
                                onStatsClick = { currentScreen = Screen.Stats },
                                onSettingsClick = { currentScreen = Screen.Settings }
                            )
                        }
                        is Screen.Notifications -> {
                            NotificationsScreen(
                                viewModel = mainViewModel,
                                onBack = { currentScreen = Screen.Categories }
                            )
                        }
                        is Screen.Reminders -> {
                            RemindersScreen(
                                viewModel = mainViewModel,
                                onNotificationsClick = { currentScreen = Screen.Notifications },
                                onArchiveClick = { currentScreen = Screen.Archive },
                                onListsClick = { currentScreen = Screen.Categories },
                                onStatsClick = { currentScreen = Screen.Stats },
                                onSettingsClick = { currentScreen = Screen.Settings }
                            )
                        }
                        is Screen.Stats -> {
                            StatsScreen(
                                viewModel = mainViewModel,
                                onNotificationsClick = { currentScreen = Screen.Notifications },
                                onListsClick = { currentScreen = Screen.Categories },
                                onRemindersClick = { currentScreen = Screen.Reminders },
                                onArchiveClick = { currentScreen = Screen.Archive },
                                onSettingsClick = { currentScreen = Screen.Settings }
                            )
                        }
                        is Screen.Settings -> {
                            SettingsScreen(
                                onBack = { currentScreen = Screen.Categories },
                                onAppSettingsClick = { currentScreen = Screen.AppSettings },
                                onThemeClick = { currentScreen = Screen.Theme },
                                onDataTransferClick = { currentScreen = Screen.DataTransfer }
                            )
                        }
                        is Screen.AppSettings -> {
                            AppSettingsScreen(
                                viewModel = mainViewModel,
                                onBack = { currentScreen = Screen.Settings }
                            )
                        }
                        is Screen.Theme -> {
                            ThemeScreen(
                                viewModel = mainViewModel,
                                onBack = { currentScreen = Screen.Settings }
                            )
                        }
                        is Screen.DataTransfer -> {
                            DataTransferScreen(
                                viewModel = mainViewModel,
                                onBack = { currentScreen = Screen.Settings }
                            )
                        }
                        is Screen.Search -> {
                            SearchScreen(
                                viewModel = mainViewModel,
                                onBack = { currentScreen = Screen.Categories }
                            )
                        }
                        is Screen.Calendar -> {
                            CalendarScreen(
                                viewModel = mainViewModel,
                                onBack = { currentScreen = Screen.Categories }
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

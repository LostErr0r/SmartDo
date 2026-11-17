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
import com.example.myapplication.data.SmartListType
import com.example.myapplication.notifications.createNotificationChannel
import com.example.myapplication.ui.ArchiveScreen
import com.example.myapplication.ui.CategoriesScreen
import com.example.myapplication.ui.CategoryDetailScreen
import com.example.myapplication.ui.NotificationsScreen
import com.example.myapplication.ui.RemindersScreen
import com.example.myapplication.ui.SmartListScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

sealed class Screen {
    data object Categories : Screen()
    data class CategoryDetail(val categoryId: Long) : Screen()
    data class SmartList(val type: SmartListType) : Screen()
    data object Archive : Screen()
    data object Notifications : Screen()
    data object Reminders : Screen()
}

class MainActivity : ComponentActivity() {
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
                        currentScreen = Screen.Categories
                    }

                    when (val screen = currentScreen) {
                        is Screen.Categories -> {
                            CategoriesScreen(
                                viewModel = mainViewModel,
                                isDarkTheme = isDark,
                                onToggleTheme = { mainViewModel.toggleTheme(isDark) },
                                onNotificationsClick = { currentScreen = Screen.Notifications },
                                onArchiveClick = { currentScreen = Screen.Archive },
                                onRemindersClick = { currentScreen = Screen.Reminders },
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
                                isDarkTheme = isDark,
                                onToggleTheme = { mainViewModel.toggleTheme(isDark) },
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
                                isDarkTheme = isDark,
                                onToggleTheme = { mainViewModel.toggleTheme(isDark) },
                                onNotificationsClick = { currentScreen = Screen.Notifications },
                                onListsClick = { currentScreen = Screen.Categories },
                                onRemindersClick = { currentScreen = Screen.Reminders }
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
                                isDarkTheme = isDark,
                                onToggleTheme = { mainViewModel.toggleTheme(isDark) },
                                onNotificationsClick = { currentScreen = Screen.Notifications },
                                onArchiveClick = { currentScreen = Screen.Archive },
                                onListsClick = { currentScreen = Screen.Categories }
                            )
                        }
                    }
                }
            }
        }
    }
}

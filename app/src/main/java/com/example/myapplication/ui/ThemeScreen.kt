package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.TaskViewModel

private data class ThemeOption(
    val mode: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val appData by viewModel.appData.collectAsStateWithLifecycle()
    val current = appData.themeMode

    val options = listOf(
        ThemeOption(
            mode = "system",
            title = "Системная",
            subtitle = "Как в настройках телефона",
            icon = Icons.Default.PhoneAndroid,
            iconTint = Color(0xFF8E8E93)
        ),
        ThemeOption(
            mode = "light",
            title = "Светлая",
            subtitle = "Всегда светлое оформление",
            icon = Icons.Default.LightMode,
            iconTint = Color(0xFFFF9500)
        ),
        ThemeOption(
            mode = "dark",
            title = "Тёмная",
            subtitle = "Всегда тёмное оформление",
            icon = Icons.Default.DarkMode,
            iconTint = Color(0xFF5856D6)
        )
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Изменить тему",
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    options.forEachIndexed { index, option ->
                        ThemeOptionRow(
                            option = option,
                            selected = option.mode == current,
                            onClick = { viewModel.setThemeMode(option.mode) }
                        )
                        if (index < options.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    option: ThemeOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            option.icon,
            contentDescription = null,
            tint = option.iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                option.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                option.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Выбрано",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

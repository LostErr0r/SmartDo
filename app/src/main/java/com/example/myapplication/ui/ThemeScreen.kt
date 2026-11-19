package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.AppIcon
import com.example.myapplication.AppIconManager
import com.example.myapplication.R
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
    val context = LocalContext.current
    var currentIcon by remember { mutableStateOf(AppIconManager.current(context)) }

    val options = listOf(
        ThemeOption(
            mode = "system",
            title = stringResource(R.string.theme_system),
            subtitle = stringResource(R.string.theme_system_sub),
            icon = Icons.Default.PhoneAndroid,
            iconTint = Color(0xFF8E8E93)
        ),
        ThemeOption(
            mode = "light",
            title = stringResource(R.string.theme_light),
            subtitle = stringResource(R.string.theme_light_sub),
            icon = Icons.Default.LightMode,
            iconTint = Color(0xFFFF9500)
        ),
        ThemeOption(
            mode = "dark",
            title = stringResource(R.string.theme_dark),
            subtitle = stringResource(R.string.theme_dark_sub),
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
                        stringResource(R.string.theme_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.3).sp
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            SectionHeader(stringResource(R.string.theme_section_mode))

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

            Spacer(Modifier.height(12.dp))
            SectionHeader(stringResource(R.string.theme_section_icon))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        AppIcon.entries.forEach { icon ->
                            AppIconTile(
                                icon = icon,
                                selected = icon == currentIcon,
                                onClick = {
                                    AppIconManager.set(context, icon)
                                    currentIcon = icon
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.theme_icon_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, start = 8.dp, end = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
    )
}

@Composable
private fun AppIconTile(
    icon: AppIcon,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = stringResource(icon.titleRes)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = painterResource(icon.previewRes),
                contentDescription = title,
                modifier = Modifier
                    .size(56.dp)
                    .border(
                        BorderStroke(
                            2.5.dp,
                            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        CircleShape
                    )
                    .padding(4.dp)
            )
            if (selected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.selected),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(3.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
                contentDescription = stringResource(R.string.selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

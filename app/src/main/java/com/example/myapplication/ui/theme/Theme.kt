package com.example.myapplication.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = IosDarkPrimary,
    onPrimary = IosDarkTextPrimary,
    primaryContainer = IosDarkSurfaceVariant,
    onPrimaryContainer = IosDarkTextPrimary,
    background = IosDarkBackground,
    onBackground = IosDarkTextPrimary,
    surface = IosDarkSurface,
    onSurface = IosDarkTextPrimary,
    surfaceVariant = IosDarkSurfaceVariant,
    onSurfaceVariant = IosDarkTextSecondary,
    outline = IosDarkBorder,
    error = IosDarkError,
    errorContainer = IosDarkError.copy(alpha = 0.2f),
    onError = IosDarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = IosLightPrimary,
    onPrimary = IosLightSurface,
    primaryContainer = IosLightSurface,
    onPrimaryContainer = IosLightTextPrimary,
    background = IosLightBackground,
    onBackground = IosLightTextPrimary,
    surface = IosLightSurface,
    onSurface = IosLightTextPrimary,
    surfaceVariant = IosLightSurfaceVariant,
    onSurfaceVariant = IosLightTextSecondary,
    outline = IosLightBorder,
    error = IosLightError,
    errorContainer = IosLightError.copy(alpha = 0.15f),
    onError = IosLightSurface
)

val IosShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = IosShapes,
        content = content
    )
}

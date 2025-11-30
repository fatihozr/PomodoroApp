package com.fatih.pomodoroapp1.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color.White,
    secondary = Color(0xFF5F6368),
    surface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF5F6368)
)

@Composable
fun PomodoroTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
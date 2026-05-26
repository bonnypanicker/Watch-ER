package com.watch_er.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = WatchGreen,
    onPrimary = Paper,
    secondary = WatchGreenDark,
    background = SoftPaper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    onSurfaceVariant = SoftInk,
    outlineVariant = Line
)

@Composable
fun WatchErTheme(content: @Composable () -> Unit) {
    val darkScheme = darkColorScheme(
        primary = WatchGreen,
        onPrimary = Ink,
        secondary = WatchGreenDark,
        background = Ink,
        onBackground = Paper,
        surface = ColorDarkSurface,
        onSurface = Paper,
        onSurfaceVariant = ColorDarkMuted,
        outlineVariant = ColorDarkLine
    )

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkScheme else LightScheme,
        typography = WatchTypography,
        content = content
    )
}

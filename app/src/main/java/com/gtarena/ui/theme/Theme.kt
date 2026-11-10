package com.gtarena.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GTColors = darkColorScheme(
    primary = Lime,
    onPrimary = OnLime,
    secondary = Lime,
    onSecondary = OnLime,
    background = Ground,
    onBackground = TextPrimary,
    surface = Panel,
    onSurface = TextPrimary,
    surfaceVariant = Panel,
    onSurfaceVariant = TextSecondary,
    outline = Hairline,
    outlineVariant = Hairline,
    error = Warn
)

@Composable
fun GTArenaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GTColors,
        typography = GTTypography,
        content = content
    )
}

package com.shl.meditation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Dynamic colour is not used. The palette is fixed so the app looks the same on
// every device, and so no accent hue can leak into a screen meant to be quiet.

private val LightColors = lightColorScheme(
    primary = PaperInk,
    onPrimary = Paper,
    secondary = PaperInkMuted,
    onSecondary = Paper,
    background = Paper,
    onBackground = PaperInk,
    surface = Paper,
    onSurface = PaperInk,
    surfaceVariant = PaperRaised,
    onSurfaceVariant = PaperInkMuted,
    outline = PaperLine,
    outlineVariant = PaperLine,
)

private val DarkColors = darkColorScheme(
    primary = NightInk,
    onPrimary = Night,
    secondary = NightInkMuted,
    onSecondary = Night,
    background = Night,
    onBackground = NightInk,
    surface = Night,
    onSurface = NightInk,
    surfaceVariant = NightRaised,
    onSurfaceVariant = NightInkMuted,
    outline = NightLine,
    outlineVariant = NightLine,
)

@Composable
fun MeditationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

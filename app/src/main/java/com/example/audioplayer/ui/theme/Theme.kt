package com.example.audioplayer.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary          = Red,
    onPrimary        = White,
    secondary        = DarkRed,
    onSecondary      = White,
    background       = Black,
    onBackground     = White,
    surface          = DarkGray,
    onSurface        = White,
    surfaceVariant   = MediumGray,
    onSurfaceVariant = LightGray
)

@Composable
fun AudioPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}

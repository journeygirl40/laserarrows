package com.laserarrows.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LaserArrowsColorScheme = darkColorScheme(
    primary = Neon1,
    secondary = Neon2,
    background = Bg1,
    surface = Bg2,
    error = DangerColor,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = TextPrimary,
)

@Composable
fun LaserArrowsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LaserArrowsColorScheme,
        typography = LaserArrowsTypography,
        content = content,
    )
}

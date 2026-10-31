package com.laserarrows.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LaserArrowsColorScheme = lightColorScheme(
    primary = Neon1,
    secondary = Neon2,
    background = Bg2,
    surface = GlassFill,
    error = DangerColor,
    onPrimary = GlassHi,
    onSecondary = GlassHi,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = GlassHi,
)

@Composable
fun LaserArrowsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LaserArrowsColorScheme,
        typography = LaserArrowsTypography,
        content = content,
    )
}

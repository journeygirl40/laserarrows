package com.laserarrows.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.laserarrows.app.engine.LaserColor

val Bg1 = Color(0xFF0D0B1F)
val Bg2 = Color(0xFF1A1035)
val Bg3 = Color(0xFF2B0F3D)

val GlassFill = Color(0x0EFFFFFF)
val GlassBorder = Color(0x24FFFFFF)
val GlassHi = Color(0x47FFFFFF)

val TextPrimary = Color(0xFFF3F1FB)
val TextMuted = Color(0xFF9691B8)

val DangerColor = Color(0xFFFF4D7D)
val GoldColor = Color(0xFFFFD23F)
val Neon1 = Color(0xFF7C5CFF)
val Neon2 = Color(0xFF4DD9FF)
val PurpleAccent = Color(0xFFA855F7)

fun LaserColor.toComposeColor(): Color = when (this) {
    LaserColor.RED -> Color(0xFFFF3B30)
    LaserColor.BLUE -> Color(0xFF4DD9FF)
    LaserColor.GREEN -> Color(0xFF7CFF9E)
    LaserColor.YELLOW -> Color(0xFFFFD23F)
    LaserColor.PURPLE -> Color(0xFFC77DFF)
    LaserColor.ORANGE -> Color(0xFFFF9500)
}

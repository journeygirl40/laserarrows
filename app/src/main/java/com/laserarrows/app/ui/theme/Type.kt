package com.laserarrows.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.journeygirl.laserarrows.R

val SpaceGroteskFontFamily = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

private val baseTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 19.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp, letterSpacing = 1.sp),
)

/** 2026年らしい、幾何学的で少し未来感のあるフォント(Space Grotesk)を全体に適用する。 */
val LaserArrowsTypography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = SpaceGroteskFontFamily),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = SpaceGroteskFontFamily),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = SpaceGroteskFontFamily),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = SpaceGroteskFontFamily),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = SpaceGroteskFontFamily),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = SpaceGroteskFontFamily),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = SpaceGroteskFontFamily),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = SpaceGroteskFontFamily),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = SpaceGroteskFontFamily),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = SpaceGroteskFontFamily),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = SpaceGroteskFontFamily),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = SpaceGroteskFontFamily),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = SpaceGroteskFontFamily),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = SpaceGroteskFontFamily),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = SpaceGroteskFontFamily),
)

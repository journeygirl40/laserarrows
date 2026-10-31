package com.laserarrows.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.laserarrows.app.R

/** 丸みのある日本語フォント(M PLUS Rounded 1c)。カジュアルゲームらしい柔らかさと、太字での存在感を両立する。 */
val RoundedFontFamily = FontFamily(
    Font(R.font.mplus_rounded_medium, FontWeight.Normal),
    Font(R.font.mplus_rounded_medium, FontWeight.Medium),
    Font(R.font.mplus_rounded_bold, FontWeight.SemiBold),
    Font(R.font.mplus_rounded_bold, FontWeight.Bold),
    Font(R.font.mplus_rounded_black, FontWeight.ExtraBold),
    Font(R.font.mplus_rounded_black, FontWeight.Black),
)

private val baseTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 19.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp, letterSpacing = 1.sp),
)

private fun TextStyle.rounded() = copy(fontFamily = RoundedFontFamily, color = TextPrimary)

val LaserArrowsTypography = Typography(
    displayLarge = baseTypography.displayLarge.rounded(),
    displayMedium = baseTypography.displayMedium.rounded(),
    displaySmall = baseTypography.displaySmall.rounded(),
    headlineLarge = baseTypography.headlineLarge.rounded(),
    headlineMedium = baseTypography.headlineMedium.rounded(),
    headlineSmall = baseTypography.headlineSmall.rounded(),
    titleLarge = baseTypography.titleLarge.rounded(),
    titleMedium = baseTypography.titleMedium.rounded(),
    titleSmall = baseTypography.titleSmall.rounded(),
    bodyLarge = baseTypography.bodyLarge.rounded(),
    bodyMedium = baseTypography.bodyMedium.rounded(),
    bodySmall = baseTypography.bodySmall.rounded(),
    labelLarge = baseTypography.labelLarge.rounded(),
    labelMedium = baseTypography.labelMedium.rounded(),
    labelSmall = baseTypography.labelSmall.rounded(),
)

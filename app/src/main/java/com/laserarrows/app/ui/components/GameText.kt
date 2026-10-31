package com.laserarrows.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.laserarrows.app.ui.theme.TextPrimary

/**
 * 見出し・演出用の「縁取り文字」。太い縁と落ち影で、明るい背景の上でもゲームらしく立体的に読ませる。
 */
@Composable
fun GameText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    outline: Color = Color.White,
    shadowColor: Color = TextPrimary.copy(alpha = 0.35f),
    textAlign: TextAlign = TextAlign.Center,
    singleLine: Boolean = false,
) {
    val maxLines = if (singleLine) 1 else Int.MAX_VALUE
    val strokeWidth = fontSize.value * 0.45f
    Box(modifier = modifier) {
        Text(
            text,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = outline,
            textAlign = textAlign,
            maxLines = maxLines,
            softWrap = !singleLine,
            style = LocalTextStyle.current.copy(
                drawStyle = Stroke(width = strokeWidth),
                shadow = Shadow(shadowColor, Offset(0f, strokeWidth * 0.6f), strokeWidth * 0.5f),
            ),
        )
        Text(text, fontSize = fontSize, fontWeight = FontWeight.Black, color = color, textAlign = textAlign, maxLines = maxLines, softWrap = !singleLine)
    }
}

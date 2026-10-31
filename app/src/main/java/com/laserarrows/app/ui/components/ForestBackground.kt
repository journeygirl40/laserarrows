package com.laserarrows.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.laserarrows.app.ui.theme.Bg1
import com.laserarrows.app.ui.theme.Bg2
import com.laserarrows.app.ui.theme.Bg3

/**
 * 全画面共通の背景: 日差しの差す空、ゆっくり流れる雲、重なった丘と丸い木。
 * 手描きの一枚絵の代わりに図形を重ねて、奥行きのある「作り込まれた世界」の空気を出す。
 */
@Composable
fun ForestBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "forestBg")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)),
        label = "cloudDrift",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(0f to Bg1, 0.55f to Bg2, 1f to Bg3))
        // 右上からの日差し。
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0x99FFF6C8), Color(0x00FFF6C8)), center = Offset(w * 0.85f, h * 0.06f), radius = w * 0.7f),
            radius = w * 0.7f,
            center = Offset(w * 0.85f, h * 0.06f),
        )
        drawCloud(Offset(((0.15f + drift) % 1.3f - 0.15f) * w, h * 0.10f), w * 0.30f)
        drawCloud(Offset(((0.75f + drift * 0.7f) % 1.3f - 0.15f) * w, h * 0.19f), w * 0.22f)
        drawCloud(Offset(((0.45f + drift * 0.5f) % 1.3f - 0.15f) * w, h * 0.04f), w * 0.18f)
        // 奥の丘・手前の丘(手前ほど濃い緑)。
        drawHill(h * 0.78f, w, h, Color(0xFFBDE6A0), bulge = 0.10f, phase = 0.2f)
        drawTrees(h * 0.76f, w, Color(0xFF8CCB6A))
        drawHill(h * 0.86f, w, h, Color(0xFF94D16C), bulge = 0.08f, phase = 0.7f)
        drawHill(h * 0.93f, w, h, Color(0xFF7DC257), bulge = 0.05f, phase = 0.4f)
    }
}

private fun DrawScope.drawCloud(center: Offset, width: Float) {
    val c = Color(0xE6FFFFFF)
    val r = width * 0.22f
    drawOval(c, topLeft = Offset(center.x - width / 2f, center.y - r * 0.3f), size = Size(width, r * 1.3f))
    drawCircle(c, radius = r, center = Offset(center.x - width * 0.18f, center.y))
    drawCircle(c, radius = r * 1.35f, center = Offset(center.x + width * 0.06f, center.y - r * 0.35f))
}

private fun DrawScope.drawHill(top: Float, w: Float, h: Float, color: Color, bulge: Float, phase: Float) {
    val path = Path().apply {
        moveTo(0f, h)
        lineTo(0f, top)
        cubicTo(w * phase, top - h * bulge, w * (phase + 0.3f), top - h * bulge * 0.2f, w, top - h * bulge * 0.5f)
        lineTo(w, h)
        close()
    }
    drawPath(path, color)
}

/** 奥の丘に並ぶ、丸い木のシルエット。 */
private fun DrawScope.drawTrees(baseY: Float, w: Float, color: Color) {
    listOf(0.06f to 0.9f, 0.18f to 0.7f, 0.82f to 1f, 0.93f to 0.75f).forEach { (x, s) ->
        val cx = w * x
        val r = w * 0.055f * s
        drawRect(Color(0xFF9C7048), topLeft = Offset(cx - r * 0.12f, baseY - r * 0.4f), size = Size(r * 0.24f, r * 0.9f))
        drawCircle(color, radius = r, center = Offset(cx, baseY - r * 1.1f))
        drawCircle(color.copy(alpha = 0.9f), radius = r * 0.75f, center = Offset(cx - r * 0.45f, baseY - r * 0.75f))
    }
}

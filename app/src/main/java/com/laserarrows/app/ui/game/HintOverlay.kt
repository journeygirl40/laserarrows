package com.laserarrows.app.ui.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.engine.LaserResult
import com.laserarrows.app.ui.theme.GoldColor

/**
 * ヒントの見せ方。タップする子を金色の輪で脈打たせ、その子が走る道筋を「流れる点線」で相手まで描き、
 * 相手の上に増えるなかまの数(+3など)を出す。タップする子の下では指マークが弾んで「ここ!」を伝える。
 * 盤面のタップは邪魔しない(このレイヤーは押せない)。
 */
@Composable
fun HintOverlay(
    origin: CellPos,
    result: LaserResult,
    gained: Int,
    cellSizeDp: Dp,
    gapDp: Dp,
    cellSizePx: Float,
    gapPx: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "hint")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "hintPulse",
    )
    val march by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing)),
        label = "hintMarch",
    )
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "hintBob",
    )
    val stridePx = cellSizePx + gapPx
    val strideDp = cellSizeDp + gapDp

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            fun center(pos: CellPos) = Offset(pos.col * stridePx + cellSizePx / 2f, pos.row * stridePx + cellSizePx / 2f)

            // 走る道筋: 相手に向かって流れる点線(白いふち+金色)。
            val points = result.path
            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(center(points.first()).x, center(points.first()).y)
                    points.drop(1).forEach { lineTo(center(it).x, center(it).y) }
                }
                val dash = cellSizePx * 0.22f
                val width = cellSizePx * 0.11f
                drawPath(path, Color.White.copy(alpha = 0.85f), style = Stroke(width * 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(
                    path,
                    GoldColor,
                    style = Stroke(
                        width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash * 0.8f), phase = -march * dash * 1.8f),
                    ),
                )
            }

            // タップする子: 脈打って広がる金色の輪(2重)。
            val topLeft = Offset(origin.col * stridePx, origin.row * stridePx)
            for (k in 0..1) {
                val t = (pulse + k * 0.5f) % 1f
                val grow = cellSizePx * 0.22f * t
                drawRoundRect(
                    color = GoldColor.copy(alpha = (1f - t) * 0.9f),
                    topLeft = topLeft - Offset(grow, grow),
                    size = Size(cellSizePx + grow * 2, cellSizePx + grow * 2),
                    cornerRadius = CornerRadius(cellSizePx * 0.3f + grow),
                    style = Stroke(cellSizePx * 0.07f),
                )
            }
            drawRoundRect(
                color = GoldColor,
                topLeft = topLeft,
                size = Size(cellSizePx, cellSizePx),
                cornerRadius = CornerRadius(cellSizePx * 0.28f),
                style = Stroke(cellSizePx * 0.08f),
            )
        }

        // 「ここ!」の指マーク。タップする子の右下で弾む。
        Text(
            "👆",
            fontSize = (cellSizeDp.value * 0.42f).sp,
            modifier = Modifier.offset(
                x = strideDp * origin.col + cellSizeDp * 0.55f,
                y = strideDp * origin.row + cellSizeDp * (0.55f + 0.08f * bob),
            ),
        )

        // 出会う相手の上に、この1手で増えるなかまの数。
        val hit = result.hit
        if (hit != null && gained > 0) {
            Text(
                "+$gained",
                fontSize = (cellSizeDp.value * 0.3f).coerceAtLeast(11f).sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier
                    .offset(x = strideDp * hit.col + cellSizeDp * 0.18f, y = strideDp * hit.row - cellSizeDp * 0.22f)
                    .background(GoldColor, RoundedCornerShape(999.dp))
                    .border(1.5.dp, Color.White, RoundedCornerShape(999.dp))
                    .padding(horizontal = 7.dp, vertical = 1.dp),
            )
        }
    }
}

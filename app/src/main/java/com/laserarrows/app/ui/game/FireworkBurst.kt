package com.laserarrows.app.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** 尺玉花火の一本一本の光条。widthとsparkleは個体差を出して単調にならないようにする。 */
internal data class FireworkRay(val angle: Float, val length: Float, val width: Float, val sparkle: Boolean)

internal fun randomFireworkRays(seed: Long, count: Int = 40): List<FireworkRay> {
    val rnd = Random(seed xor 0x5EED1234)
    val step = (2 * Math.PI).toFloat() / count
    return List(count) { i ->
        val baseAngle = i * step
        val jitter = (rnd.nextFloat() - 0.5f) * step * 0.8f
        FireworkRay(
            angle = baseAngle + jitter,
            length = 26f + rnd.nextFloat() * 34f,
            width = 1.1f + rnd.nextFloat() * 1.1f,
            sparkle = rnd.nextFloat() < 0.35f,
        )
    }
}

/**
 * 尺玉花火が開く瞬間の放射状バースト。[BeamCanvas]の着弾演出と、レベルクリア時に
 * 複数箇所で同時に打ち上げる[ClearFinaleCanvas]の両方から共通で使う。
 * [t]はバースト進行度(0=開く直前, 1=消えきった), [flarePulse]は着弾直後の白い閃光の進行度。
 */
internal fun DrawScope.drawFireworkBurst(
    center: Offset,
    burstColor: Color,
    scaleFactor: Float,
    t: Float,
    flarePulse: Float,
    rays: List<FireworkRay>,
) {
    // 着弾の瞬間の白い閃光
    drawCircle(
        color = Color.White.copy(alpha = ((1f - flarePulse) * 0.9f).coerceIn(0f, 1f)),
        radius = (10f + flarePulse * 22f) * scaleFactor,
        center = center,
    )

    // 花火が開く前の、やわらかいアンビエントブルーム(受光した玉の色)
    drawCircle(
        color = burstColor.copy(alpha = ((1f - t) * 0.3f).coerceIn(0f, 1f)),
        radius = (18f + t * 9f) * scaleFactor,
        center = center,
    )

    // 尺玉花火のように放射状へ密に伸びる光条。色は受光した玉の色で統一し、
    // 根元だけ白く熱いコアにすることで単色の輪ではなく本物の光の質感に近づける。
    // 本物の花火は一瞬でパッと開いてから消えていくので、開く動きは素早く・消える動きはゆっくりにする。
    val growT = (t / 0.12f).coerceIn(0f, 1f)
    val rayAlpha = (1f - ((t - 0.12f) / 0.88f).coerceIn(0f, 1f))
    val innerR = 4f * scaleFactor
    rays.forEach { ray ->
        val dirX = cos(ray.angle)
        val dirY = sin(ray.angle)
        val fullLen = ray.length * scaleFactor
        val curLen = innerR + (fullLen - innerR) * growT
        val startPoint = Offset(center.x + dirX * innerR, center.y + dirY * innerR)
        val ctrlPoint = Offset(center.x + dirX * curLen * 0.55f, center.y + dirY * curLen * 0.55f)
        // 重力で少したわむ「柳咲き」の曲線。上向きの光条ほどまっすぐに、
        // 下向き・横向きほど大きくたわむようにして本物の写真に近づける。
        val droopFactor = 0.28f + 0.72f * ((dirY + 1f) / 2f)
        val droopAmount = curLen * 0.24f * droopFactor
        val tipPoint = Offset(center.x + dirX * curLen, center.y + dirY * curLen + droopAmount)

        val rayPath = Path().apply {
            moveTo(startPoint.x, startPoint.y)
            quadraticTo(ctrlPoint.x, ctrlPoint.y, tipPoint.x, tipPoint.y)
        }
        // 根元は白熱、途中から受光色になり、先端の手前で消える。
        val rayBrush = Brush.linearGradient(
            0f to Color.White.copy(alpha = rayAlpha),
            0.22f to burstColor.copy(alpha = rayAlpha),
            0.7f to burstColor.copy(alpha = rayAlpha * 0.9f),
            1f to burstColor.copy(alpha = 0f),
            start = startPoint,
            end = tipPoint,
        )
        drawPath(
            path = rayPath,
            brush = rayBrush,
            style = Stroke(width = ray.width * scaleFactor, cap = StrokeCap.Round),
        )
        if (ray.sparkle && growT > 0.85f) {
            drawCircle(color = Color.White.copy(alpha = rayAlpha * 0.85f), radius = 1f * scaleFactor, center = tipPoint)
        }
    }

    // 中心の発光コア(縮みながら消える)
    drawCircle(
        color = Color.White.copy(alpha = (1f - t * 2.2f).coerceIn(0f, 1f)),
        radius = (13f * (1f - t * 0.5f) * scaleFactor).coerceAtLeast(0f),
        center = center,
    )
}

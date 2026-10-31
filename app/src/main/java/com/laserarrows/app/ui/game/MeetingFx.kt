package com.laserarrows.app.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** 出会いの瞬間のハート色(どの動物の色とも喧嘩しない、明るいピンク)。 */
internal val LovePink = Color(0xFFFF6F9C)

/** 盤面の上を動くキャラクター1体分の姿勢。center=顔の中心、scaleX/Y=つぶれ具合、rotation=傾き(度)。 */
internal data class SpritePose(
    val center: Offset,
    val ground: Offset,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
)

/** 出会いで弾ける小さなハート1つ分(飛ぶ向き・速さ・大きさ・揺れの位相)。 */
internal data class LoveHeartSpec(val angle: Float, val speed: Float, val size: Float, val wobble: Float)

internal fun randomLoveHearts(seed: Long): List<LoveHeartSpec> {
    val rnd = Random(seed xor 0x1F0E_7A11)
    return List(7) { i ->
        // 上半分(-150°〜-30°)に均等に散らしつつ少し揺らぎを入れ、左右どちらにも偏らないようにする。
        val base = -150f + i * (120f / 6f)
        LoveHeartSpec(
            angle = ((base + (rnd.nextFloat() - 0.5f) * 14f) * PI / 180f).toFloat(),
            speed = 0.75f + rnd.nextFloat() * 0.45f,
            size = 0.15f + rnd.nextFloat() * 0.09f,
            wobble = rnd.nextFloat() * 6.28f,
        )
    }
}

internal fun clamp01(x: Float) = x.coerceIn(0f, 1f)

internal fun easeOutCubic(x: Float): Float = 1f - (1f - clamp01(x)).pow(3)

/** 目標を少し行き過ぎてから戻る(ぽよんと膨らむハートに使う)。 */
internal fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val t = clamp01(x) - 1f
    return 1f + c3 * t * t * t + c1 * t * t
}

internal fun mix(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

/** 足元の丸い影。跳ねて高く上がっているほど小さく薄くなる。 */
internal fun DrawScope.drawGroundShadow(pose: SpritePose, cellSizePx: Float) {
    val height = (pose.ground.y - pose.center.y).coerceAtLeast(0f) / cellSizePx
    val shrink = clamp01(1f - height * 2.2f)
    val w = cellSizePx * 0.42f * (0.6f + 0.4f * shrink)
    val h = cellSizePx * 0.11f * (0.6f + 0.4f * shrink)
    val foot = pose.ground + Offset(0f, cellSizePx * 0.25f)
    drawOval(
        color = Color.Black.copy(alpha = 0.2f * (0.5f + 0.5f * shrink) * pose.alpha),
        topLeft = Offset(foot.x - w / 2f, foot.y - h / 2f),
        size = Size(w, h),
    )
}

/** キャラクターの画像を、足元を支点につぶしたり傾けたりして描く。 */
internal fun DrawScope.drawSprite(image: ImageBitmap, pose: SpritePose, sizePx: Float) {
    if (pose.alpha <= 0.01f || sizePx <= 1f) return
    val half = sizePx / 2f
    withTransform({
        translate(pose.center.x, pose.center.y)
        rotate(pose.rotation, pivot = Offset.Zero)
        scale(pose.scaleX, pose.scaleY, pivot = Offset(0f, half))
    }) {
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset((-half).roundToInt(), (-half).roundToInt()),
            dstSize = IntSize(sizePx.roundToInt(), sizePx.roundToInt()),
            alpha = clamp01(pose.alpha),
            filterQuality = FilterQuality.Medium,
        )
    }
}

/**
 * 出会いの瞬間の広がる光の輪と、上へふわふわ舞い上がる小さなハートたち。
 * m=出会い演出全体の進み(0..1)。二人がくっつく0.2付近から始まる。
 */
internal fun DrawScope.drawLoveBurst(
    center: Offset,
    animalColor: Color,
    heart: ImageBitmap,
    hearts: List<LoveHeartSpec>,
    cellSizePx: Float,
    m: Float,
) {
    val ring = clamp01((m - 0.2f) / 0.45f)
    if (ring in 0.001f..0.999f) {
        val e = easeOutCubic(ring)
        drawCircle(
            color = LovePink.copy(alpha = (1f - e) * 0.6f),
            radius = cellSizePx * (0.25f + 0.95f * e),
            center = center,
            style = Stroke(width = cellSizePx * (0.06f * (1f - e) + 0.015f)),
        )
        drawCircle(
            color = Color.White.copy(alpha = (1f - e) * 0.55f),
            radius = cellSizePx * (0.18f + 0.6f * e),
            center = center,
        )
    }
    val ring2 = clamp01((m - 0.28f) / 0.45f)
    if (ring2 in 0.001f..0.999f) {
        val e = easeOutCubic(ring2)
        drawCircle(
            color = animalColor.copy(alpha = (1f - e) * 0.5f),
            radius = cellSizePx * (0.2f + 0.75f * e),
            center = center,
            style = Stroke(width = cellSizePx * (0.04f * (1f - e) + 0.01f)),
        )
    }

    // 輪と一緒に散る小さな光の粒(白と動物の色)。
    val spark = clamp01((m - 0.2f) / 0.4f)
    if (spark in 0.001f..0.999f) {
        val e = easeOutCubic(spark)
        for (k in 0 until 12) {
            val a = (k / 12f) * 2f * PI.toFloat() + 0.26f
            val d = cellSizePx * (0.2f + (0.7f + (k % 3) * 0.12f) * e)
            val p = center + Offset(cos(a) * d, sin(a) * d)
            val c = if (k % 2 == 0) Color.White else animalColor
            drawCircle(c.copy(alpha = (1f - spark) * 0.95f), radius = cellSizePx * 0.035f * (1f - spark * 0.5f), center = p)
        }
    }

    val float = clamp01((m - 0.24f) / 0.68f)
    if (float in 0.001f..0.999f) {
        val e = easeOutCubic(float)
        val pop = easeOutBack(clamp01(float / 0.25f))
        val alpha = (1f - float).pow(1.2f)
        hearts.forEach { h ->
            val d = cellSizePx * 0.85f * h.speed * e
            val drift = sin(float * 7f + h.wobble) * cellSizePx * 0.05f
            val p = center + Offset(cos(h.angle) * d + drift, sin(h.angle) * d - float * cellSizePx * 0.35f)
            drawSprite(
                heart,
                SpritePose(center = p, ground = p, rotation = sin(float * 6f + h.wobble) * 16f, alpha = alpha),
                cellSizePx * h.size * pop,
            )
        }
    }
}

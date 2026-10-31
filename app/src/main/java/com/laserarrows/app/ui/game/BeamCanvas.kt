package com.laserarrows.app.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.ui.theme.EggTrail
import com.laserarrows.app.ui.theme.toComposeColor
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ParticleSpec(val angle: Float, val speed: Float, val size: Float)

/** ビームが伸びていく先端から後方へ飛び散る火花(ロケット花火の軌道のような質感を出す)。 */
private data class TrailSpark(val trailOffset: Float, val perp: Float, val size: Float, val twinklePhase: Float)

@Composable
fun BeamCanvas(
    shot: ShotAnimation?,
    cellSizePx: Float,
    gapPx: Float,
    onRevealFinished: () -> Unit,
    onSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val growth = remember(shot?.token) { Animatable(0f) }
    val muzzleFlash = remember(shot?.token) { Animatable(0f) }
    val shockwave = remember(shot?.token) { Animatable(0f) }
    val burstProgress = remember(shot?.token) { Animatable(0f) }
    val flarePulse = remember(shot?.token) { Animatable(0f) }
    val missFizzle = remember(shot?.token) { Animatable(0f) }
    val particles = remember(shot?.token) {
        val rnd = Random(shot?.token ?: 0L)
        List(28) {
            ParticleSpec(
                angle = rnd.nextFloat() * (2 * Math.PI).toFloat(),
                speed = 50f + rnd.nextFloat() * 100f,
                size = 1.4f + rnd.nextFloat() * 2.8f,
            )
        }
    }
    // 着弾時に本物の尺玉花火のように放射状へ密に伸びる光条。写真のような柳(やなぎ)咲きに
    // 近づけるため、本数を多くし長さ・太さ・きらめきの有無に個体差を持たせる。
    val fireworkRays = remember(shot?.token) { randomFireworkRays(shot?.token ?: 0L) }
    // ビームの先端が伸びていく間、後方にこぼれ落ちる火花(花火のロケット軌道のような質感)。
    val trailSparks = remember(shot?.token) {
        val rnd = Random((shot?.token ?: 0L) xor 0x51ED2701)
        List(10) {
            TrailSpark(
                trailOffset = 2f + rnd.nextFloat() * 16f,
                perp = (rnd.nextFloat() - 0.5f) * 6f,
                size = 0.6f + rnd.nextFloat() * 1.1f,
                twinklePhase = rnd.nextFloat() * 6.28f,
            )
        }
    }

    LaunchedEffect(shot?.token) {
        val s = shot ?: return@LaunchedEffect
        val path = s.result.path
        val segCount = (path.size - 1).coerceAtLeast(0)

        growth.snapTo(0f)
        muzzleFlash.snapTo(0f)
        shockwave.snapTo(0f)
        launch { muzzleFlash.animateTo(1f, tween(220, easing = LinearEasing)) }
        launch { shockwave.animateTo(1f, tween(320, easing = EaseOutCubic)) }

        if (segCount > 0) {
            growth.animateTo(segCount.toFloat(), tween(segCount * 36, easing = LinearEasing))
        }
        onRevealFinished()
        if (s.result.hit != null) {
            burstProgress.snapTo(0f)
            flarePulse.snapTo(0f)
            launch { flarePulse.animateTo(1f, tween(190, easing = EaseOutCubic)) }
            burstProgress.animateTo(1f, tween(620, easing = LinearEasing))
        } else {
            missFizzle.snapTo(0f)
            missFizzle.animateTo(1f, tween(280, easing = LinearEasing))
            delay(60)
        }
        onSettled()
    }

    val stridePx = cellSizePx + gapPx

    fun cellCenter(pos: CellPos): Offset =
        Offset(pos.col * stridePx + cellSizePx / 2f, pos.row * stridePx + cellSizePx / 2f)

    val scaleFactor = cellSizePx / 60f

    Canvas(modifier = modifier) {
        val s = shot ?: return@Canvas
        val path = s.result.path
        if (path.isEmpty()) return@Canvas
        val colorTrail = s.result.colorTrail
        val maxSeg = (path.size - 1).coerceAtLeast(0)
        val g = growth.value.coerceIn(0f, maxSeg.toFloat())
        val fullSegs = floor(g).toInt().coerceIn(0, maxSeg)
        val frac = g - fullSegs
        val stillGrowing = g < maxSeg.toFloat()

        // 明るい芝生の上でも映えるよう、白い縁取りの上に動物の色のリボンを重ね、中央に足あとのような白い点線を走らせる。
        val footprints = PathEffect.dashPathEffect(floatArrayOf(4f * scaleFactor, 9f * scaleFactor), 0f)
        fun drawBeamSegment(p1: Offset, p2: Offset, hex: Color) {
            drawLine(color = hex.copy(alpha = 0.25f), start = p1, end = p2, strokeWidth = 26f * scaleFactor, cap = StrokeCap.Round)
            drawLine(color = Color.White, start = p1, end = p2, strokeWidth = 15f * scaleFactor, cap = StrokeCap.Round)
            drawLine(color = hex, start = p1, end = p2, strokeWidth = 10f * scaleFactor, cap = StrokeCap.Round)
            drawLine(color = Color.White.copy(alpha = 0.9f), start = p1, end = p2, strokeWidth = 3.4f * scaleFactor, cap = StrokeCap.Round, pathEffect = footprints)
        }

        for (i in 0 until fullSegs) {
            val p1 = cellCenter(path[i])
            val p2 = cellCenter(path[i + 1])
            val hex = colorTrail.getOrNull(i)?.toComposeColor() ?: EggTrail
            drawBeamSegment(p1, p2, hex)
        }

        var tipPos: Offset? = null
        if (fullSegs < maxSeg && frac > 0f) {
            val p1 = cellCenter(path[fullSegs])
            val p2 = cellCenter(path[fullSegs + 1])
            val tip = Offset(p1.x + (p2.x - p1.x) * frac, p1.y + (p2.y - p1.y) * frac)
            val hex = colorTrail.getOrNull(fullSegs)?.toComposeColor() ?: EggTrail
            drawBeamSegment(p1, tip, hex)
            tipPos = tip
        } else if (fullSegs == maxSeg) {
            tipPos = cellCenter(path[fullSegs])
        }

        for (i in 0..fullSegs) {
            if (path[i] in s.bends) {
                val bendColor = colorTrail.getOrNull((i - 1).coerceAtLeast(0))?.toComposeColor() ?: EggTrail
                val center = cellCenter(path[i])
                drawCircle(color = bendColor.copy(alpha = 0.32f), radius = 12f * scaleFactor, center = center)
                drawCircle(color = bendColor.copy(alpha = 0.6f), radius = 7.5f * scaleFactor, center = center)
                drawCircle(color = Color.White.copy(alpha = 0.95f), radius = 4f * scaleFactor, center = center)
            }
        }

        if (stillGrowing && tipPos != null) {
            val headColor = colorTrail.getOrNull(fullSegs)?.toComposeColor() ?: EggTrail
            drawCircle(color = headColor.copy(alpha = 0.28f), radius = 17f * scaleFactor, center = tipPos)
            drawCircle(color = headColor.copy(alpha = 0.85f), radius = 9f * scaleFactor, center = tipPos)
            drawCircle(color = Color.White, radius = 4f * scaleFactor, center = tipPos)

            // 先端から後方へ飛び散る火花(ロケット花火の軌道のような質感)。
            val segStart = cellCenter(path[fullSegs])
            val segEnd = cellCenter(path[fullSegs + 1])
            val segVec = segEnd - segStart
            val segLen = sqrt(segVec.x * segVec.x + segVec.y * segVec.y)
            if (segLen > 0.5f) {
                val dir = Offset(segVec.x / segLen, segVec.y / segLen)
                val perpDir = Offset(-dir.y, dir.x)
                val travelled = frac * segLen
                trailSparks.forEach { spark ->
                    val backDist = (spark.trailOffset * scaleFactor).coerceAtMost(travelled)
                    val gravityDrop = backDist * backDist * 0.0012f
                    val pos = tipPos - dir * backDist + perpDir * (spark.perp * scaleFactor) + Offset(0f, gravityDrop)
                    val twinkle = sin(frac * 8f + spark.twinklePhase).toFloat() * 0.5f + 0.5f
                    val fade = (1f - backDist / (16f * scaleFactor)).coerceIn(0f, 1f)
                    val alpha = fade * (0.35f + twinkle * 0.65f)
                    drawCircle(color = headColor.copy(alpha = alpha * 0.85f), radius = spark.size * scaleFactor, center = pos)
                    drawCircle(color = Color.White.copy(alpha = alpha * 0.75f), radius = spark.size * 0.5f * scaleFactor, center = pos)
                }
            }
        }

        val mf = muzzleFlash.value
        if (mf < 1f) {
            val originPos = cellCenter(path[0])
            val originColor = colorTrail.getOrNull(0)?.toComposeColor() ?: EggTrail
            drawCircle(
                color = originColor.copy(alpha = (1f - mf) * 0.35f),
                radius = (14f + mf * 36f) * scaleFactor,
                center = originPos,
            )
            drawCircle(
                color = originColor.copy(alpha = (1f - mf) * 0.85f),
                radius = (6f + mf * 22f) * scaleFactor,
                center = originPos,
            )
            drawCircle(
                color = Color.White.copy(alpha = (1f - mf)),
                radius = (3f + mf * 7f) * scaleFactor,
                center = originPos,
            )
        }

        val sw = shockwave.value
        if (sw < 1f) {
            val originPos = cellCenter(path[0])
            val originColor = colorTrail.getOrNull(0)?.toComposeColor() ?: EggTrail
            drawCircle(
                color = originColor.copy(alpha = (1f - sw) * 0.55f),
                radius = (10f + sw * 46f) * scaleFactor,
                center = originPos,
                style = Stroke(width = (3f * (1f - sw) + 0.6f) * scaleFactor),
            )
            val rayLen = (8f + sw * 30f) * scaleFactor
            val rayInner = 4f * scaleFactor
            val rayAlpha = (1f - sw) * 0.8f
            for (k in 0 until 8) {
                val ang = (Math.PI / 4 * k).toFloat()
                val dir = Offset(cos(ang), sin(ang))
                drawLine(
                    color = originColor.copy(alpha = rayAlpha),
                    start = originPos + dir * rayInner,
                    end = originPos + dir * rayLen,
                    strokeWidth = 2f * scaleFactor,
                    cap = StrokeCap.Round,
                )
            }
        }

        val hitPos = s.result.hit
        if (hitPos != null && !s.hitIsShooterClash) {
            val p = cellCenter(hitPos)
            val t = burstProgress.value
            val fp = flarePulse.value
            val burstColor = colorTrail.lastOrNull()?.toComposeColor() ?: EggTrail
            drawFireworkBurst(p, burstColor, scaleFactor, t, fp, fireworkRays)
        } else if (hitPos != null && s.hitIsShooterClash) {
            // 矢印同士の衝突: 命中はしているが狙い通りの接続ではないため、
            // 派手な虹色バーストではなく、くすんだ色味で控えめな「不発」寄りの見た目にする。
            val p = cellCenter(hitPos)
            val t = burstProgress.value
            val clashColor = Color(0xFFC97A3D)
            drawCircle(
                color = clashColor.copy(alpha = (1f - t * 1.3f).coerceIn(0f, 1f)),
                radius = (t * 28f * scaleFactor).coerceAtLeast(0f),
                center = p,
                style = Stroke(width = 3f * scaleFactor),
            )
            drawCircle(
                color = Color.White.copy(alpha = (0.35f - t * 0.6f).coerceIn(0f, 1f)),
                radius = (t * 15f * scaleFactor).coerceAtLeast(0f),
                center = p,
                style = Stroke(width = 2f * scaleFactor),
            )
            particles.take(10).forEach { particle ->
                val dist = particle.speed * 0.55f * t * scaleFactor
                drawCircle(
                    color = clashColor.copy(alpha = (1f - t * 1.2f).coerceIn(0f, 1f)),
                    radius = particle.size * 0.7f * scaleFactor,
                    center = p + Offset(cos(particle.angle) * dist, sin(particle.angle) * dist),
                )
            }
        } else if (missFizzle.value > 0f) {
            val p = cellCenter(path.last())
            val t = missFizzle.value
            drawCircle(
                color = Color(0xFF9691B8).copy(alpha = (1f - t) * 0.7f),
                radius = (6f + t * 16f) * scaleFactor,
                center = p,
                style = Stroke(width = 2f * scaleFactor),
            )
            particles.take(8).forEach { particle ->
                val dist = particle.speed * 0.45f * t * scaleFactor
                drawCircle(
                    color = Color.White.copy(alpha = (1f - t * 1.4f).coerceIn(0f, 1f)),
                    radius = particle.size * 0.7f * scaleFactor,
                    center = p + Offset(cos(particle.angle) * dist, sin(particle.angle) * dist),
                )
            }
        }
    }
}

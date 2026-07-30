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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.ui.theme.toComposeColor
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ParticleSpec(val angle: Float, val speed: Float, val size: Float)

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

        fun drawBeamSegment(p1: Offset, p2: Offset, hex: Color) {
            drawLine(color = hex.copy(alpha = 0.22f), start = p1, end = p2, strokeWidth = 30f * scaleFactor, cap = StrokeCap.Round)
            drawLine(color = hex.copy(alpha = 0.55f), start = p1, end = p2, strokeWidth = 16f * scaleFactor, cap = StrokeCap.Round)
            drawLine(color = Color.White.copy(alpha = 0.95f), start = p1, end = p2, strokeWidth = 5f * scaleFactor, cap = StrokeCap.Round)
            drawLine(color = Color.White, start = p1, end = p2, strokeWidth = 2.2f * scaleFactor, cap = StrokeCap.Round)
        }

        for (i in 0 until fullSegs) {
            val p1 = cellCenter(path[i])
            val p2 = cellCenter(path[i + 1])
            val hex = colorTrail.getOrNull(i)?.toComposeColor() ?: Color.White
            drawBeamSegment(p1, p2, hex)
        }

        var tipPos: Offset? = null
        if (fullSegs < maxSeg && frac > 0f) {
            val p1 = cellCenter(path[fullSegs])
            val p2 = cellCenter(path[fullSegs + 1])
            val tip = Offset(p1.x + (p2.x - p1.x) * frac, p1.y + (p2.y - p1.y) * frac)
            val hex = colorTrail.getOrNull(fullSegs)?.toComposeColor() ?: Color.White
            drawBeamSegment(p1, tip, hex)
            tipPos = tip
        } else if (fullSegs == maxSeg) {
            tipPos = cellCenter(path[fullSegs])
        }

        for (i in 0..fullSegs) {
            if (path[i] in s.bends) {
                val bendColor = colorTrail.getOrNull((i - 1).coerceAtLeast(0))?.toComposeColor() ?: Color.White
                val center = cellCenter(path[i])
                drawCircle(color = bendColor.copy(alpha = 0.32f), radius = 12f * scaleFactor, center = center)
                drawCircle(color = bendColor.copy(alpha = 0.6f), radius = 7.5f * scaleFactor, center = center)
                drawCircle(color = Color.White.copy(alpha = 0.95f), radius = 4f * scaleFactor, center = center)
            }
        }

        if (stillGrowing && tipPos != null) {
            val headColor = colorTrail.getOrNull(fullSegs)?.toComposeColor() ?: Color.White
            drawCircle(color = headColor.copy(alpha = 0.28f), radius = 17f * scaleFactor, center = tipPos)
            drawCircle(color = headColor.copy(alpha = 0.85f), radius = 9f * scaleFactor, center = tipPos)
            drawCircle(color = Color.White, radius = 4f * scaleFactor, center = tipPos)
        }

        val mf = muzzleFlash.value
        if (mf < 1f) {
            val originPos = cellCenter(path[0])
            val originColor = colorTrail.getOrNull(0)?.toComposeColor() ?: Color.White
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
            val originColor = colorTrail.getOrNull(0)?.toComposeColor() ?: Color.White
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
            val burstColor = colorTrail.lastOrNull()?.toComposeColor() ?: Color.White

            drawCircle(
                color = Color.White.copy(alpha = ((1f - fp) * 0.9f).coerceIn(0f, 1f)),
                radius = (10f + fp * 22f) * scaleFactor,
                center = p,
            )

            val flareLen = (6f + fp * 36f) * scaleFactor
            val flareAlpha = ((1f - fp) * 0.9f).coerceIn(0f, 1f)
            for (k in 0 until 4) {
                val ang = (Math.PI / 2 * k).toFloat() + (Math.PI / 4).toFloat()
                val dir = Offset(cos(ang), sin(ang))
                drawLine(
                    color = Color.White.copy(alpha = flareAlpha),
                    start = p - dir * flareLen,
                    end = p + dir * flareLen,
                    strokeWidth = 2.4f * scaleFactor,
                    cap = StrokeCap.Round,
                )
            }

            val ringSpecs = listOf(
                Triple(1.0f, 46f, burstColor),
                Triple(0.78f, 33f, Color.White),
                Triple(0.55f, 20f, burstColor),
            )
            ringSpecs.forEach { (speed, maxR, c) ->
                val rt = (t / speed).coerceIn(0f, 1f)
                drawCircle(
                    color = c.copy(alpha = ((1f - rt) * 0.75f).coerceIn(0f, 1f)),
                    radius = (rt * maxR * scaleFactor).coerceAtLeast(0f),
                    center = p,
                    style = Stroke(width = (3f * (1f - rt * 0.6f)) * scaleFactor),
                )
            }

            drawCircle(
                color = Color.White.copy(alpha = (1f - t * 2.2f).coerceIn(0f, 1f)),
                radius = (13f * (1f - t * 0.5f) * scaleFactor).coerceAtLeast(0f),
                center = p,
            )

            particles.forEach { particle ->
                val dist = particle.speed * t * scaleFactor
                val prevDist = (particle.speed * (t - 0.06f).coerceAtLeast(0f)) * scaleFactor
                val dir = Offset(cos(particle.angle), sin(particle.angle))
                val pos = p + dir * dist
                val prevPos = p + dir * prevDist
                val alpha = (1f - t * 1.1f).coerceIn(0f, 1f)
                drawLine(
                    color = burstColor.copy(alpha = alpha * 0.5f),
                    start = prevPos,
                    end = pos,
                    strokeWidth = (particle.size * 0.8f) * scaleFactor,
                    cap = StrokeCap.Round,
                )
                drawCircle(color = burstColor.copy(alpha = alpha * 0.35f), radius = particle.size * 2.2f * scaleFactor, center = pos)
                drawCircle(color = Color.White.copy(alpha = alpha), radius = particle.size * 0.6f * scaleFactor, center = pos)
                drawCircle(color = burstColor.copy(alpha = alpha), radius = particle.size * scaleFactor, center = pos)
            }
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

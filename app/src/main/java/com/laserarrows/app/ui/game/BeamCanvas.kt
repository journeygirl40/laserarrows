package com.laserarrows.app.ui.game

import androidx.compose.animation.core.Animatable
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
    val burstProgress = remember(shot?.token) { Animatable(0f) }
    val missFizzle = remember(shot?.token) { Animatable(0f) }
    val particles = remember(shot?.token) {
        val rnd = Random(shot?.token ?: 0L)
        List(20) {
            ParticleSpec(
                angle = rnd.nextFloat() * (2 * Math.PI).toFloat(),
                speed = 55f + rnd.nextFloat() * 85f,
                size = 1.5f + rnd.nextFloat() * 2.4f,
            )
        }
    }

    LaunchedEffect(shot?.token) {
        val s = shot ?: return@LaunchedEffect
        val path = s.result.path
        val segCount = (path.size - 1).coerceAtLeast(0)

        growth.snapTo(0f)
        muzzleFlash.snapTo(0f)
        launch { muzzleFlash.animateTo(1f, tween(170, easing = LinearEasing)) }

        if (segCount > 0) {
            growth.animateTo(segCount.toFloat(), tween(segCount * 36, easing = LinearEasing))
        }
        onRevealFinished()
        if (s.result.hit != null) {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(1f, tween(480, easing = LinearEasing))
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

        for (i in 0 until fullSegs) {
            val p1 = cellCenter(path[i])
            val p2 = cellCenter(path[i + 1])
            val hex = colorTrail.getOrNull(i)?.toComposeColor() ?: Color.White
            drawLine(color = hex.copy(alpha = 0.6f), start = p1, end = p2, strokeWidth = 16f * scaleFactor, cap = StrokeCap.Round)
            drawLine(color = Color.White, start = p1, end = p2, strokeWidth = 5f * scaleFactor, cap = StrokeCap.Round)
        }

        var tipPos: Offset? = null
        if (fullSegs < maxSeg && frac > 0f) {
            val p1 = cellCenter(path[fullSegs])
            val p2 = cellCenter(path[fullSegs + 1])
            val tip = Offset(p1.x + (p2.x - p1.x) * frac, p1.y + (p2.y - p1.y) * frac)
            val hex = colorTrail.getOrNull(fullSegs)?.toComposeColor() ?: Color.White
            drawLine(color = hex.copy(alpha = 0.6f), start = p1, end = tip, strokeWidth = 16f * scaleFactor, cap = StrokeCap.Round)
            drawLine(color = Color.White, start = p1, end = tip, strokeWidth = 5f * scaleFactor, cap = StrokeCap.Round)
            tipPos = tip
        } else if (fullSegs == maxSeg) {
            tipPos = cellCenter(path[fullSegs])
        }

        for (i in 0..fullSegs) {
            if (path[i] in s.bends) {
                drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 5.5f * scaleFactor, center = cellCenter(path[i]))
            }
        }

        if (stillGrowing && tipPos != null) {
            val headColor = colorTrail.getOrNull(fullSegs)?.toComposeColor() ?: Color.White
            drawCircle(color = headColor.copy(alpha = 0.85f), radius = 9f * scaleFactor, center = tipPos)
            drawCircle(color = Color.White, radius = 4f * scaleFactor, center = tipPos)
        }

        val mf = muzzleFlash.value
        if (mf < 1f) {
            val originPos = cellCenter(path[0])
            val originColor = colorTrail.getOrNull(0)?.toComposeColor() ?: Color.White
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

        val hitPos = s.result.hit
        if (hitPos != null && !s.hitIsShooterClash) {
            val p = cellCenter(hitPos)
            val t = burstProgress.value
            val burstColor = colorTrail.lastOrNull()?.toComposeColor() ?: Color.White
            drawCircle(
                color = burstColor.copy(alpha = (1f - t * 1.3f).coerceIn(0f, 1f)),
                radius = (t * 42f * scaleFactor).coerceAtLeast(0f),
                center = p,
                style = Stroke(width = 3f * scaleFactor),
            )
            drawCircle(
                color = Color.White.copy(alpha = (0.6f - t * 0.9f).coerceIn(0f, 1f)),
                radius = (t * 26f * scaleFactor).coerceAtLeast(0f),
                center = p,
                style = Stroke(width = 2f * scaleFactor),
            )
            drawCircle(
                color = Color.White.copy(alpha = (1f - t * 2.2f).coerceIn(0f, 1f)),
                radius = (13f * (1f - t * 0.5f) * scaleFactor).coerceAtLeast(0f),
                center = p,
            )
            particles.forEach { particle ->
                val dist = particle.speed * t * scaleFactor
                drawCircle(
                    color = burstColor.copy(alpha = (1f - t * 1.1f).coerceIn(0f, 1f)),
                    radius = particle.size * scaleFactor,
                    center = p + Offset(cos(particle.angle) * dist, sin(particle.angle) * dist),
                )
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

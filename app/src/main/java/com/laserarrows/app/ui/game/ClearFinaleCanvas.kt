package com.laserarrows.app.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.ui.theme.toComposeColor
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class FinaleBurstState(
    val center: Offset,
    val color: Color,
    val rays: List<FireworkRay>,
    val delayMs: Long,
) {
    val progress = Animatable(0f)
    val flare = Animatable(0f)
}

/**
 * レベルクリア時、そのステージ中に鳴らした花火の位置を全て一斉に打ち上げる祝賀演出。
 * ほぼ同時に発射しつつ、実際の花火大会の打ち上げのように数百ms以内でわずかにずらすことで
 * 単調な全同時発火よりも賑やかに見せる。
 */
@Composable
fun ClearFinaleCanvas(
    finale: ClearFinale?,
    cellSizePx: Float,
    gapPx: Float,
    modifier: Modifier = Modifier,
) {
    val stridePx = cellSizePx + gapPx
    val scaleFactor = cellSizePx / 60f

    val bursts = remember(finale?.token) {
        val list = finale?.bursts ?: emptyList()
        val rnd = Random(finale?.token ?: 0L)
        list.map { burst ->
            val center = Offset(
                burst.pos.col * stridePx + cellSizePx / 2f,
                burst.pos.row * stridePx + cellSizePx / 2f,
            )
            val seed = (finale?.token ?: 0L) xor (burst.pos.row * 6151L + burst.pos.col * 97L)
            FinaleBurstState(
                center = center,
                color = burst.color.toComposeColor(),
                rays = randomFireworkRays(seed),
                delayMs = (rnd.nextFloat() * 260).toLong(),
            )
        }
    }

    LaunchedEffect(finale?.token) {
        bursts.forEach { burst ->
            launch {
                delay(burst.delayMs)
                launch { burst.flare.animateTo(1f, tween(190, easing = EaseOutCubic)) }
                burst.progress.animateTo(1f, tween(620, easing = LinearEasing))
            }
        }
    }

    if (bursts.isEmpty()) return

    Canvas(modifier = modifier) {
        bursts.forEach { burst ->
            drawFireworkBurst(burst.center, burst.color, scaleFactor, burst.progress.value, burst.flare.value, burst.rays)
        }
    }
}

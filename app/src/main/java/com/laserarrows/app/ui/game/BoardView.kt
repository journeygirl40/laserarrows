package com.laserarrows.app.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.engine.Board
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.engine.Direction
import com.laserarrows.app.engine.Tile
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.toComposeColor

@Composable
fun BoardView(
    board: Board,
    cellSizeDp: Dp,
    shakeCell: CellPos?,
    onTapCell: (CellPos) -> Unit,
    modifier: Modifier = Modifier,
    emptyCellsTappable: Boolean = false,
    firingCell: CellPos? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        for (r in 0 until board.rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (c in 0 until board.cols) {
                    val pos = CellPos(r, c)
                    val tile = board.tiles[pos]
                    ArrowTile(
                        tile = tile,
                        cols = board.cols,
                        sizeDp = cellSizeDp,
                        shaking = pos == shakeCell,
                        emptyTappable = emptyCellsTappable,
                        firing = pos == firingCell,
                        onClick = { onTapCell(pos) },
                    )
                }
            }
        }
    }
}

private fun arrowGlyph(dir: Direction): String = when (dir) {
    Direction.U -> "↑"
    Direction.D -> "↓"
    Direction.L -> "←"
    Direction.R -> "→"
}

@Composable
private fun ArrowTile(
    tile: Tile?,
    cols: Int,
    sizeDp: Dp,
    shaking: Boolean,
    emptyTappable: Boolean,
    firing: Boolean,
    onClick: () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(shaking) {
        if (shaking) {
            offsetX.snapTo(0f)
            offsetX.animateTo(-4f, tween(75))
            offsetX.animateTo(4f, tween(150))
            offsetX.animateTo(0f, tween(75))
        }
    }

    val fireFlash = remember { Animatable(0f) }
    LaunchedEffect(firing) {
        if (firing) {
            fireFlash.snapTo(1f)
            fireFlash.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
        }
    }

    val shape = RoundedCornerShape(10.dp)
    var boxModifier = Modifier
        .size(sizeDp)
        .offset(x = offsetX.value.dp)

    if (tile != null) {
        val hex = if (firing) TextMuted else tile.color.toComposeColor()
        boxModifier = boxModifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.5.dp, hex, shape)
            .clickable(onClick = onClick)
    } else if (emptyTappable) {
        boxModifier = boxModifier
            .clip(shape)
            .background(GoldColor.copy(alpha = 0.08f))
            .border(1.5.dp, GoldColor.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick)
    }

    Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
        when (tile) {
            is Tile.Shooter -> {
                ShooterGlow(
                    color = tile.color.toComposeColor(),
                    flash = fireFlash.value,
                    active = !firing,
                    sizeDp = sizeDp,
                )
                Text(
                    text = arrowGlyph(tile.dir),
                    color = if (firing) TextMuted else tile.color.toComposeColor(),
                    fontWeight = FontWeight.Black,
                    fontSize = (28 - cols).coerceAtLeast(15).sp,
                )
            }
            is Tile.Receiver -> ReceiverOrb(color = tile.color.toComposeColor(), sizeDp = sizeDp)
            null -> if (emptyTappable) {
                Text("+", color = GoldColor.copy(alpha = 0.6f), fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ShooterGlow(color: Color, flash: Float, active: Boolean, sizeDp: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "shooterBreath")
    val breath by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shooterBreathAlpha",
    )
    // 単純な呼吸だけだと機械的なので、周期の違う揺らぎを重ねて火種のような不規則さを出す。
    val flicker by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(370, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shooterFlicker",
    )
    val baseAlpha = if (active) breath * flicker else 0.22f
    val glowAlpha = (baseAlpha * 0.4f + flash * 0.55f).coerceIn(0f, 1f)
    val glowScale = 1f + flash * 0.35f
    // 花火の光暈のように、外側のやわらかい広がりを1枚重ねてから芯の光を置く。
    Box(
        modifier = Modifier
            .size(sizeDp * 0.98f)
            .scale(glowScale)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(color.copy(alpha = glowAlpha * 0.32f), color.copy(alpha = 0f)))),
    )
    Box(
        modifier = Modifier
            .size(sizeDp * 0.68f)
            .scale(glowScale)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(color.copy(alpha = glowAlpha), color.copy(alpha = 0f)))),
    )
    // 芯の白熱コア(花火の光条が根元で白く熱くなるのと同じ質感を、発射前の火種にも持たせる)。
    val coreAlpha = if (active) (0.55f + flash * 0.45f).coerceAtMost(1f) * flicker else 0.3f
    Box(
        modifier = Modifier
            .size(sizeDp * (0.22f + flash * 0.1f))
            .scale(glowScale)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color.White.copy(alpha = coreAlpha), color.copy(alpha = coreAlpha * 0.7f), color.copy(alpha = 0f)))),
    )
    // 発射待ちの火種らしい、細かくきらめく十字の光条(受光側より速く・強めに明滅させて「これから発射する」energy差を出す)。
    val sparkleTransition = rememberInfiniteTransition(label = "shooterSparkle")
    val sparkleRotation by sparkleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(3400, easing = LinearEasing)),
        label = "shooterSparkleRotation",
    )
    val sparkleAlpha by sparkleTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shooterSparkleAlpha",
    )
    if (active) {
        Canvas(modifier = Modifier.size(sizeDp * 0.8f).rotate(sparkleRotation)) {
            val r = size.minDimension / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val inner = r * 0.45f
            val a = sparkleAlpha * flicker
            drawLine(Color.White.copy(alpha = a), Offset(cx, cy - r), Offset(cx, cy - inner), strokeWidth = 1.3f, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = a), Offset(cx, cy + inner), Offset(cx, cy + r), strokeWidth = 1.3f, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = a * 0.7f), Offset(cx - r, cy), Offset(cx - inner, cy), strokeWidth = 1.1f, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = a * 0.7f), Offset(cx + inner, cy), Offset(cx + r, cy), strokeWidth = 1.1f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun ReceiverOrb(color: Color, sizeDp: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbScale",
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbHalo",
    )
    // 打ち上がる前の花火玉のように、ゆっくり回るきらめきを重ねる(halo/coreとは別周期で回転・明滅させ、機械的に見えないようにする)。
    val sparkleTransition = rememberInfiniteTransition(label = "orbSparkle")
    val sparkleRotation by sparkleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(5200, easing = LinearEasing)),
        label = "orbSparkleRotation",
    )
    val sparkleAlpha by sparkleTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbSparkleAlpha",
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(sizeDp)) {
        Box(
            modifier = Modifier
                .size(sizeDp * 0.9f)
                .scale(scale)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(color.copy(alpha = haloAlpha), color.copy(alpha = 0f)))),
        )
        Box(
            modifier = Modifier
                .size(sizeDp * 0.52f)
                .scale(scale)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White, color))),
        )
        Canvas(modifier = Modifier.size(sizeDp * 0.86f).rotate(sparkleRotation)) {
            val r = size.minDimension / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val inner = r * 0.4f
            drawLine(Color.White.copy(alpha = sparkleAlpha), Offset(cx, cy - r), Offset(cx, cy - inner), strokeWidth = 1.4f, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = sparkleAlpha), Offset(cx, cy + inner), Offset(cx, cy + r), strokeWidth = 1.4f, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = sparkleAlpha * 0.7f), Offset(cx - r, cy), Offset(cx - inner, cy), strokeWidth = 1.2f, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = sparkleAlpha * 0.7f), Offset(cx + inner, cy), Offset(cx + r, cy), strokeWidth = 1.2f, cap = StrokeCap.Round)
        }
        Box(
            modifier = Modifier
                .size(sizeDp * 0.14f)
                .scale(scale)
                .offset(x = -(sizeDp * 0.12f), y = -(sizeDp * 0.12f))
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f)),
        )
    }
}

/** 盤面のミニプレビュー(お気に入り一覧などのカードで使う、極小サイズの縮小表示)。 */
@Composable
fun BoardPreview(board: Board, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (board.rows <= 0 || board.cols <= 0) return@Canvas
        val gap = size.minDimension * 0.03f
        val cellW = (size.width - gap * (board.cols - 1)) / board.cols
        val cellH = (size.height - gap * (board.rows - 1)) / board.rows
        val cell = minOf(cellW, cellH)
        val totalW = cell * board.cols + gap * (board.cols - 1)
        val totalH = cell * board.rows + gap * (board.rows - 1)
        val offsetX = (size.width - totalW) / 2f
        val offsetY = (size.height - totalH) / 2f
        val cornerRadius = CornerRadius(cell * 0.28f)

        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                val tile = board.tiles[CellPos(r, c)]
                val color = when (tile) {
                    is Tile.Shooter -> tile.color.toComposeColor()
                    is Tile.Receiver -> tile.color.toComposeColor()
                    null -> Color.White.copy(alpha = 0.06f)
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(offsetX + c * (cell + gap), offsetY + r * (cell + gap)),
                    size = Size(cell, cell),
                    cornerRadius = cornerRadius,
                )
            }
        }
    }
}

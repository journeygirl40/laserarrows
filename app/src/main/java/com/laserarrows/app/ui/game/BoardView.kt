package com.laserarrows.app.ui.game

import com.laserarrows.app.ui.components.GameText
import com.laserarrows.app.ui.theme.WoodDark
import com.laserarrows.app.ui.theme.EggTrail
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.laserarrows.app.R
import com.laserarrows.app.engine.Board
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.engine.Direction
import com.laserarrows.app.engine.Tile
import com.laserarrows.app.ui.components.ChunkySurface
import com.laserarrows.app.ui.theme.BoardGrassDark
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.animalRes
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
                    AnimalTile(
                        tile = board.tiles[pos],
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

@Composable
private fun AnimalTile(
    tile: Tile?,
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
    val shape = RoundedCornerShape(sizeDp * 0.26f)
    Box(modifier = Modifier.size(sizeDp).offset(x = offsetX.value.dp)) {
        when (tile) {
            is Tile.Shooter -> RunnerTile(tile, sizeDp, shape, firing, onClick)
            is Tile.Receiver -> WaitingTile(tile, sizeDp, shape, onClick)
            null -> EmptyCell(sizeDp, shape, emptyTappable, onClick)
        }
    }
}

/**
 * 走り出す子: 動物の色のぷっくりしたマスに3Dの顔。走る向きは、縁に付いた白い丸札の矢印で示す。
 * なぞのたまごは生成り色のマスに3Dのたまごと「?」を載せ、まだ何者でもないことを見せる。
 */
@Composable
private fun RunnerTile(tile: Tile.Shooter, sizeDp: Dp, shape: RoundedCornerShape, firing: Boolean, onClick: () -> Unit) {
    val color = if (tile.egg) EggTrail else tile.color.toComposeColor()
    ChunkySurface(
        face = color,
        edge = lerp(color, Color.Black, 0.28f),
        shape = shape,
        edgeHeight = sizeDp * 0.07f,
        onClick = onClick,
        modifier = Modifier.fillMaxSize().alpha(if (firing) 0.55f else 1f),
    ) {
        Image(
            painterResource(if (tile.egg) R.drawable.icon_egg else tile.color.animalRes()),
            contentDescription = null,
            modifier = Modifier.size(sizeDp * 0.6f),
        )
        if (tile.egg) {
            GameText(
                "?",
                fontSize = with(LocalDensity.current) { (sizeDp * 0.3f).toSp() },
                color = WoodDark,
                modifier = Modifier.align(Alignment.TopStart).offset(x = sizeDp * 0.1f, y = sizeDp * 0.02f),
            )
        }
        DirectionChip(tile.dir, color, sizeDp)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.DirectionChip(dir: Direction, color: Color, sizeDp: Dp) {
    // 小さな盤面(列が多い時)でも矢印が潰れないよう、丸札には最小の大きさを持たせる。
    val chip = (sizeDp * 0.32f).coerceAtLeast(17.dp)
    val align = when (dir) {
        Direction.U -> Alignment.TopCenter
        Direction.D -> Alignment.BottomCenter
        Direction.L -> Alignment.CenterStart
        Direction.R -> Alignment.CenterEnd
    }
    val nudge = sizeDp * 0.04f
    val (dx, dy) = when (dir) {
        Direction.U -> 0.dp to -nudge
        Direction.D -> 0.dp to nudge
        Direction.L -> -nudge to 0.dp
        Direction.R -> nudge to 0.dp
    }
    Box(
        modifier = Modifier
            .align(align)
            .offset(x = dx, y = dy)
            .size(chip)
            .shadow(3.dp, CircleShape)
            .background(Color.White, CircleShape)
            .border(1.5.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // 文字の矢印は小さいと上下が切れて読めなくなるため、線と三角で描く(どの大きさでもくっきり見える)。
        val ink = lerp(color, Color.Black, 0.25f)
        val angle = when (dir) {
            Direction.R -> 0f
            Direction.D -> 90f
            Direction.L -> 180f
            Direction.U -> 270f
        }
        Canvas(modifier = Modifier.fillMaxSize().padding(chip * 0.2f).rotate(angle)) {
            val w = size.width
            val h = size.height
            drawLine(ink, Offset(w * 0.08f, h / 2f), Offset(w * 0.6f, h / 2f), strokeWidth = h * 0.2f, cap = StrokeCap.Round)
            val head = Path().apply {
                moveTo(w * 0.98f, h / 2f)
                lineTo(w * 0.48f, h * 0.1f)
                lineTo(w * 0.48f, h * 0.9f)
                close()
            }
            drawPath(head, ink)
        }
    }
}

/** 相手を待っている子: 白いマスに動物の色の縁取り。顔がふわふわ揺れ、3Dのハートを浮かべている。 */
@Composable
private fun WaitingTile(tile: Tile.Receiver, sizeDp: Dp, shape: RoundedCornerShape, onClick: () -> Unit) {
    val color = tile.color.toComposeColor()
    val transition = rememberInfiniteTransition(label = "waiting")
    val bob by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "waitingBob",
    )
    ChunkySurface(
        face = lerp(color, Color.White, 0.82f),
        edge = lerp(color, Color.Black, 0.1f),
        shape = shape,
        edgeHeight = sizeDp * 0.07f,
        borderColor = color,
        onClick = onClick,
        modifier = Modifier.fillMaxSize(),
    ) {
        Image(
            painterResource(tile.color.animalRes()),
            contentDescription = null,
            modifier = Modifier.size(sizeDp * 0.56f).scale(bob),
        )
        Image(
            painterResource(R.drawable.icon_heart),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = sizeDp * 0.06f, y = -(sizeDp * 0.06f))
                .size(sizeDp * 0.34f)
                .scale(2.1f - bob),
        )
    }
}

/** 何もないマス: 芝生に少しくぼんだ跡。仲間を呼ぶ時だけ金色に光って押せるようになる。 */
@Composable
private fun EmptyCell(sizeDp: Dp, shape: RoundedCornerShape, tappable: Boolean, onClick: () -> Unit) {
    var m = Modifier
        .fillMaxSize()
        .padding(sizeDp * 0.08f)
        .background(if (tappable) Color.White.copy(alpha = 0.55f) else BoardGrassDark.copy(alpha = 0.7f), shape)
    if (tappable) m = m.border(1.5.dp, GoldColor.copy(alpha = 0.7f), shape).clickable(onClick = onClick)
    Box(modifier = m, contentAlignment = Alignment.Center) {
        if (tappable) Text("+", color = GoldColor.copy(alpha = 0.8f), fontWeight = FontWeight.Black)
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
                    is Tile.Shooter -> if (tile.egg) EggTrail else tile.color.toComposeColor()
                    is Tile.Receiver -> lerp(tile.color.toComposeColor(), Color.White, 0.6f)
                    null -> BoardGrassDark
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

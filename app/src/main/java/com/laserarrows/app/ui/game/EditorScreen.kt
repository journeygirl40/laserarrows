package com.laserarrows.app.ui.game

import com.laserarrows.app.ui.theme.animalRes
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.engine.Direction
import com.laserarrows.app.engine.LaserColor
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.components.GlassSurface
import com.laserarrows.app.ui.components.HomeIconButton
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.BoardGrass
import com.laserarrows.app.ui.theme.GlassBorder
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import com.laserarrows.app.ui.theme.toComposeColor
import androidx.compose.ui.graphics.Brush

@Composable
fun EditorScreen(
    editorState: EditorState,
    onBack: () -> Unit,
    onSetSize: (rows: Int, cols: Int) -> Unit,
    onSelectTool: (EditorTool) -> Unit,
    onSelectColor: (LaserColor) -> Unit,
    onSelectDirection: (Direction) -> Unit,
    onTapCell: (CellPos) -> Unit,
    onClearAll: () -> Unit,
    onTestPlay: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val board = editorState.board

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeIconButton(onClick = onBack)
            Text(
                "🛠 エディタ",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            SizeStepper(
                label = "行",
                value = board.rows,
                onDec = { onSetSize(board.rows - 1, board.cols) },
                onInc = { onSetSize(board.rows + 1, board.cols) },
            )
            SizeStepper(
                label = "列",
                value = board.cols,
                onDec = { onSetSize(board.rows, board.cols - 1) },
                onInc = { onSetSize(board.rows, board.cols + 1) },
            )
        }

        GlassSurface(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolChip(
                        "🧹 消す",
                        selected = editorState.tool == EditorTool.ERASER,
                        onClick = { onSelectTool(EditorTool.ERASER) },
                        modifier = Modifier.weight(1f),
                    )
                    ToolChip(
                        "🏃 走る子",
                        selected = editorState.tool == EditorTool.SHOOTER,
                        onClick = { onSelectTool(EditorTool.SHOOTER) },
                        modifier = Modifier.weight(1f),
                    )
                    ToolChip(
                        "💗 待つ子",
                        selected = editorState.tool == EditorTool.RECEIVER,
                        onClick = { onSelectTool(EditorTool.RECEIVER) },
                        modifier = Modifier.weight(1f),
                    )
                    ToolChip(
                        "🥚 たまご",
                        selected = editorState.tool == EditorTool.EGG,
                        onClick = { onSelectTool(EditorTool.EGG) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // たまごは種類を持たないので、動物選びは出さない。
                if (editorState.tool == EditorTool.SHOOTER || editorState.tool == EditorTool.RECEIVER) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LaserColor.entries.forEach { c ->
                            ColorSwatch(
                                color = c,
                                selected = editorState.color == c,
                                onClick = { onSelectColor(c) },
                            )
                        }
                    }
                }
                if (editorState.tool == EditorTool.SHOOTER || editorState.tool == EditorTool.EGG) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Direction.entries.forEach { d ->
                            DirChip(
                                dir = d,
                                selected = editorState.direction == d,
                                onClick = { onSelectDirection(d) },
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val gapDp = 5.dp
                val marginDp = 10.dp
                val availW = maxWidth - marginDp * 2
                val availH = maxHeight - marginDp * 2
                val cellFromW = (availW - gapDp * (board.cols - 1)) / board.cols
                val cellFromH = (availH - gapDp * (board.rows - 1)) / board.rows
                val cellSizeDp = if (cellFromW < cellFromH) cellFromW else cellFromH
                val boardWidthDp = cellSizeDp * board.cols + gapDp * (board.cols - 1)
                val boardHeightDp = cellSizeDp * board.rows + gapDp * (board.rows - 1)

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(boardWidthDp + marginDp * 2, boardHeightDp + marginDp * 2)
                        .background(BoardGrass, RoundedCornerShape(22.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    BoardView(
                        board = board,
                        cellSizeDp = cellSizeDp,
                        shakeCell = null,
                        onTapCell = onTapCell,
                        emptyCellsTappable = true,
                        modifier = Modifier.size(boardWidthDp, boardHeightDp),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            GhostButton("🗑 全消し", onClick = onClearAll, modifier = Modifier.weight(1f))
            NeonButton("▶ テスト", onClick = onTestPlay, modifier = Modifier.weight(1f))
        }
        GhostButton("🔗 共有", onClick = onShare, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SizeStepper(label: String, value: Int, onDec: () -> Unit, onInc: () -> Unit) {
    GlassSurface(shape = RoundedCornerShape(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
            Text(
                "－",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onDec).padding(6.dp),
            )
            Text("$label $value", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 2.dp))
            Text(
                "＋",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onInc).padding(6.dp),
            )
        }
    }
}

@Composable
private fun ToolChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Text(
        text,
        color = if (selected) GoldColor else TextPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(if (selected) GoldColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f), shape)
            .border(1.dp, if (selected) GoldColor else GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
    )
}

@Composable
private fun ColorSwatch(color: LaserColor, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.toComposeColor().copy(alpha = if (selected) 0.55f else 0.2f))
            .border(if (selected) 3.dp else 1.5.dp, if (selected) color.toComposeColor() else GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(color.animalRes()), contentDescription = null, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun DirChip(dir: Direction, selected: Boolean, onClick: () -> Unit) {
    val glyph = when (dir) {
        Direction.U -> "↑"
        Direction.D -> "↓"
        Direction.L -> "←"
        Direction.R -> "→"
    }
    val shape = RoundedCornerShape(8.dp)
    Text(
        glyph,
        color = if (selected) GoldColor else TextPrimary,
        fontWeight = FontWeight.Black,
        fontSize = 16.sp,
        modifier = Modifier
            .background(if (selected) GoldColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f), shape)
            .border(1.dp, if (selected) GoldColor else GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

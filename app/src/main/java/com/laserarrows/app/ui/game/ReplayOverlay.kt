package com.laserarrows.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.engine.ReplayEngine
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.Bg1
import com.laserarrows.app.ui.theme.Bg3
import com.laserarrows.app.ui.theme.GlassBorder
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.Neon2
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary

/** お気に入りのクリア記録を、麻雀の牌譜のように一手ずつ「戻る/進む」で振り返る画面。 */
@Composable
fun ReplayOverlay(
    replay: Overlay.Replay,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val frame = remember(replay.initialBoard, replay.steps, replay.stepIndex) {
        ReplayEngine.frameAt(replay.initialBoard, replay.steps, replay.stepIndex)
    }
    val totalSteps = replay.steps.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Bg1, Bg3))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text("📖 プレイログ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text(
                    replay.entry.memo.ifBlank { "(メモなし)" },
                    fontSize = 12.5.sp,
                    color = TextMuted,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("手 ${replay.stepIndex}/$totalSteps", Neon2)
                InfoChip("スコア ${frame.score}", GoldColor)
                if (frame.combo >= 2) InfoChip("コンボ ×${frame.combo}", GoldColor)
                if (frame.lightAddedAt != null) InfoChip("💡 光を設置", GoldColor)
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val board = frame.board
                    val hasBoard = board.cols > 0 && board.rows > 0
                    val gapDp = 5.dp
                    val marginDp = 10.dp
                    if (hasBoard) {
                        val availW = maxWidth - marginDp * 2
                        val availH = maxHeight - marginDp * 2
                        val cellFromW = (availW - gapDp * (board.cols - 1)) / board.cols
                        val cellFromH = (availH - gapDp * (board.rows - 1)) / board.rows
                        val cellSizeDp = if (cellFromW < cellFromH) cellFromW else cellFromH
                        val boardWidthDp = cellSizeDp * board.cols + gapDp * (board.cols - 1)
                        val boardHeightDp = cellSizeDp * board.rows + gapDp * (board.rows - 1)
                        val panelWidthDp = boardWidthDp + marginDp * 2
                        val panelHeightDp = boardHeightDp + marginDp * 2

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(panelWidthDp, panelHeightDp)
                                .background(Color(0x0EFFFFFF), RoundedCornerShape(22.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            BoardView(
                                board = board,
                                cellSizeDp = cellSizeDp,
                                shakeCell = null,
                                onTapCell = {},
                                modifier = Modifier.size(boardWidthDp, boardHeightDp),
                                firingCell = frame.firedFrom,
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                GhostButton(
                    "◀ 戻る",
                    onClick = onStepBack,
                    enabled = replay.stepIndex > 0,
                    modifier = Modifier.weight(1f),
                )
                NeonButton(
                    "進む ▶",
                    onClick = onStepForward,
                    enabled = replay.stepIndex < totalSteps,
                    modifier = Modifier.weight(1f),
                )
            }
            GhostButton("⭐ お気に入りへ戻る", onClick = onClose, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun InfoChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

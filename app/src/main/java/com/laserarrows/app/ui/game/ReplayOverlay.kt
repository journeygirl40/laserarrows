package com.laserarrows.app.ui.game

import androidx.compose.foundation.layout.PaddingValues
import com.laserarrows.app.ui.components.AdaptiveScreen
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.engine.ReplayEngine
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.BoardGrass
import com.laserarrows.app.ui.theme.GlassBorder
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.GoldText
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

    // 直前の手が発射なら、命中前(発射元・着弾先のタイルがまだ残っている)盤面から始めてビームを流し、
    // 着弾演出が終わったところで実際の盤面(タイルが消えた状態)に切り替える。今プレイ中と同じ見え方にするため。
    val isFireStep = replay.stepIndex > 0 &&
        replay.steps[replay.stepIndex - 1] is ReplayEngine.Step.Fire &&
        frame.shotResult != null
    val preShotBoard = remember(replay.initialBoard, replay.steps, replay.stepIndex) {
        if (isFireStep) ReplayEngine.frameAt(replay.initialBoard, replay.steps, replay.stepIndex - 1).board else null
    }
    var displayBoard by remember(replay.stepIndex) {
        mutableStateOf(if (isFireStep && preShotBoard != null) preShotBoard else frame.board)
    }
    var showBeam by remember(replay.stepIndex) { mutableStateOf(isFireStep) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
    ) {
        AdaptiveScreen(
            portraitPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            spacing = 12.dp,
            sideWidth = 320.dp,
            header = {
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
                    InfoChip("スコア ${frame.score}", GoldText)
                    if (frame.combo >= 2) InfoChip("コンボ ×${frame.combo}", GoldText)
                    if (frame.lightAddedAt != null) InfoChip("💡 光を設置", GoldText)
                }
            },
            footer = {
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
            },
        ) { boardModifier ->
            Box(
                modifier = boardModifier,
                contentAlignment = Alignment.Center,
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    // ゲーム画面と同じく、画面の向きに合わせて盤面を折り返して見せる([BoardOrientation])。
                    val flip = shouldFlipBoard(frame.board)
                    val board = frame.board.flippedIf(flip)
                    val hasBoard = board.cols > 0 && board.rows > 0
                    val gapDp = 5.dp
                    val marginDp = 10.dp
                    if (hasBoard) {
                        val availW = maxWidth - marginDp * 2
                        val availH = maxHeight - marginDp * 2
                        val cellFromW = (availW - gapDp * (board.cols - 1)) / board.cols
                        val cellFromH = (availH - gapDp * (board.rows - 1)) / board.rows
                        val cellSizeDp = (if (cellFromW < cellFromH) cellFromW else cellFromH).coerceAtLeast(0.dp) // 画面がとても小さい時にマイナスにならないように
                        val boardWidthDp = cellSizeDp * board.cols + gapDp * (board.cols - 1)
                        val boardHeightDp = cellSizeDp * board.rows + gapDp * (board.rows - 1)
                        val panelWidthDp = boardWidthDp + marginDp * 2
                        val panelHeightDp = boardHeightDp + marginDp * 2
                        val cellSizePx = with(LocalDensity.current) { cellSizeDp.toPx() }
                        val gapPx = with(LocalDensity.current) { gapDp.toPx() }

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(panelWidthDp, panelHeightDp)
                                .background(BoardGrass, RoundedCornerShape(22.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(modifier = Modifier.size(boardWidthDp, boardHeightDp)) {
                                BoardView(
                                    board = displayBoard.flippedIf(flip),
                                    cellSizeDp = cellSizeDp,
                                    shakeCell = null,
                                    onTapCell = {},
                                    firingCell = if (showBeam) frame.firedFrom?.flippedIf(flip) else null,
                                    meetingCell = if (showBeam && !frame.shotHitIsShooterClash) frame.shotResult?.hit?.flippedIf(flip) else null,
                                )
                                if (showBeam) {
                                    val shotResult = frame.shotResult
                                    val origin = frame.firedFrom
                                    if (shotResult != null && origin != null) {
                                        BeamCanvas(
                                            shot = ShotAnimation(
                                                originKey = origin,
                                                result = shotResult,
                                                bends = frame.shotBends,
                                                hitIsShooterClash = frame.shotHitIsShooterClash,
                                                token = replay.stepIndex.toLong(),
                                            ).flippedIf(flip),
                                            cellSizePx = cellSizePx,
                                            gapPx = gapPx,
                                            onRevealFinished = {},
                                            onSettled = {
                                                displayBoard = frame.board
                                                showBeam = false
                                            },
                                            modifier = Modifier.size(boardWidthDp, boardHeightDp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
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

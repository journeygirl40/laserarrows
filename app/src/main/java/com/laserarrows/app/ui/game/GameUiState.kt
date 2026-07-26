package com.laserarrows.app.ui.game

import com.laserarrows.app.data.ImportHistoryEntry
import com.laserarrows.app.engine.Board
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.engine.Direction
import com.laserarrows.app.engine.LaserColor
import com.laserarrows.app.engine.LaserResult
import com.laserarrows.app.engine.Tile

data class HistoryEntry(
    val removedAPos: CellPos,
    val removedATile: Tile,
    val removedBPos: CellPos,
    val removedBTile: Tile,
    val gained: Int,
    val wasReceiver: Boolean,
    val previousCombo: Int,
)

data class LevelSnapshot(val board: Board, val lives: Int, val score: Int, val combo: Int = 0)

data class ShotAnimation(
    val originKey: CellPos,
    val result: LaserResult,
    val bends: Set<CellPos>,
    /** 命中先が光る球(Receiver)ではなく別の矢印(Shooter)だった場合はtrue(狙い通りの接続ではない見た目にする)。 */
    val hitIsShooterClash: Boolean = false,
    val token: Long,
)

data class ToastMessage(val text: String, val isDanger: Boolean = false, val token: Long = 0L)

data class ComboPopup(val combo: Int, val token: Long = 0L)

/** プレイ中にハイスコアを初めて上回った瞬間に一度だけ発火する演出。 */
data class HighScorePopup(val score: Int, val token: Long = 0L)

/** プレイ中、これまでの最深到達レベルを初めて超えた瞬間に一度だけ発火する演出。 */
data class LevelRecordPopup(val level: Int, val token: Long = 0L)

enum class EditorTool { ERASER, SHOOTER, RECEIVER }

data class EditorState(
    val board: Board = Board(6, 6),
    val tool: EditorTool = EditorTool.SHOOTER,
    val color: LaserColor = LaserColor.RED,
    val direction: Direction = Direction.R,
)

data class GameUiState(
    val overlay: Overlay = Overlay.Home,
    val level: Int = 1,
    val score: Int = 0,
    val combo: Int = 0,
    val lives: Int = 5,
    val board: Board = Board(0, 0),
    val remainingReceivers: Int = 0,
    val busy: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
    val addingLight: Boolean = false,
    val pendingLightCell: CellPos? = null,
    val levelSnapshot: LevelSnapshot? = null,
    val activeShot: ShotAnimation? = null,
    val toast: ToastMessage? = null,
    val comboPopup: ComboPopup? = null,
    val highScorePopup: HighScorePopup? = null,
    val levelRecordPopup: LevelRecordPopup? = null,
    val crossedHighScoreThisRun: Boolean = false,
    val crossedLevelRecordThisRun: Boolean = false,
    val bestLevel: Int = 1,
    val bestScore: Int = 0,
    val bestLevelReached: Int = 1,
    val clearedLevels: Set<Int> = emptySet(),
    val levelBestLivesLost: Map<Int, Int> = emptyMap(),
    val levelBestScore: Map<Int, Int> = emptyMap(),
    val importHistory: List<ImportHistoryEntry> = emptyList(),
    val hasActiveRun: Boolean = false,
    val isImported: Boolean = false,
    val isEditorTest: Boolean = false,
    val editorState: EditorState = EditorState(),
) {
    val comboMultiplier: Double get() = 1.0 + combo * 0.1
}

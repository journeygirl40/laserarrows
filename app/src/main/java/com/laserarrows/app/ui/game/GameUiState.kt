package com.laserarrows.app.ui.game

import com.laserarrows.app.data.FavoriteSortKey
import com.laserarrows.app.data.ImportHistoryEntry
import com.laserarrows.app.engine.Board
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.engine.Direction
import com.laserarrows.app.engine.LaserColor
import com.laserarrows.app.engine.LaserResult
import com.laserarrows.app.engine.Tile

sealed class HistoryEntry {
    /** 発射して2つのタイルが消えた一手(命中/衝突どちらも含む)。 */
    data class Shot(
        val removedAPos: CellPos,
        val removedATile: Tile,
        val removedBPos: CellPos,
        val removedBTile: Tile,
        val gained: Int,
        val wasReceiver: Boolean,
        val previousCombo: Int,
    ) : HistoryEntry()

    /** ライフを消費して新しい光源(矢印)を盤面に設置した一手。 */
    data class LightAdded(
        val cell: CellPos,
        val tile: Tile.Shooter,
        val previousCombo: Int,
    ) : HistoryEntry()
}

data class LevelSnapshot(val board: Board, val lives: Int, val score: Int, val combo: Int = 0)

/** エディタの「テストプレイ」に入る前の本来のラン状態の退避先。テストを抜けたら復元し、テスト中の進行で上書きされないようにする。 */
data class SavedRun(
    val level: Int,
    val score: Int,
    val combo: Int,
    val lives: Int,
    val board: Board,
    val remainingReceivers: Int,
    val history: List<HistoryEntry>,
    val levelSnapshot: LevelSnapshot?,
    val isImported: Boolean,
    val activeFavoriteImportedAt: Long?,
    val bestScoreAtRunStart: Int,
    val crossedHighScoreThisRun: Boolean,
    val crossedLevelRecordThisRun: Boolean,
)

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

/** そのアクションで加算されたスコアを、スコアの上に一時的に表示する演出。 */
data class ScoreBonusPopup(val gained: Int, val token: Long = 0L)

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
    val scoreBonusPopup: ScoreBonusPopup? = null,
    val highScorePopup: HighScorePopup? = null,
    val levelRecordPopup: LevelRecordPopup? = null,
    val crossedHighScoreThisRun: Boolean = false,
    val crossedLevelRecordThisRun: Boolean = false,
    val bestLevel: Int = 1,
    val bestScore: Int = 0,
    /** このラン開始時点でのハイスコア(ラン中の差分表示は、途中で更新されていく[bestScore]ではなくこちらを基準にする)。 */
    val bestScoreAtRunStart: Int = 0,
    val bestLevelReached: Int = 1,
    val clearedLevels: Set<Int> = emptySet(),
    val levelBestLivesLost: Map<Int, Int> = emptyMap(),
    val levelBestScore: Map<Int, Int> = emptyMap(),
    val importHistory: List<ImportHistoryEntry> = emptyList(),
    val hasActiveRun: Boolean = false,
    /** レベル一覧を開いた時点で「パーフェクトを除外」フィルタを最初からONにするか(ホームの未パーフェクト件数バッジから遷移した場合)。 */
    val levelListExcludePerfect: Boolean = false,
    val levelListSortDescending: Boolean = true,
    val favoriteSortKey: FavoriteSortKey = FavoriteSortKey.DATE,
    val favoriteSortDescending: Boolean = true,
    val isImported: Boolean = false,
    /** 現在プレイ中の盤面が由来するお気に入り/インポート履歴エントリのID(importedAt)。クリア時にそのエントリの記録を更新するために使う。 */
    val activeFavoriteImportedAt: Long? = null,
    val isEditorTest: Boolean = false,
    /** テストプレイに入る前の本来のラン。テストを抜けたタイミングで復元する。 */
    val savedRun: SavedRun? = null,
    val editorState: EditorState = EditorState(),
)

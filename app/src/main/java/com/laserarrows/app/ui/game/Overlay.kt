package com.laserarrows.app.ui.game

import com.laserarrows.app.data.ImportHistoryEntry
import com.laserarrows.app.engine.Board
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.engine.LaserColor
import com.laserarrows.app.engine.ReplayEngine

sealed class Overlay {
    data object Home : Overlay()
    data object LevelList : Overlay()
    data object Import : Overlay()
    data object Favorites : Overlay()
    data object Editor : Overlay()
    data object None : Overlay()
    data class Clear(val totalScore: Int, val livesLost: Int) : Overlay()
    data class GameOver(val beatBest: Boolean) : Overlay()
    data class ColorPick(val cell: CellPos, val choices: List<LaserColor>) : Overlay()
    data class DirectionPick(val cell: CellPos, val color: LaserColor) : Overlay()
    /** お気に入りのクリア記録を一手ごとに振り返る画面。stepIndexは0(初期盤面)〜steps.size(全手適用後)。 */
    data class Replay(val entry: ImportHistoryEntry, val initialBoard: Board, val steps: List<ReplayEngine.Step>, val stepIndex: Int) : Overlay()
}

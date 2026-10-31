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
    data class Clear(
        val totalScore: Int,
        val livesLost: Int,
        val levelScore: Int = 0,
        val par: Int = 0,
        val stars: Int = 0,
        /** このステージで出会えたふたり組(出会った順)。クリア画面下のパレードに並べる。 */
        val couples: List<ClearCouple> = emptyList(),
        /** このクリアで森のなかまに加わった赤ちゃんの数(お気に入り・自作のテストでは0=森は育たない)。 */
        val populationGain: Int = 0,
        /** 広告を見てpopulationGainをもう一度もらった(=2倍にした)か。1回のクリアにつき1度だけ。 */
        val doubled: Boolean = false,
    ) : Overlay()
    data class GameOver(val beatBest: Boolean) : Overlay()
    data class ColorPick(val cell: CellPos, val choices: List<LaserColor>) : Overlay()
    data class DirectionPick(val cell: CellPos, val color: LaserColor) : Overlay()
    /** お気に入りのクリア記録を一手ごとに振り返る画面。stepIndexは0(初期盤面)〜steps.size(全手適用後)。 */
    data class Replay(val entry: ImportHistoryEntry, val initialBoard: Board, val steps: List<ReplayEngine.Step>, val stepIndex: Int) : Overlay()
}

/** 出会えたふたり組1組分。babyRunner=走ってきた子がたまご生まれの赤ちゃんだった。 */
data class ClearCouple(val color: LaserColor, val babyRunner: Boolean)

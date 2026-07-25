package com.laserarrows.app.ui.game

import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.engine.LaserColor

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
}

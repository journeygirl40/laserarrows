package com.laserarrows.app.engine

data class CellPos(val row: Int, val col: Int) {
    fun inBounds(rows: Int, cols: Int) = row in 0 until rows && col in 0 until cols
    operator fun plus(dir: Direction) = CellPos(row + dir.dr, col + dir.dc)
}

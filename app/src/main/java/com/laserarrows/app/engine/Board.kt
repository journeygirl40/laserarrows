package com.laserarrows.app.engine

data class Board(
    val rows: Int,
    val cols: Int,
    val tiles: Map<CellPos, Tile> = emptyMap(),
) {
    fun allCells(): List<CellPos> =
        (0 until rows).flatMap { r -> (0 until cols).map { c -> CellPos(r, c) } }

    fun emptyCells(): List<CellPos> = allCells().filter { it !in tiles }

    fun place(pos: CellPos, tile: Tile) = copy(tiles = tiles + (pos to tile))

    fun remove(pos: CellPos) = copy(tiles = tiles - pos)
}

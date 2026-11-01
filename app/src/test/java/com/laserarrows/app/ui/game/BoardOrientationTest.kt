package com.laserarrows.app.ui.game

import com.laserarrows.app.engine.Board
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.engine.Direction
import com.laserarrows.app.engine.LaserColor
import com.laserarrows.app.engine.LaserSimulator
import com.laserarrows.app.engine.PuzzleGenerator
import com.laserarrows.app.engine.Tile
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class BoardOrientationTest {

    @Test
    fun `2回折り返すと元に戻る`() {
        val board = Board(
            rows = 7,
            cols = 5,
            tiles = mapOf(
                CellPos(0, 1) to Tile.Shooter(Direction.D, LaserColor.RED),
                CellPos(6, 4) to Tile.Receiver(LaserColor.BLUE),
                CellPos(3, 2) to Tile.egg(Direction.L),
            ),
        )
        assertEquals(board, board.flipped().flipped())
        Direction.entries.forEach { assertEquals(it, it.flipped().flipped()) }
    }

    @Test
    fun `縦長の盤面は横長になり矢印の向きも折り返される`() {
        val board = Board(rows = 12, cols = 8, tiles = mapOf(CellPos(10, 3) to Tile.Shooter(Direction.U, LaserColor.GREEN)))
        val flipped = board.flipped()
        assertEquals(8, flipped.rows)
        assertEquals(12, flipped.cols)
        assertEquals(Tile.Shooter(Direction.L, LaserColor.GREEN), flipped.tiles[CellPos(3, 10)])
    }

    @Test
    fun `折り返した盤面でも同じ子が同じ道を走り同じ相手に出会う`() {
        // 実際のレベルの盤面で、すべての走る子について結果が「折り返した結果」と一致することを確かめる。
        for (level in listOf(1, 8, 17, 33, 48)) {
            val board = PuzzleGenerator.generateForLevel(level, Random(level.toLong())).board
            val flipped = board.flipped()
            board.tiles.filterValues { it is Tile.Shooter }.keys.forEach { start ->
                val original = LaserSimulator.fireLaser(board, start)
                val shown = LaserSimulator.fireLaser(flipped, start.flipped())
                assertEquals("Lv.$level $start", original.flipped(), shown)
            }
        }
    }
}

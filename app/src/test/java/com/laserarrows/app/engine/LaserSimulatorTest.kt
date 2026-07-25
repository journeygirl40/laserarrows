package com.laserarrows.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LaserSimulatorTest {

    @Test
    fun `直進して同色に命中する`() {
        val board = Board(
            rows = 5,
            cols = 5,
            tiles = mapOf(
                CellPos(2, 0) to Tile.Shooter(Direction.R, LaserColor.RED),
                CellPos(2, 4) to Tile.Receiver(LaserColor.RED),
            ),
        )
        val result = LaserSimulator.fireLaser(board, CellPos(2, 0))
        assertEquals(CellPos(2, 4), result.hit)
        assertEquals(listOf(LaserColor.RED, LaserColor.RED, LaserColor.RED, LaserColor.RED), result.colorTrail)
    }

    @Test
    fun `違う色の矢印に当たって曲がり色が変わって命中する`() {
        // (0,0)からRED,R方向 -> (0,2)のBLUE,D矢印に当たりBLUE,Dに変化 -> (3,2)のBLUE受信球に命中
        val board = Board(
            rows = 5,
            cols = 5,
            tiles = mapOf(
                CellPos(0, 0) to Tile.Shooter(Direction.R, LaserColor.RED),
                CellPos(0, 2) to Tile.Shooter(Direction.D, LaserColor.BLUE),
                CellPos(3, 2) to Tile.Receiver(LaserColor.BLUE),
            ),
        )
        val result = LaserSimulator.fireLaser(board, CellPos(0, 0))
        assertEquals(CellPos(3, 2), result.hit)
        assertEquals(
            listOf(LaserColor.RED, LaserColor.RED, LaserColor.BLUE, LaserColor.BLUE, LaserColor.BLUE),
            result.colorTrail,
        )
    }

    @Test
    fun `色違いのreceiverに当たるとブロックされて失敗する`() {
        val board = Board(
            rows = 5,
            cols = 5,
            tiles = mapOf(
                CellPos(0, 0) to Tile.Shooter(Direction.R, LaserColor.RED),
                CellPos(0, 3) to Tile.Receiver(LaserColor.BLUE),
            ),
        )
        val result = LaserSimulator.fireLaser(board, CellPos(0, 0))
        assertNull(result.hit)
    }

    @Test
    fun `盤外に出ると失敗する`() {
        val board = Board(
            rows = 5,
            cols = 5,
            tiles = mapOf(
                CellPos(0, 0) to Tile.Shooter(Direction.U, LaserColor.RED),
            ),
        )
        val result = LaserSimulator.fireLaser(board, CellPos(0, 0))
        assertNull(result.hit)
    }

    @Test
    fun `同色ならshooterでも命中扱いになる(色一致がrole判定より優先)`() {
        val board = Board(
            rows = 5,
            cols = 5,
            tiles = mapOf(
                CellPos(0, 0) to Tile.Shooter(Direction.R, LaserColor.RED),
                CellPos(0, 3) to Tile.Shooter(Direction.D, LaserColor.RED),
            ),
        )
        val result = LaserSimulator.fireLaser(board, CellPos(0, 0))
        assertEquals(CellPos(0, 3), result.hit)
    }

    @Test
    fun `receiverタイルを起点にしても発射できない`() {
        val board = Board(
            rows = 3,
            cols = 3,
            tiles = mapOf(CellPos(0, 0) to Tile.Receiver(LaserColor.RED)),
        )
        val result = LaserSimulator.fireLaser(board, CellPos(0, 0))
        assertNull(result.hit)
        assertEquals(listOf(CellPos(0, 0)), result.path)
    }
}

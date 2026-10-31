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
    fun `違う種類の矢印に当たると向きだけ変わり種類はそのままで同じ種類に出会う`() {
        // (0,0)からRED,R方向 -> (0,2)のBLUE,D矢印で下向きに曲がる(REDのまま) -> (3,2)のRED受信に命中
        val board = Board(
            rows = 5,
            cols = 5,
            tiles = mapOf(
                CellPos(0, 0) to Tile.Shooter(Direction.R, LaserColor.RED),
                CellPos(0, 2) to Tile.Shooter(Direction.D, LaserColor.BLUE),
                CellPos(3, 2) to Tile.Receiver(LaserColor.RED),
            ),
        )
        val result = LaserSimulator.fireLaser(board, CellPos(0, 0))
        assertEquals(CellPos(3, 2), result.hit)
        assertEquals(List(5) { LaserColor.RED }, result.colorTrail)
        assertEquals(1, result.redirects)
    }

    @Test
    fun `曲がった先が違う種類の受信なら止まる`() {
        val board = Board(
            rows = 5,
            cols = 5,
            tiles = mapOf(
                CellPos(0, 0) to Tile.Shooter(Direction.R, LaserColor.RED),
                CellPos(0, 2) to Tile.Shooter(Direction.D, LaserColor.BLUE),
                CellPos(3, 2) to Tile.Receiver(LaserColor.BLUE),
            ),
        )
        assertNull(LaserSimulator.fireLaser(board, CellPos(0, 0)).hit)
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

    @Test
    fun `なぞのたまごはぶつかった動物に変わって曲がり、その動物の相手に出会う`() {
        // たまご(→) -> キツネ(↓)にぶつかりキツネの赤ちゃんに -> 下のキツネに出会う
        val board = Board(5, 5)
            .place(CellPos(0, 0), Tile.egg(Direction.R))
            .place(CellPos(0, 2), Tile.Shooter(Direction.D, LaserColor.RED))
            .place(CellPos(3, 2), Tile.Receiver(LaserColor.RED))
        val result = LaserSimulator.fireLaser(board, CellPos(0, 0))
        assertEquals(CellPos(3, 2), result.hit)
        assertEquals(listOf(null, null, LaserColor.RED, LaserColor.RED, LaserColor.RED), result.colorTrail)
    }

    @Test
    fun `なぞのたまごは曲がるたびに化け直し、最後に化けた動物で出会いが決まる`() {
        // たまご(→) -> キツネ(↓)でキツネに -> ペンギン(→)でペンギンに -> 右のペンギンに出会う(途中のキツネには止められる手前で曲がる)
        val board = Board(5, 5)
            .place(CellPos(0, 0), Tile.egg(Direction.R))
            .place(CellPos(0, 2), Tile.Shooter(Direction.D, LaserColor.RED))
            .place(CellPos(2, 2), Tile.Shooter(Direction.R, LaserColor.BLUE))
            .place(CellPos(2, 4), Tile.Receiver(LaserColor.BLUE))
        val result = LaserSimulator.fireLaser(board, CellPos(0, 0))
        assertEquals(CellPos(2, 4), result.hit)
        assertEquals(2, result.redirects)
    }

    @Test
    fun `まだ何にも化けていないたまごは誰とも出会えない`() {
        val board = Board(5, 5)
            .place(CellPos(0, 0), Tile.egg(Direction.R))
            .place(CellPos(0, 3), Tile.Receiver(LaserColor.RED))
        assertNull(LaserSimulator.fireLaser(board, CellPos(0, 0)).hit)
    }

    @Test
    fun `走る子がたまごに当たると向きだけ変わり、種類は変わらない`() {
        val board = Board(5, 5)
            .place(CellPos(0, 0), Tile.Shooter(Direction.R, LaserColor.BLUE))
            .place(CellPos(0, 2), Tile.egg(Direction.D))
            .place(CellPos(3, 2), Tile.Receiver(LaserColor.BLUE))
        val result = LaserSimulator.fireLaser(board, CellPos(0, 0))
        assertEquals(CellPos(3, 2), result.hit)
        assertEquals(1, result.redirects)
    }

    @Test
    fun `たまごを含む盤面も共有コードで往復できる`() {
        val board = Board(4, 4)
            .place(CellPos(0, 0), Tile.egg(Direction.R))
            .place(CellPos(1, 2), Tile.Shooter(Direction.D, LaserColor.GREEN))
            .place(CellPos(3, 2), Tile.Receiver(LaserColor.GREEN))
        assertEquals(board, BoardCode.decode(BoardCode.encode(board)))
    }
}

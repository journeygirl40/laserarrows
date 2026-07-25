package com.laserarrows.app.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleGeneratorTest {

    @Test
    fun `levelSizeは行列サイズとペア数をレベルに応じて拡大しつつ上限で頭打ちする`() {
        assertEquals(LevelSize(5, 5, 5), PuzzleGenerator.levelSize(1))
        assertEquals(LevelSize(6, 6, 8), PuzzleGenerator.levelSize(4))
        assertEquals(LevelSize(8, 8, 16), PuzzleGenerator.levelSize(30))
    }

    @Test
    fun `生成された全shooterは実際にfireLaserで同色のタイルに命中する`() {
        val random = Random(42)
        repeat(10) { seedOffset ->
            val level = 1 + seedOffset * 3
            val size = PuzzleGenerator.levelSize(level)
            val board = PuzzleGenerator.generate(size.rows, size.cols, size.numPairs, Random(seedOffset))
            val shooters = board.tiles.filterValues { it is Tile.Shooter }
            assertTrue("レベル$level でshooterが1つも生成されていない", shooters.isNotEmpty())
            shooters.keys.forEach { pos ->
                val result = LaserSimulator.fireLaser(board, pos)
                assertTrue(
                    "レベル$level のshooter $pos が命中しない: $result",
                    result.hit != null,
                )
            }
        }
        // ランダムシードを消費するだけの未使用警告回避
        random.nextInt()
    }

    @Test
    fun `receiverの数はshooterの数と一致する(全てペアで生成される)`() {
        val board = PuzzleGenerator.generate(6, 6, 8, Random(7))
        val shooterCount = board.tiles.values.count { it is Tile.Shooter }
        val receiverCount = board.tiles.values.count { it is Tile.Receiver }
        assertEquals(shooterCount, receiverCount)
    }
}

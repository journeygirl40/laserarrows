package com.laserarrows.app.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleGeneratorTest {

    @Test
    fun `levelSizeの代表的なレベルの盤面サイズとペア数`() {
        assertEquals(LevelSize(5, 5, 4), PuzzleGenerator.levelSize(1))
        assertEquals(LevelSize(5, 5, 6), PuzzleGenerator.levelSize(5))
        assertEquals(LevelSize(6, 5, 6), PuzzleGenerator.levelSize(6))
        assertEquals(LevelSize(8, 7, 14), PuzzleGenerator.levelSize(30))
        assertEquals(LevelSize(12, 8, 24), PuzzleGenerator.levelSize(100))
    }

    @Test
    fun `盤面の大きさは5レベルごとに1段階だけ変わる`() {
        for (bracket in 0 until 12) {
            val first = PuzzleGenerator.levelSize(bracket * 5 + 1)
            for (level in bracket * 5 + 2..bracket * 5 + 5) {
                val size = PuzzleGenerator.levelSize(level)
                assertEquals("Lv.$level", first.rows to first.cols, size.rows to size.cols)
            }
        }
    }

    @Test
    fun `ペア数はレベルが上がっても減らず、マス数の4分の1を超えない`() {
        var previous = 0
        for (level in 1..120) {
            val size = PuzzleGenerator.levelSize(level)
            assertTrue("Lv.$level でペア数が減った", size.numPairs >= previous)
            assertTrue("Lv.$level でペアが多すぎる", size.numPairs <= size.rows * size.cols / 4)
            assertTrue("Lv.$level でペアが少なすぎる", size.numPairs >= 3)
            previous = size.numPairs
        }
    }

    @Test
    fun `盤面は横8列・縦12行で頭打ちになる`() {
        val sizes = (1..200).map { PuzzleGenerator.levelSize(it) }
        assertEquals(8, sizes.maxOf { it.cols })
        assertEquals(12, sizes.maxOf { it.rows })
    }

    @Test
    fun `生成時の撃ち順どおりに撃てば必ず全消しできる`() {
        repeat(30) { seed ->
            val level = 1 + seed * 2
            val size = PuzzleGenerator.levelSize(level)
            val generated = PuzzleGenerator.generateWithSolution(size.rows, size.cols, size.numPairs, Random(seed))
            var board = generated.board
            generated.solution.forEach { origin ->
                val result = LaserSimulator.fireLaser(board, origin)
                val hit = result.hit
                assertTrue("レベル$level: $origin が届かない", hit != null && board.tiles[hit] is Tile.Receiver)
                board = board.remove(origin).remove(hit!!)
            }
            assertTrue("レベル$level: 受信が残った", board.tiles.isEmpty())
        }
    }

    @Test
    fun `道案内のペアも生成される`() {
        val size = PuzzleGenerator.levelSize(20)
        val generated = PuzzleGenerator.generateWithSolution(size.rows, size.cols, size.numPairs, Random(3))
        var board = generated.board
        var redirects = 0
        generated.solution.forEach { origin ->
            val result = LaserSimulator.fireLaser(board, origin)
            redirects += result.redirects
            board = board.remove(origin).remove(result.hit!!)
        }
        assertTrue("道案内が1回も無い", redirects > 0)
    }

    @Test
    fun `receiverの数はshooterの数と一致する(全てペアで生成される)`() {
        val board = PuzzleGenerator.generate(6, 6, 8, Random(7))
        val shooterCount = board.tiles.values.count { it is Tile.Shooter }
        val receiverCount = board.tiles.values.count { it is Tile.Receiver }
        assertEquals(shooterCount, receiverCount)
    }

    @Test
    fun `同じシードなら同じ盤面が生成される`() {
        val size = PuzzleGenerator.levelSize(12)
        val a = PuzzleGenerator.generate(size.rows, size.cols, size.numPairs, Random(PuzzleGenerator.seedForLevel(12)))
        val b = PuzzleGenerator.generate(size.rows, size.cols, size.numPairs, Random(PuzzleGenerator.seedForLevel(12)))
        assertEquals(a, b)
    }

    private fun hasEgg(board: Board) = board.tiles.values.any { it is Tile.Shooter && it.egg }

    @Test
    fun `なぞのたまごはLv10までは出ず、Lv11以降で出てくる`() {
        for (level in 1 until PuzzleGenerator.EGG_FIRST_LEVEL) {
            assertTrue("Lv.$level にたまごが出た", !hasEgg(PuzzleGenerator.generateForLevel(level).board))
        }
        val withEgg = (PuzzleGenerator.EGG_FIRST_LEVEL..PuzzleGenerator.EGG_FIRST_LEVEL + 19)
            .count { hasEgg(PuzzleGenerator.generateForLevel(it).board) }
        assertTrue("Lv.11〜30でたまごが出たのは$withEgg レベルだけ", withEgg >= 10)
    }

    @Test
    fun `たまご入りの盤面も、生成時の撃ち順どおりに撃てば必ず全消しできる`() {
        repeat(30) { seed ->
            val size = PuzzleGenerator.levelSize(11 + seed)
            val generated = PuzzleGenerator.generateWithSolution(size.rows, size.cols, size.numPairs, Random(seed), eggChance = 0.4)
            var board = generated.board
            generated.solution.forEach { origin ->
                val result = LaserSimulator.fireLaser(board, origin)
                val hit = result.hit
                assertTrue("seed=$seed: $origin が届かない", hit != null && board.tiles[hit] is Tile.Receiver)
                board = board.remove(origin).remove(hit!!)
            }
            assertTrue("seed=$seed: 受信が残った", board.tiles.isEmpty())
        }
    }
}

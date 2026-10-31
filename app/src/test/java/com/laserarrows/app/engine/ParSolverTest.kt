package com.laserarrows.app.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParSolverTest {

    // S(青→右)が O(赤↓)で下に曲がり、青のまま R(青)に出会う。O は R にふさがれていて、S が先でないと届かない。
    // S を先に撃てば道案内1回(3点)+O が奥の赤へ(1点)で4点。
    private val relayBoard = Board(5, 5)
        .place(CellPos(0, 0), Tile.Shooter(Direction.R, LaserColor.BLUE))
        .place(CellPos(0, 2), Tile.Shooter(Direction.D, LaserColor.RED))
        .place(CellPos(2, 2), Tile.Receiver(LaserColor.BLUE))
        .place(CellPos(4, 2), Tile.Receiver(LaserColor.RED))

    @Test
    fun `中継を先に撃つ順番がパーになる`() {
        val result = ParSolver.solve(relayBoard)
        assertEquals(4, result.par)
        assertFalse(result.approximate)
    }

    @Test
    fun `開始コンボがあるとパーもその分伸びる`() {
        // コンボ5から: round(3*1.6)=5, round(1*1.7)=2
        assertEquals(7, ParSolver.solve(relayBoard, startCombo = 5).par)
    }

    @Test
    fun `ノード上限を超えると貪欲法の近似値を返す`() {
        val result = ParSolver.solve(relayBoard, nodeBudget = 0)
        assertTrue(result.approximate)
        assertEquals(4, result.par)
    }

    @Test
    fun `撃てるけれど詰む手を迷いどころとして数える`() {
        // 青A(→)も青B(↑)も手前の青R1に出会える。Aが先に取るとBは行き場を失う。Bが先ならAは奥のR2へ届く。
        val board = Board(5, 5)
            .place(CellPos(0, 0), Tile.Shooter(Direction.R, LaserColor.BLUE))
            .place(CellPos(3, 3), Tile.Shooter(Direction.U, LaserColor.BLUE))
            .place(CellPos(0, 3), Tile.Receiver(LaserColor.BLUE))
            .place(CellPos(0, 4), Tile.Receiver(LaserColor.BLUE))
        val analysis = ParSolver.analyze(board)
        assertTrue(analysis.clearable)
        assertEquals(1, analysis.traps)
        assertEquals(2, analysis.par)
    }

    @Test
    fun `パーは全消しできる手順の点で、途中で詰む手順の点は数えない`() {
        // 実機で、パーに到達したのに受信機が残って詰む盤面があった(固定シードのLv.3)。
        for (level in 1..20) {
            var board = PuzzleGenerator.generateForLevel(level, kotlin.random.Random(level.toLong())).board
            val par = ParSolver.solve(board)
            if (par.approximate) continue
            var combo = 0
            var remaining = par.par
            while (board.tiles.values.any { it is Tile.Receiver }) {
                val next = board.tiles.filterValues { it is Tile.Shooter }.keys.firstNotNullOfOrNull { origin ->
                    val result = LaserSimulator.fireLaser(board, origin)
                    val hit = result.hit ?: return@firstNotNullOfOrNull null
                    val isReceiver = board.tiles[hit] is Tile.Receiver
                    val nextCombo = if (isReceiver) combo + 1 else 0
                    val gained = if (isReceiver) ScoreRules.shotScore(nextCombo, ScoreRules.relayCount(result)) else 0
                    val nextBoard = board.remove(origin).remove(hit)
                    val rest = if (nextBoard.tiles.values.any { it is Tile.Receiver }) ParSolver.solve(nextBoard, nextCombo).par else 0
                    if (gained + rest == remaining) Triple(nextBoard, nextCombo, gained) else null
                }
                assertTrue("Lv.$level: パーの手順を辿ると受信機が残って詰む", next != null)
                board = next!!.first
                combo = next.second
                remaining -= next.third
            }
            assertEquals("Lv.$level", 0, remaining)
        }
    }

    @Test
    fun `生成盤面で解き順により点差が出る割合を計測する`() {
        var differs = 0
        var withTraps = 0
        val total = 50
        val started = System.currentTimeMillis()
        for (level in 1..total) {
            val t0 = System.currentTimeMillis()
            val board = PuzzleGenerator.generateForLevel(level, kotlin.random.Random(level.toLong())).board
            val genMs = System.currentTimeMillis() - t0
            val analysis = ParSolver.analyze(board)
            if (analysis.traps > 0) withTraps++
            // 何も考えずに左上から順に「出会える子」を撃っていった場合の点と比べ、パーの方が高ければ「解き順が点に効く」盤面。
            val naive = naiveScore(board)
            if (analysis.par > naive) differs++
            println("Lv.$level par=${analysis.par} naive=$naive traps=${analysis.traps}/${PuzzleGenerator.targetTraps(level)} gen=${genMs}ms")
        }
        println("解き順が点に効く盤面: $differs / $total, 迷いどころあり: $withTraps / $total, 合計${System.currentTimeMillis() - started}ms")
        assertTrue(withTraps > total * 8 / 10)
    }

    private fun naiveScore(start: Board): Int {
        var board = start
        var combo = 0
        var total = 0
        while (true) {
            val origin = board.tiles.filterValues { it is Tile.Shooter }.keys
                .sortedWith(compareBy({ it.row }, { it.col }))
                .firstOrNull { pos -> LaserSimulator.fireLaser(board, pos).hit?.let { board.tiles[it] is Tile.Receiver } == true }
                ?: return total
            val result = LaserSimulator.fireLaser(board, origin)
            combo++
            total += ScoreRules.shotScore(combo, ScoreRules.relayCount(result))
            board = board.remove(origin).remove(result.hit!!)
        }
    }
}

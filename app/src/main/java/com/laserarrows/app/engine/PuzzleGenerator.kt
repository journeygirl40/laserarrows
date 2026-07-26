package com.laserarrows.app.engine

import kotlin.random.Random

/**
 * レベル生成: 各候補配置は実際にLaserSimulator.fireLaserでシミュレートし、
 * 意図した接続が成立することを確認してから確定する(色の偶然一致や経路干渉を防ぐ)。
 */
object PuzzleGenerator {
    // 5レベルごとに1段階だけ盤面サイズが変わる(横は8で頭打ち、縦は12まで伸びる)。
    private val ROWS_BY_BRACKET = listOf(5, 6, 6, 7, 7, 8, 8, 9, 10, 11, 12)
    private val COLS_BY_BRACKET = listOf(5, 5, 6, 6, 7, 7, 8, 8, 8, 8, 8)

    private fun capFor(bracket: Int): Int {
        val b = bracket.coerceAtLeast(0)
        val rows = ROWS_BY_BRACKET.getOrElse(b) { ROWS_BY_BRACKET.last() }
        val cols = COLS_BY_BRACKET.getOrElse(b) { COLS_BY_BRACKET.last() }
        // 盤面の面積の半分(=ペア×2マス)を上限にする。
        return (rows * cols) / 4
    }

    fun levelSize(level: Int): LevelSize {
        val bracket = (level - 1) / 5
        val rows = ROWS_BY_BRACKET.getOrElse(bracket) { ROWS_BY_BRACKET.last() }
        val cols = COLS_BY_BRACKET.getOrElse(bracket) { COLS_BY_BRACKET.last() }
        val cap = capFor(bracket)
        val prevCap = if (bracket == 0) 3 else capFor(bracket - 1)
        val withinIndex = (level - 1) % 5
        // 盤面が固定の5レベルの間、ペア数は前段階の上限から今の上限まで徐々に増えていく。
        val numPairs = (prevCap + (cap - prevCap) * (withinIndex + 1) / 5.0)
            .let { Math.round(it).toInt() }
            .coerceIn(3, cap)
        return LevelSize(rows, cols, numPairs)
    }

    /** レベル生成では使わない、逆引き専用の上限(この段階を超えるとペア数上限が頭打ちになるため)。 */
    private const val MAX_ESTIMATE_LEVEL = 200

    /**
     * お気に入り・インポートした盤面など「実際に生成されたレベル」が分からない盤面について、
     * ペア数からおおよそ何レベル相当の難易度かを逆算する(一覧・HUD表示用)。
     */
    fun estimateLevelForPairCount(numPairs: Int): Int {
        var level = 1
        while (levelSize(level).numPairs < numPairs && level < MAX_ESTIMATE_LEVEL) {
            level++
        }
        return level
    }

    fun estimateLevelForBoard(board: Board): Int =
        estimateLevelForPairCount(board.tiles.values.count { it is Tile.Receiver })

    private fun directionBetween(a: CellPos, b: CellPos): Direction? = when {
        a.row == b.row && a.col != b.col -> if (b.col > a.col) Direction.R else Direction.L
        a.col == b.col && a.row != b.row -> if (b.row > a.row) Direction.D else Direction.U
        else -> null
    }

    private fun rayFrom(pos: CellPos, dir: Direction, rows: Int, cols: Int): List<CellPos> {
        val cells = mutableListOf<CellPos>()
        var cur = pos + dir
        while (cur.inBounds(rows, cols)) {
            cells += cur
            cur += dir
        }
        return cells
    }

    fun generate(rows: Int, cols: Int, numPairs: Int, random: Random = Random): Board {
        var board = Board(rows, cols)
        val colors = LaserColor.entries
        val shooterCells = mutableListOf<CellPos>()

        // 新規タイルの追加がレイパス上に割り込み、既存ペアの接続を壊してしまうことがあるため、
        // 新規配置を確定する前に既存shooterが引き続き全て命中することも確認する。
        fun allExistingShootersStillConnect(candidateBoard: Board): Boolean =
            shooterCells.all { LaserSimulator.fireLaser(candidateBoard, it).hit != null }

        fun tryDirect(): Boolean {
            val cells = board.emptyCells().shuffled(random)
            if (cells.size < 2) return false
            for (color in colors.shuffled(random)) {
                for (i in cells.indices) {
                    val a = cells[i]
                    for (j in cells.indices) {
                        if (i == j) continue
                        val b = cells[j]
                        val dir = directionBetween(a, b) ?: continue
                        val trialBoard = board
                            .place(a, Tile.Shooter(dir, color))
                            .place(b, Tile.Receiver(color))
                        val result = LaserSimulator.fireLaser(trialBoard, a)
                        if (result.hit == b && allExistingShootersStillConnect(trialBoard)) {
                            board = trialBoard
                            shooterCells += a
                            return true
                        }
                    }
                }
            }
            return false
        }

        fun tryBend(): Boolean {
            val shooterOnly = shooterCells
                .filter { board.tiles[it] is Tile.Shooter }
                .shuffled(random)
            for (origin in shooterOnly) {
                val originTile = board.tiles[origin] as Tile.Shooter
                val rColor = originTile.color
                val rDir = originTile.dir
                val rayCells = rayFrom(origin, rDir, rows, cols)
                    .filter { it !in board.tiles }
                    .shuffled(random)
                for (receiverPos in rayCells) {
                    val candidates = board.emptyCells()
                        .filter { it != receiverPos }
                        .shuffled(random)
                    for (shooterPos in candidates) {
                        val sDir = directionBetween(shooterPos, origin) ?: continue
                        for (sColor in colors.filter { it != rColor }.shuffled(random)) {
                            val trialBoard = board
                                .place(shooterPos, Tile.Shooter(sDir, sColor))
                                .place(receiverPos, Tile.Receiver(rColor))
                            val result = LaserSimulator.fireLaser(trialBoard, shooterPos)
                            if (result.hit == receiverPos && allExistingShootersStillConnect(trialBoard)) {
                                board = trialBoard
                                shooterCells += shooterPos
                                return true
                            }
                        }
                    }
                }
            }
            return false
        }

        for (k in numPairs downTo 1) {
            val preferBend = shooterCells.isNotEmpty() && random.nextDouble() < 0.65
            if (preferBend) {
                if (!tryBend()) tryDirect()
            } else {
                if (!tryDirect()) tryBend()
            }
        }
        return board
    }
}

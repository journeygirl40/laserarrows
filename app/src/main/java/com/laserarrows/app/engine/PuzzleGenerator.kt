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

    /**
     * 通常レベルの盤面はレベル番号から決まるシードで生成し、同じレベルは何度挑戦しても同じ盤面にする(★の取り直しに意味を持たせるため)。
     * 注意: generate()の乱数の消費順を変えると、全レベルの盤面が変わってしまう。
     */
    fun seedForLevel(level: Int): Long = LEVEL_SEED_SALT + level

    private const val LEVEL_SEED_SALT = 0x1A5E_A770_0000L

    private const val CANDIDATES_PER_LEVEL = 8
    private const val ANALYSIS_NODE_BUDGET = 30_000

    /** なぞのたまごが登場し始めるレベル(第2ワールドの目玉)。 */
    const val EGG_FIRST_LEVEL = 11

    /** そのレベルで1ペアごとに「たまごのペア」を試す確率。登場直後は控えめにし、先へ進むほど増やす。 */
    fun eggChance(level: Int): Double =
        if (level < EGG_FIRST_LEVEL) 0.0 else (0.18 + (level - EGG_FIRST_LEVEL) * 0.01).coerceAtMost(0.35)

    /** レベルごとに目指す「迷いどころ」の数。Lv.1〜5は1つ、以降5レベルごとに1つずつ増やし、6で頭打ち。 */
    fun targetTraps(level: Int): Int = (1 + (level - 1) / 5).coerceAtMost(6)

    /**
     * 通常レベルの盤面。レベルのシードから候補をいくつか作り、迷いどころ(撃てるけれど詰む手)の数が
     * そのレベルの目標に最も近いものを選ぶ。目標に届く候補が複数あれば、超過が少ない(=難しすぎない)ものを優先する。
     */
    fun generateForLevel(level: Int, onProgress: (Float) -> Unit = {}): Generated {
        val size = levelSize(level)
        val random = Random(seedForLevel(level))
        val target = targetTraps(level)
        // 候補を1つ作って分析するたびに進捗を知らせる(大きな盤面では数秒かかるので、読み込みバーに使う)。
        // 乱数の消費順を変えないよう、候補の生成は必ずこの順番で行う。
        val scored = List(CANDIDATES_PER_LEVEL) { i ->
            val candidate = generateWithSolution(size.rows, size.cols, size.numPairs, random, eggChance(level))
            val traps = ParSolver.analyze(candidate.board, nodeBudget = ANALYSIS_NODE_BUDGET).traps
            onProgress((i + 1).toFloat() / CANDIDATES_PER_LEVEL)
            candidate to (if (traps >= target) 1000 - (traps - target) else traps)
        }
        return scored.maxBy { it.second }.first
    }

    /** 生成した盤面と、それを必ず全消しできる撃ち順(発射元セルの列)。 */
    data class Generated(val board: Board, val solution: List<CellPos>)

    fun generate(rows: Int, cols: Int, numPairs: Int, random: Random = Random): Board =
        generateWithSolution(rows, cols, numPairs, random).board

    /**
     * 「最後に撃つペアから順に置いていく」逆算方式で生成する。新しく置くペアは、置いた時点の盤面で相手に届けばよい。
     * 置いた順の逆順に撃てば、撃つたびに盤面が「そのペアを置く直前の盤面」に戻るので、必ず全消しできる。
     */
    fun generateWithSolution(rows: Int, cols: Int, numPairs: Int, random: Random = Random, eggChance: Double = 0.0): Generated {
        var board = Board(rows, cols)
        val colors = LaserColor.entries
        val shooterCells = mutableListOf<CellPos>()

        // 直線のペアは、既存の矢印の行く手をふさがない配置だけにして、序盤の盤面をやさしく保つ。
        // (道案内のペアのせいで最初から届かない矢印もあるので、「今届いている矢印を新たにふさがない」ことだけを見る。)
        fun allExistingShootersStillConnect(candidateBoard: Board): Boolean =
            shooterCells.all { LaserSimulator.fireLaser(board, it).hit == null || LaserSimulator.fireLaser(candidateBoard, it).hit != null }

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

        // 道案内のペア: 新しい子Sが既存の矢印Oに当たってOの向きへ曲がり、O の進む先で待つ同じ種類の相手Rに出会う。
        // Rは O の行く手をふさぐことがある(=Oより先にSを撃たないといけない)ので、既存の矢印が届くかは確認しない。
        fun tryBend(): Boolean {
            val shooterOnly = shooterCells
                .filter { (board.tiles[it] as? Tile.Shooter)?.egg == false }
                .shuffled(random)
            for (origin in shooterOnly) {
                val originTile = board.tiles[origin] as Tile.Shooter
                val oColor = originTile.color
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
                        for (sColor in colors.filter { it != oColor }.shuffled(random)) {
                            val trialBoard = board
                                .place(shooterPos, Tile.Shooter(sDir, sColor))
                                .place(receiverPos, Tile.Receiver(sColor))
                            val result = LaserSimulator.fireLaser(trialBoard, shooterPos)
                            if (result.hit == receiverPos && result.redirects > 0) {
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

        // 取り合いのペア: 既存の子Aの通り道(Aの相手より手前)に、Aと同じ種類の相手R1を置き、R1にしか届かない新しい子Bを足す。
        // Aを先に撃つとR1を取ってしまいBが行き場を失う(詰む)。Bを先に撃てば全員出会える。
        fun tryDecoy(): Boolean {
            for (a in shooterCells.filter { (board.tiles[it] as? Tile.Shooter)?.egg == false }.shuffled(random)) {
                val result = LaserSimulator.fireLaser(board, a)
                val target = result.hit ?: continue
                val color = (board.tiles[a] as Tile.Shooter).color
                if (board.tiles[target]?.color != color) continue
                val decoyCells = result.path.drop(1).dropLast(1).filter { it !in board.tiles }.shuffled(random)
                for (decoy in decoyCells) {
                    for (b in board.emptyCells().filter { it != decoy }.shuffled(random)) {
                        val dir = directionBetween(b, decoy) ?: continue
                        val trialBoard = board
                            .place(decoy, Tile.Receiver(color))
                            .place(b, Tile.Shooter(dir, color))
                        if (LaserSimulator.fireLaser(trialBoard, b).hit == decoy) {
                            board = trialBoard
                            shooterCells += b
                            return true
                        }
                    }
                }
            }
            return false
        }

        // たまごのペア: なぞのたまごEが既存の動物の矢印Oにぶつかって「Oの赤ちゃん」に変わり、Oの向きへ曲がって、
        // その先で待つOと同じ種類の相手Rに出会う。RはOの通り道にあるため、Oを先に撃つとRを取られてたまごが行き場を失う。
        fun tryEgg(): Boolean {
            for (o in shooterCells.filter { (board.tiles[it] as? Tile.Shooter)?.egg == false }.shuffled(random)) {
                val oTile = board.tiles[o] as Tile.Shooter
                val receiverCells = rayFrom(o, oTile.dir, rows, cols).filter { it !in board.tiles }.shuffled(random)
                for (receiverPos in receiverCells) {
                    for (eggPos in board.emptyCells().filter { it != receiverPos }.shuffled(random)) {
                        val eggDir = directionBetween(eggPos, o) ?: continue
                        val trialBoard = board
                            .place(eggPos, Tile.egg(eggDir))
                            .place(receiverPos, Tile.Receiver(oTile.color))
                        if (LaserSimulator.fireLaser(trialBoard, eggPos).hit == receiverPos) {
                            board = trialBoard
                            shooterCells += eggPos
                            return true
                        }
                    }
                }
            }
            return false
        }

        for (k in numPairs downTo 1) {
            // eggChanceが0のレベル(Lv.10まで)では乱数を消費しないので、たまご導入前の盤面はそのまま変わらない。
            if (eggChance > 0.0 && shooterCells.isNotEmpty() && random.nextDouble() < eggChance && tryEgg()) continue
            if (shooterCells.isNotEmpty() && random.nextDouble() < 0.3 && tryDecoy()) continue
            val preferBend = shooterCells.isNotEmpty() && random.nextDouble() < 0.65
            if (preferBend) {
                if (!tryBend()) tryDirect()
            } else {
                if (!tryDirect()) tryBend()
            }
        }
        return Generated(board, shooterCells.reversed())
    }
}

package com.laserarrows.app.engine

data class ParResult(val par: Int, val approximate: Boolean)

/**
 * 盤面の「迷いどころ」の分析結果。
 * traps: 最善手順を辿る途中の各局面で、撃てる(誰かに出会える)のに選ぶと全消しできなくなる(詰む)手の延べ数。
 * コンボ倍率の端数で1匹差がつくだけの手は、遊ぶ人には違いが感じられないので数えない。
 * 多いほど「どれを先に撃つか」を考える意味がある盤面。clearable=falseなら全消しの手順が見つからなかった。
 */
data class BoardAnalysis(val par: Int, val traps: Int, val clearable: Boolean, val approximate: Boolean)

/**
 * 盤面の「パー」(その盤面を全消しできる手順のうち、最も点の高いものの点)を求める。
 * 途中で詰む手順の点はパーに数えない(パーを狙うとクリアできない、という矛盾を避けるため)。
 * 全消しできる手順が見つからない盤面に限り、詰む手順も含めた最高点をパーとする。
 * 撃つ順番を全て探索し、残りタイルの集合(ビットマスク)とコンボ値でメモ化する。
 * ミスは盤面が変わらずライフを失うだけなので最適解には含まれず、命中する手だけを展開する。
 * 探索ノード数が上限を超えた場合は、以降を貪欲法(その場で一番点の高い手を選び続ける)で埋めた下限値を返す。
 */
object ParSolver {
    private const val DEFAULT_NODE_BUDGET = 200_000

    fun solve(board: Board, startCombo: Int = 0, nodeBudget: Int = DEFAULT_NODE_BUDGET): ParResult {
        val cells = board.tiles.keys.toList()
        if (cells.size > 63) {
            return ParResult(greedy(board, startCombo), approximate = true)
        }
        val fullMask = (1L shl cells.size) - 1
        val clearing = Search(board, cells, nodeBudget, requireClear = true)
        val clearPar = clearing.best(fullMask, startCombo)
        if (clearPar != STUCK) return ParResult(clearPar, approximate = clearing.exhausted)
        val anyPath = Search(board, cells, nodeBudget, requireClear = false)
        return ParResult(anyPath.best(fullMask, startCombo), approximate = true)
    }

    fun analyze(board: Board, startCombo: Int = 0, nodeBudget: Int = DEFAULT_NODE_BUDGET): BoardAnalysis {
        val cells = board.tiles.keys.toList()
        if (cells.size > 63) return BoardAnalysis(greedy(board, startCombo), traps = 0, clearable = false, approximate = true)
        val search = Search(board, cells, nodeBudget, requireClear = true)
        var mask = (1L shl cells.size) - 1
        var combo = startCombo
        val par = search.best(mask, combo)
        if (par == STUCK) return BoardAnalysis(0, traps = 0, clearable = false, approximate = search.exhausted)
        var traps = 0
        var remaining = par
        // パーの手順を1手ずつ辿り、各局面で「撃てるけれど詰む手」を数える。
        while (remaining > 0 || search.hasReceivers(mask)) {
            val options = search.optionsAt(mask, combo)
            val bestOption = options.firstOrNull { it.value != STUCK && it.move.gained + it.value == remaining } ?: break
            traps += options.count { it.value == STUCK }
            remaining -= bestOption.move.gained
            combo = bestOption.move.nextCombo
            mask = bestOption.nextMask
        }
        return BoardAnalysis(par, traps, clearable = true, approximate = search.exhausted)
    }

    /** 全消しを条件にした探索で「この先どう撃っても受信機が残る」ことを表す値。 */
    private const val STUCK = Int.MIN_VALUE

    private class Move(val origin: CellPos, val hit: CellPos, val gained: Int, val nextCombo: Int)

    private fun movesFor(board: Board, combo: Int): List<Move> =
        board.tiles.filterValues { it is Tile.Shooter }.keys.mapNotNull { origin ->
            val result = LaserSimulator.fireLaser(board, origin)
            val hit = result.hit ?: return@mapNotNull null
            if (board.tiles[hit] is Tile.Receiver) {
                val nextCombo = combo + 1
                Move(origin, hit, ScoreRules.shotScore(nextCombo, ScoreRules.relayCount(result)), nextCombo)
            } else {
                // 矢印同士の衝突: 得点は無くコンボも途切れるが、盤面を動かすために必要な場合もあるので候補に残す。
                Move(origin, hit, 0, 0)
            }
        }

    private fun hasReceivers(board: Board): Boolean = board.tiles.values.any { it is Tile.Receiver }

    /** その場で一番点の高い手を選び続ける近似。requireClearなら、受信機を残して詰んだ場合はSTUCKを返す。 */
    private fun greedy(start: Board, startCombo: Int, requireClear: Boolean = false): Int {
        var board = start
        var combo = startCombo
        var total = 0
        while (true) {
            val move = movesFor(board, combo).maxByOrNull { it.gained }
                ?: return if (requireClear && hasReceivers(board)) STUCK else total
            total += move.gained
            combo = move.nextCombo
            board = board.remove(move.origin).remove(move.hit)
        }
    }

    private class Search(
        private val initial: Board,
        private val cells: List<CellPos>,
        private val nodeBudget: Int,
        private val requireClear: Boolean,
    ) {
        private val indexOf = cells.withIndex().associate { (i, pos) -> pos to i }
        private val memo = HashMap<Pair<Long, Int>, Int>()
        private var nodes = 0
        var exhausted = false
            private set

        class Option(val move: Move, val nextMask: Long, val value: Int)

        fun hasReceivers(mask: Long): Boolean = hasReceivers(boardFor(mask))

        /** その局面で撃てる各手と、撃った後に取れる最高点(全消しできなければSTUCK)。 */
        fun optionsAt(mask: Long, combo: Int): List<Option> =
            movesFor(boardFor(mask), combo).map { move ->
                val nextMask = nextMaskOf(mask, move)
                Option(move, nextMask, best(nextMask, move.nextCombo))
            }

        private fun nextMaskOf(mask: Long, move: Move): Long =
            mask and (1L shl indexOf.getValue(move.origin)).inv() and (1L shl indexOf.getValue(move.hit)).inv()

        private fun boardFor(mask: Long): Board =
            initial.copy(tiles = cells.withIndex().filter { (i, _) -> mask and (1L shl i) != 0L }.associate { (_, pos) -> pos to initial.tiles.getValue(pos) })

        fun best(mask: Long, combo: Int): Int {
            val key = mask to combo
            memo[key]?.let { return it }
            val board = boardFor(mask)
            if (nodes >= nodeBudget) {
                exhausted = true
                return greedy(board, combo, requireClear)
            }
            nodes++
            // 受信機が全て消えた時点でクリア(残った矢印は撃たない)。
            if (!hasReceivers(board)) {
                memo[key] = 0
                return 0
            }
            var result = if (requireClear) STUCK else 0
            for (move in movesFor(board, combo)) {
                val rest = best(nextMaskOf(mask, move), move.nextCombo)
                if (rest != STUCK) result = maxOf(result, move.gained + rest)
            }
            memo[key] = result
            return result
        }
    }
}

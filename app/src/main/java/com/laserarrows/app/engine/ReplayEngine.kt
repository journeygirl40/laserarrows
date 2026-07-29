package com.laserarrows.app.engine

/**
 * お気に入りのクリア記録を「一手ごと」に振り返るための再生ロジック。
 * 保存するのは発射元セルの列だけで、各ステップの盤面・スコア・コンボは
 * 初期盤面から都度LaserSimulatorで再計算する(GameViewModel.finishShotの得点ロジックと同じ)。
 */
object ReplayEngine {
    data class Frame(val board: Board, val score: Int, val combo: Int, val firedFrom: CellPos?, val hitAt: CellPos?)

    fun encodeSteps(steps: List<CellPos>): String = steps.joinToString(";") { "${it.row},${it.col}" }

    fun decodeSteps(raw: String): List<CellPos> {
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { part ->
            val xy = part.split(",")
            if (xy.size != 2) return@mapNotNull null
            val row = xy[0].toIntOrNull() ?: return@mapNotNull null
            val col = xy[1].toIntOrNull() ?: return@mapNotNull null
            CellPos(row, col)
        }
    }

    /** stepIndex手目までを適用した状態(0なら初期盤面)を返す。 */
    fun frameAt(initialBoard: Board, steps: List<CellPos>, stepIndex: Int): Frame {
        var board = initialBoard
        var score = 0
        var combo = 0
        var firedFrom: CellPos? = null
        var hitAt: CellPos? = null
        for (i in 0 until stepIndex) {
            val origin = steps[i]
            val hit = LaserSimulator.fireLaser(board, origin).hit ?: continue
            val wasReceiver = board.tiles[hit] is Tile.Receiver
            combo = if (wasReceiver) combo + 1 else 0
            score += if (wasReceiver) ScoreRules.shotScore(combo) else 0
            board = board.remove(origin).remove(hit)
            firedFrom = origin
            hitAt = hit
        }
        return Frame(board, score, combo, firedFrom, hitAt)
    }
}

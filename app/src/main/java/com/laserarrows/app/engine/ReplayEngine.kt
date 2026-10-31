package com.laserarrows.app.engine

/**
 * お気に入りのクリア記録を「一手ごと」に振り返るための再生ロジック。
 * 保存するのは各手の種類(発射 or 光源設置)と座標などの最小限の情報だけで、
 * 各ステップの盤面・スコア・コンボは初期盤面から都度再計算する
 * (GameViewModelのfinishShot/confirmLightDirectionの得点ロジックと同じ)。
 */
object ReplayEngine {
    sealed class Step {
        data class Fire(val origin: CellPos) : Step()
        data class AddLight(val cell: CellPos, val color: LaserColor, val dir: Direction) : Step()
    }

    data class Frame(
        val board: Board,
        val score: Int,
        val combo: Int,
        val firedFrom: CellPos?,
        val hitAt: CellPos?,
        /** 直前の手が光源設置だった場合、その設置先セル(演出・表示用)。 */
        val lightAddedAt: CellPos?,
        /** 直前の手が発射だった場合の、ビーム経路描画用の結果(発射元→着弾までの経路・色)。 */
        val shotResult: LaserResult?,
        /** ビームが曲がる中継セル(演出用)。 */
        val shotBends: Set<CellPos>,
        /** 着弾先が光る球ではなく別の矢印だった場合(狙い通りの接続ではない見た目にする)。 */
        val shotHitIsShooterClash: Boolean,
    )

    fun encodeSteps(steps: List<Step>): String = steps.joinToString(";") { step ->
        when (step) {
            is Step.Fire -> "F,${step.origin.row},${step.origin.col}"
            is Step.AddLight -> "A,${step.cell.row},${step.cell.col},${step.color.name},${step.dir.name}"
        }
    }

    fun decodeSteps(raw: String): List<Step> {
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { part -> decodeStep(part) }
    }

    private fun decodeStep(part: String): Step? {
        val fields = part.split(",")
        return when (fields.getOrNull(0)) {
            "F" -> {
                if (fields.size != 3) return null
                val row = fields[1].toIntOrNull() ?: return null
                val col = fields[2].toIntOrNull() ?: return null
                Step.Fire(CellPos(row, col))
            }
            "A" -> {
                if (fields.size != 5) return null
                val row = fields[1].toIntOrNull() ?: return null
                val col = fields[2].toIntOrNull() ?: return null
                val color = runCatching { LaserColor.valueOf(fields[3]) }.getOrNull() ?: return null
                val dir = runCatching { Direction.valueOf(fields[4]) }.getOrNull() ?: return null
                Step.AddLight(CellPos(row, col), color, dir)
            }
            // 旧フォーマット(発射元セル座標のみ、種別プレフィックス無し)との後方互換。
            else -> {
                if (fields.size != 2) return null
                val row = fields[0].toIntOrNull() ?: return null
                val col = fields[1].toIntOrNull() ?: return null
                Step.Fire(CellPos(row, col))
            }
        }
    }

    /** stepIndex手目までを適用した状態(0なら初期盤面)を返す。 */
    fun frameAt(initialBoard: Board, steps: List<Step>, stepIndex: Int): Frame {
        var board = initialBoard
        var score = 0
        var combo = 0
        var firedFrom: CellPos? = null
        var hitAt: CellPos? = null
        var lightAddedAt: CellPos? = null
        var shotResult: LaserResult? = null
        var shotBends: Set<CellPos> = emptySet()
        var shotHitIsShooterClash = false
        for (i in 0 until stepIndex) {
            when (val step = steps[i]) {
                is Step.Fire -> {
                    lightAddedAt = null
                    val result = LaserSimulator.fireLaser(board, step.origin)
                    val hit = result.hit
                    firedFrom = step.origin
                    hitAt = hit
                    shotResult = result
                    shotBends = findBends(result.path)
                    shotHitIsShooterClash = hit?.let { board.tiles[it] is Tile.Shooter } ?: false
                    if (hit == null) continue
                    val wasReceiver = board.tiles[hit] is Tile.Receiver
                    combo = if (wasReceiver) combo + 1 else 0
                    score += if (wasReceiver) ScoreRules.shotScore(combo, ScoreRules.relayCount(result)) else 0
                    board = board.remove(step.origin).remove(hit)
                }
                is Step.AddLight -> {
                    board = board.place(step.cell, Tile.Shooter(step.dir, step.color))
                    firedFrom = null
                    hitAt = null
                    lightAddedAt = step.cell
                    combo = 0
                    shotResult = null
                    shotBends = emptySet()
                    shotHitIsShooterClash = false
                }
            }
        }
        return Frame(board, score, combo, firedFrom, hitAt, lightAddedAt, shotResult, shotBends, shotHitIsShooterClash)
    }

    /** ビーム経路のうち曲がり角に当たるセル(演出用)。GameViewModelの発射時の見た目と合わせる。 */
    internal fun findBends(path: List<CellPos>): Set<CellPos> {
        if (path.size < 3) return emptySet()
        val bends = mutableSetOf<CellPos>()
        for (i in 1 until path.size - 1) {
            val d1 = path[i].row - path[i - 1].row to path[i].col - path[i - 1].col
            val d2 = path[i + 1].row - path[i].row to path[i + 1].col - path[i].col
            if (d1 != d2) bends += path[i]
        }
        return bends
    }
}

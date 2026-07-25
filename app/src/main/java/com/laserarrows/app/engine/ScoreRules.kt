package com.laserarrows.app.engine

import kotlin.math.roundToInt

object ScoreRules {
    fun comboMultiplier(combo: Int): Double = 1.0 + combo * 0.1
    fun shotScore(combo: Int): Int = (1 * comboMultiplier(combo)).roundToInt()

    /** ノーミスで全ペアを連続コンボ成立させた場合の理論上の最高スコア。 */
    fun maxScoreForPairs(numPairs: Int): Int = (1..numPairs).sumOf { shotScore(it) }
}

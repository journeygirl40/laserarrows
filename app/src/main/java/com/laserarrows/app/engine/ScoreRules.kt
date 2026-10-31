package com.laserarrows.app.engine

import kotlin.math.roundToInt

object ScoreRules {
    fun comboMultiplier(combo: Int): Double = 1.0 + combo * 0.1

    /** 命中までに違う種類の矢印に道案内された(曲がった)回数。 */
    fun relayCount(result: LaserResult): Int = result.redirects

    /** 道案内が多いほど大きく伸びる基礎点(0回=1, 1回=3, 2回=6, 3回=10…)。遠回りして出会う手を先に撃つ解き順ほど高得点になる。 */
    fun basePoints(relays: Int): Int = 1 + relays * (relays + 3) / 2

    fun shotScore(combo: Int, relays: Int = 0): Int = (basePoints(relays) * comboMultiplier(combo)).roundToInt()

    /** ★評価: クリアで1、パーの70%以上で2、パー以上で3。 */
    fun starsFor(levelScore: Int, par: Int): Int = when {
        par <= 0 || levelScore >= par -> 3
        levelScore * 10 >= par * 7 -> 2
        else -> 1
    }
}

package com.laserarrows.app.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreRulesTest {

    @Test
    fun `中継なし・コンボなしのショットは1点`() {
        assertEquals(1, ScoreRules.shotScore(0))
    }

    @Test
    fun `中継が増えるほど基礎点が大きく伸びる`() {
        assertEquals(listOf(1, 3, 6, 10), (0..3).map { ScoreRules.basePoints(it) })
    }

    @Test
    fun `コンボ倍率は基礎点に掛かる`() {
        assertEquals(3, ScoreRules.shotScore(1, relays = 1))
        assertEquals(12, ScoreRules.shotScore(10, relays = 2))
    }

    @Test
    fun `relayCountは道案内で曲がった回数`() {
        assertEquals(2, ScoreRules.relayCount(LaserResult(CellPos(0, 0), emptyList(), emptyList(), redirects = 2)))
    }

    @Test
    fun `starsForはパーに対する割合で1から3つ`() {
        assertEquals(3, ScoreRules.starsFor(10, 10))
        assertEquals(2, ScoreRules.starsFor(7, 10))
        assertEquals(1, ScoreRules.starsFor(6, 10))
    }
}

package com.laserarrows.app.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreRulesTest {

    @Test
    fun `コンボなしのショットは15点`() {
        assertEquals(15, ScoreRules.shotScore(0))
    }

    @Test
    fun `コンボが乗るとスコアが増える`() {
        assertEquals(17, ScoreRules.shotScore(1))
        assertEquals(30, ScoreRules.shotScore(10))
    }
}

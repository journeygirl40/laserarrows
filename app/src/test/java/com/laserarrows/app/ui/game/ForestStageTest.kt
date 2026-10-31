package com.laserarrows.app.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForestStageTest {
    @Test
    fun stagesAreOrderedAndIndexed() {
        ForestStage.stages.forEachIndexed { i, s -> assertEquals(i, s.level) }
        ForestStage.stages.zipWithNext().forEach { (a, b) -> assertTrue(a.minScore < b.minScore) }
    }

    @Test
    fun stageForScoreUsesThresholds() {
        assertEquals(0, ForestStage.of(0).level)
        assertEquals(1, ForestStage.of(1).level)
        assertEquals(1, ForestStage.of(49).level)
        assertEquals(2, ForestStage.of(50).level)
        assertEquals(7, ForestStage.of(1_000_000).level)
    }

    @Test
    fun lastStageHasNoNext() {
        assertNull(ForestStage.next(ForestStage.stages.last()))
        assertEquals(1, ForestStage.next(ForestStage.stages.first())?.level)
    }
}

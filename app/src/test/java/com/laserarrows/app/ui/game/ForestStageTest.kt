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
    fun stageForStarsUsesThresholds() {
        // にぎわいは集めた★の数で育つ。
        assertEquals(0, ForestStage.of(0).level)
        assertEquals(1, ForestStage.of(1).level)
        assertEquals(1, ForestStage.of(9).level)
        assertEquals(2, ForestStage.of(10).level)
        // 全部★3で進んだ時の目安: Lv.10(30個)で3段目、Lv.30(90個)で5段目、Lv.80(240個)で最後の段。
        assertEquals(3, ForestStage.of(30).level)
        assertEquals(5, ForestStage.of(90).level)
        assertEquals(7, ForestStage.of(240).level)
        assertEquals(7, ForestStage.of(1_000_000).level)
    }

    @Test
    fun lastStageHasNoNext() {
        assertNull(ForestStage.next(ForestStage.stages.last()))
        assertEquals(1, ForestStage.next(ForestStage.stages.first())?.level)
    }

    @Test
    fun expansionStagesAreOrderedAndUsePopulation() {
        ForestExpansion.stages.forEachIndexed { i, s -> assertEquals(i, s.level) }
        ForestExpansion.stages.zipWithNext().forEach { (a, b) -> assertTrue(a.minScore < b.minScore) }
        assertEquals(0, ForestExpansion.of(0).level)
        assertEquals(1, ForestExpansion.of(30).level)
        assertEquals(8, ForestExpansion.of(1_000_000).level)
        assertNull(ForestExpansion.next(ForestExpansion.stages.last()))
    }
}

package com.jesuslcorominas.wwii.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameInfoTest {
    @Test
    fun campaignHasEighteenTurns() {
        assertEquals(18, GameInfo.CAMPAIGN_TURNS)
    }

    @Test
    fun startYearIs1939() {
        assertEquals(1939, GameInfo.START_YEAR)
    }

    @Test
    fun summaryMentionsTitleAndTurns() {
        val summary = GameInfo.summary()

        assertTrue(summary.contains(GameInfo.TITLE))
        assertTrue(summary.contains("18"))
    }
}

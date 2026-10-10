package com.example.ui.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveViewModelDeltaTest {
    @Test
    fun hidesDeltasBeforeSessionStartsWhenNoLapTimesExist() {
        assertTrue(
            shouldHideDeltasBeforeTiming(
                sessionStarted = false,
                sessionFinished = false,
                hasLapTimes = false
            )
        )
    }

    @Test
    fun keepsDeltasWhenSessionHasStartedOrHasLapTimes() {
        assertFalse(
            shouldHideDeltasBeforeTiming(
                sessionStarted = true,
                sessionFinished = false,
                hasLapTimes = false
            )
        )
        assertFalse(
            shouldHideDeltasBeforeTiming(
                sessionStarted = false,
                sessionFinished = false,
                hasLapTimes = true
            )
        )
    }

    @Test
    fun keepsFinalClassificationDeltasAfterSessionFinishes() {
        assertFalse(
            shouldHideDeltasBeforeTiming(
                sessionStarted = false,
                sessionFinished = true,
                hasLapTimes = false
            )
        )
    }

    @Test
    fun normalizesPositiveAndNegativeLeaderGapSigns() {
        assertEquals("+0.500s", normalizeLeaderGap("+0.500s"))
        assertEquals("+0.500s", normalizeLeaderGap("-0.500s"))
        assertEquals("1 LAP", normalizeLeaderGap("1 LAP"))
        assertEquals(null, normalizeLeaderGap(" "))
    }
}

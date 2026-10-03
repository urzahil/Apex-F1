package com.example

import com.example.data.model.HistoryBestLap
import com.example.data.model.HistoryDriverInfo
import com.example.data.model.HistoryQualiStat
import com.example.data.model.HistoryTimingLine
import com.example.data.repository.HistoryClassificationMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HistoryClassificationMapperTest {
    @Test
    fun qualifyingSegmentsAreReadByExplicitKeysNotIterationOrder() {
        val line = HistoryTimingLine(
            position = 1,
            racingNumber = "44",
            stats = linkedMapOf(
                "Q3" to HistoryQualiStat(timeDiffToFastest = "0.000"),
                "Q1" to HistoryQualiStat(timeDiffToFastest = "+0.500"),
                "Q2" to HistoryQualiStat(timeDiffToFastest = "+0.200")
            ),
            bestLapTimes = linkedMapOf(
                "Q3" to HistoryBestLap("1:30.100"),
                "Q1" to HistoryBestLap("1:31.200"),
                "Q2" to HistoryBestLap("1:30.700")
            )
        )
        val result = HistoryClassificationMapper.map(
            lines = mapOf("driver-key" to line),
            driverMap = mapOf("44" to HistoryDriverInfo(racingNumber = "44", fullName = "Lewis Hamilton")),
            totalRaceTime = null
        ).single()

        assertEquals("1:31.200", result.q1Time)
        assertEquals("1:30.700", result.q2Time)
        assertEquals("1:30.100", result.q3Time)
        assertEquals("+0.500", result.q1Diff)
        assertEquals("+0.200", result.q2Diff)
        assertEquals("0.000", result.q3Diff)
        assertEquals("Lewis Hamilton", result.fullName)
    }

    @Test
    fun driverMetadataUsesRacingNumberEvenWhenLinesMapKeyDiffers() {
        val line = HistoryTimingLine(position = 2, racingNumber = "016")
        val driver = HistoryDriverInfo(racingNumber = "16", fullName = "Charles Leclerc")
        val result = HistoryClassificationMapper.map(mapOf("legacy-key" to line), mapOf("16" to driver), null).single()

        assertEquals("16", result.driverNumber)
        assertEquals("Charles Leclerc", result.fullName)
        assertNotNull(result)
    }
}

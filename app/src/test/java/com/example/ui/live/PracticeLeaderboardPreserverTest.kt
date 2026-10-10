package com.example.ui.live

import com.example.data.model.LiveSessionInfo
import com.example.data.model.StatusResponse
import com.example.data.model.TimingDriverLine
import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeLeaderboardPreserverTest {
    @Test
    fun finalisedResponseWithChangedMetadataStillPreservesSamePracticeSession() {
        val previous = StatusResponse(
            session = LiveSessionInfo(
                type = "Practice",
                path = "2026/GP/Practice_1"
            )
        )
        val finalised = StatusResponse(
            session = LiveSessionInfo(
                type = null,
                name = null,
                path = "2026/GP/Practice_1"
            )
        )

        assertEquals(
            true,
            shouldMergePracticeLeaderboard(
                finalised,
                previous,
                "2026/GP/Practice_1",
                "2026/GP/Practice_1"
            )
        )
        assertEquals(
            false,
            shouldMergePracticeLeaderboard(
                finalised,
                previous,
                "2026/GP/Practice_2",
                "2026/GP/Practice_1"
            )
        )
    }

    @Test
    fun partialTopThreeResponseRetainsDriversMissingFromFinalisedFeed() {
        val previous = (1..20).map { number ->
            TimingDriverLine(
                position = number.toString(),
                racingNumber = number.toString(),
                driverNumber = number.toString(),
                name = "Driver " + number,
                bestLapTime = "1:30.000"
            )
        }
        val current = previous.take(3).map { it.copy(bestLapTime = "1:29.999") }

        val result = mergePracticeLeaderboard(current, previous)

        assertEquals(20, result.size)
        assertEquals("1:29.999", result.first().bestLapTime)
        assertEquals("Driver 20", result.last().name)
        assertEquals("1:30.000", result.last().bestLapTime)
    }

    @Test
    fun currentRowsKeepFreshValuesAndFillOmittedFieldsFromPreviousRow() {
        val previous = listOf(
            TimingDriverLine(
                position = "7", racingNumber = "44", driverNumber = "44",
                name = "Driver 44", team = "Mercedes", bestLapTime = "1:31.234",
                lapsCompleted = 12
            )
        )
        val current = listOf(
            TimingDriverLine(
                position = "5", racingNumber = "44", driverNumber = "44",
                bestLapTime = "1:30.900"
            )
        )

        val result = mergePracticeLeaderboard(current, previous).single()

        assertEquals("5", result.position)
        assertEquals("Driver 44", result.name)
        assertEquals("Mercedes", result.team)
        assertEquals("1:30.900", result.bestLapTime)
        assertEquals(12, result.lapsCompleted)
    }

    @Test
    fun newDriversAreAddedAndPreviousDriversAreNotDuplicated() {
        val previous = listOf(
            TimingDriverLine(position = "1", racingNumber = "1", driverNumber = "1", name = "One"),
            TimingDriverLine(position = "2", racingNumber = "2", driverNumber = "2", name = "Two")
        )
        val current = listOf(
            TimingDriverLine(
                position = "1",
                racingNumber = "1",
                driverNumber = "1",
                name = "One updated"
            ),
            TimingDriverLine(position = "2", racingNumber = "3", driverNumber = "3", name = "Three")
        )

        val result = mergePracticeLeaderboard(current, previous)

        assertEquals(listOf("One updated", "Three", "Two"), result.map { it.name })
    }

    @Test
    fun finalisedRaceWithPartialRefreshKeepsKnownGapsForAllDrivers() {
        val previousStatus = StatusResponse(
            session = LiveSessionInfo(type = "Race", path = "2026/GP/Race")
        )
        val finalisedStatus = StatusResponse(
            session = LiveSessionInfo(
                type = null,
                name = null,
                sessionStatus = "Finished",
                path = "2026/GP/Race"
            )
        )

        assertEquals(
            true,
            shouldMergeSameSessionLeaderboard(
                finalisedStatus,
                previousStatus,
                "2026/GP/Race",
                "2026/GP/Race"
            )
        )
        assertEquals(
            false,
            shouldMergeSameSessionLeaderboard(
                finalisedStatus,
                previousStatus,
                "2026/GP/Sprint",
                "2026/GP/Race"
            )
        )

        val previous = (1..20).map { number ->
            TimingDriverLine(
                position = number.toString(),
                racingNumber = number.toString(),
                driverNumber = number.toString(),
                gapToLeader = if (number == 1) null else "+${number}.000",
                gap = if (number == 1) null else "+${number}.000",
                bestLapTime = "1:30.000"
            )
        }
        val partialRefresh = previous.take(3).map { driver ->
            driver.copy(
                gapToLeader = if (driver.getDisplayPosition() == "1") null else "+0.500",
                gap = if (driver.getDisplayPosition() == "1") null else "+0.500"
            )
        }
        val merged = mergePracticeLeaderboard(partialRefresh, previous)

        assertEquals(20, merged.size)
        assertEquals("+20.000", merged.single { it.driverNumber == "20" }.gapToLeader)
        assertEquals("+0.500", merged.single { it.driverNumber == "2" }.gapToLeader)
    }

    @Test
    fun finalHistoryClassificationRestoresResultsForDriversMissingFromLiveFeed() {
        val current = (1..3).map { number ->
            TimingDriverLine(
                position = number.toString(),
                racingNumber = number.toString(),
                driverNumber = number.toString()
            )
        }
        val classification = (1..20).map { number ->
            com.example.data.repository.MergedHistoryClassification(
                position = number.toString(),
                driverNumber = number.toString(),
                fullName = "Driver $number",
                broadcastName = "Driver $number",
                tla = "D$number",
                teamName = "Team $number",
                teamColour = "123456",
                headshotUrl = null,
                countryCode = null,
                gapToLeader = if (number == 1) null else "+$number.000",
                intervalToAhead = null,
                timeDiffToFastest = null,
                bestLapTime = "1:30." + number.toString().padStart(3, '0'),
                totalRaceTime = if (number == 1) "1:25:00.000" else null,
                q1Time = null,
                q2Time = null,
                q3Time = null,
                q1Diff = null,
                q2Diff = null,
                q3Diff = null,
                knockedOut = null,
                numberOfLaps = 15,
                numberOfPitStops = null,
                isRetired = false,
                inPit = false,
                stopped = false
            )
        }

        val result = mergeFinalSessionClassification(current, classification)

        assertEquals(20, result.size)
        assertEquals("1:30.020", result.single { it.driverNumber == "20" }.bestLapTime)
        assertEquals("+20.000", result.single { it.driverNumber == "20" }.gapToLeader)
        assertEquals("Driver 20", result.single { it.driverNumber == "20" }.name)
        assertEquals("1:25:00.000", result.single { it.driverNumber == "1" }.totalRaceTime)
    }
}

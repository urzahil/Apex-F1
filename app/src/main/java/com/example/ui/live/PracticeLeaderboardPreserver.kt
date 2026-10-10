package com.example.ui.live

import com.example.data.model.StatusResponse
import com.example.data.model.TimingDriverLine
import com.example.data.repository.MergedHistoryClassification

internal fun shouldMergePracticeLeaderboard(
    currentStatus: StatusResponse?,
    previousStatus: StatusResponse?,
    currentPath: String?,
    previousPath: String?
): Boolean {
    if (currentPath.isNullOrBlank() || currentPath != previousPath) return false
    fun isPractice(status: StatusResponse?): Boolean {
        val type = status?.session?.type.orEmpty()
        val name = status?.session?.name.orEmpty()
        return type.contains("Practice", true) || name.contains("Practice", true)
    }
    return isPractice(currentStatus) || isPractice(previousStatus)
}

/**
 * Keep the last complete leaderboard for the same session when the live/history feed
 * becomes partial after finalisation. This applies to races and sprints as well as
 * practice and qualifying, and never merges data across session paths.
 */
internal fun shouldMergeSameSessionLeaderboard(
    currentStatus: StatusResponse?,
    previousStatus: StatusResponse?,
    currentPath: String?,
    previousPath: String?
): Boolean {
    if (currentPath.isNullOrBlank() || currentPath != previousPath) return false

    fun isSupportedClassificationSession(status: StatusResponse?): Boolean {
        val type = status?.session?.type.orEmpty()
        val name = status?.session?.name.orEmpty()
        val metadata = "$type $name"
        return listOf("Practice", "Qualifying", "Race", "Sprint")
            .any { metadata.contains(it, ignoreCase = true) }
    }

    return isSupportedClassificationSession(currentStatus) ||
        isSupportedClassificationSession(previousStatus)
}

/**
 * Merges a partial practice timing response with the last known classification for the
 * same session. Current values win; fields omitted by the feed and drivers omitted from
 * the partial response retain their last known values.
 */
internal fun mergePracticeLeaderboard(
    current: List<TimingDriverLine>,
    previous: List<TimingDriverLine>
): List<TimingDriverLine> {
    fun key(driver: TimingDriverLine): String? {
        val value = (driver.driverNumber?.takeIf(String::isNotBlank) ?: driver.racingNumber)?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: return null
        val number = value.toDoubleOrNull()
        return if (number != null && number % 1.0 == 0.0) number.toInt().toString() else value
    }

    fun text(value: String?, fallback: String?): String? =
        value?.takeIf(String::isNotBlank) ?: fallback?.takeIf(String::isNotBlank)

    val previousByKey = previous.mapNotNull { driver -> key(driver)?.let { it to driver } }.toMap()
    val currentKeys = current.mapNotNull(::key).toSet()

    val mergedCurrent = current.map { driver ->
        val old = key(driver)?.let(previousByKey::get) ?: return@map driver
        driver.copy(
            position = text(driver.position, old.position),
            name = text(driver.name, old.name),
            tla = text(driver.tla, old.tla),
            acronym = text(driver.acronym, old.acronym),
            team = text(driver.team, old.team),
            teamColour = text(driver.teamColour, old.teamColour),
            gap = text(driver.gap, old.gap),
            gapToLeader = text(driver.gapToLeader, old.gapToLeader),
            timeDiffToFastest = text(driver.timeDiffToFastest, old.timeDiffToFastest),
            interval = text(driver.interval, old.interval),
            lapTime = text(driver.lapTime, old.lapTime),
            bestLapTime = text(driver.bestLapTime, old.bestLapTime),
            lastLapTime = text(driver.lastLapTime, old.lastLapTime),
            laps = driver.laps ?: old.laps,
            lapsCompleted = driver.lapsCompleted ?: old.lapsCompleted,
            inPit = driver.inPit ?: old.inPit,
            pitOut = driver.pitOut ?: old.pitOut,
            pitStops = driver.pitStops ?: old.pitStops,
            retired = driver.retired ?: old.retired,
            stopped = driver.stopped ?: old.stopped,
            knockOut = driver.knockOut ?: old.knockOut,
            headshotUrl = text(driver.getEffectiveHeadshotUrl(), old.getEffectiveHeadshotUrl()),
            totalRaceTime = text(driver.totalRaceTime, old.totalRaceTime),
            stints = if (driver.stints.isNotEmpty()) driver.stints else old.stints
        )
    }
    val omittedPrevious =
        previous.filter { driver -> key(driver)?.let { it !in currentKeys } ?: false }
    return mergedCurrent + omittedPrevious
}

/**
 * Uses the completed history classification to restore full session results that disappear
 * from the live feed after finalisation, while retaining other live fields when present.
 */
internal fun mergeFinalSessionClassification(
    current: List<TimingDriverLine>,
    classification: List<MergedHistoryClassification>
): List<TimingDriverLine> {
    val historyRows = classification.map { item ->
        TimingDriverLine(
            position = item.position,
            racingNumber = item.driverNumber,
            driverNumber = item.driverNumber,
            name = item.fullName,
            tla = item.tla,
            acronym = item.tla,
            team = item.teamName,
            teamColour = item.teamColour,
            headshotUrl = item.headshotUrl,
            gap = item.timeDiffToFastest ?: item.gapToLeader,
            gapToLeader = item.gapToLeader ?: item.timeDiffToFastest,
            interval = item.intervalToAhead,
            lapTime = item.bestLapTime,
            bestLapTime = item.bestLapTime,
            laps = item.numberOfLaps,
            lapsCompleted = item.numberOfLaps,
            totalRaceTime = item.totalRaceTime,
            stints = item.stints,
            inPit = item.inPit,
            retired = item.isRetired,
            stopped = item.stopped
        )
    }
    return mergePracticeLeaderboard(current, historyRows)
}

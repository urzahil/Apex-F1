package com.example.data.repository

import com.example.data.model.HistoryBestLap
import com.example.data.model.HistoryDriverInfo
import com.example.data.model.HistoryQualiStat
import com.example.data.model.HistoryTimingLine
import com.example.data.model.MergedHistoryClassification

internal object HistoryClassificationMapper {
    fun map(lines: Map<String, HistoryTimingLine>, driverMap: Map<String, HistoryDriverInfo>, totalRaceTime: String?): List<MergedHistoryClassification> =
        lines.map { (lineKey, line) ->
            val driverNumber = line.racingNumber?.takeIf { it.isNotBlank() } ?: lineKey
            val driverInfo = findDriver(driverNumber, driverMap)
            val position = displayPosition(line.position)
            MergedHistoryClassification(
                position = position,
                driverNumber = driverNumber,
                fullName = driverInfo?.fullName ?: driverInfo?.broadcastName ?: "Driver #$driverNumber",
                broadcastName = driverInfo?.broadcastName ?: "",
                tla = driverInfo?.tla ?: "",
                teamName = driverInfo?.teamName ?: "",
                teamColour = driverInfo?.teamColour ?: "E10600",
                headshotUrl = driverInfo?.headshotUrl,
                countryCode = driverInfo?.countryCode,
                gapToLeader = line.gapToLeader,
                intervalToAhead = line.intervalToPositionAhead?.value,
                timeDiffToFastest = line.timeDiffToFastest,
                bestLapTime = line.bestLapTime?.value,
                totalRaceTime = if (position == "1") totalRaceTime else null,
                q1Time = segmentValue(line.bestLapTimes, 0),
                q2Time = segmentValue(line.bestLapTimes, 1),
                q3Time = segmentValue(line.bestLapTimes, 2),
                q1Diff = segmentDiff(line.stats, 0),
                q2Diff = segmentDiff(line.stats, 1),
                q3Diff = segmentDiff(line.stats, 2),
                knockedOut = line.knockedOut,
                numberOfLaps = line.numberOfLaps,
                numberOfPitStops = line.numberOfPitStops,
                isRetired = line.retired == true,
                inPit = line.inPit == true,
                stopped = line.stopped == true
            )
        }.sortedBy { it.position.toIntOrNull() ?: Int.MAX_VALUE }

    private fun findDriver(racingNumber: String, driverMap: Map<String, HistoryDriverInfo>): HistoryDriverInfo? {
        val normalized = normalizeNumber(racingNumber)
        return driverMap[racingNumber] ?: driverMap.entries.firstOrNull {
            normalizeNumber(it.key) == normalized || normalizeNumber(it.value.racingNumber) == normalized
        }?.value
    }

    private fun segmentValue(values: Map<String, HistoryBestLap>?, index: Int): String? {
        val item = values?.get(index.toString()) ?: values?.get("Q" + (index + 1))
            ?: values?.entries?.firstOrNull { normalizeSegmentKey(it.key) == index }?.value
        return item?.value?.takeIf { it.isNotBlank() }
    }

    private fun segmentDiff(values: Map<String, HistoryQualiStat>?, index: Int): String? {
        val item = values?.get(index.toString()) ?: values?.get("Q" + (index + 1))
            ?: values?.entries?.firstOrNull { normalizeSegmentKey(it.key) == index }?.value
        return item?.timeDiffToFastest?.takeIf { it.isNotBlank() }
    }

    private fun normalizeSegmentKey(key: String): Int? {
        val trimmed = key.trim()
        val numeric = trimmed.removePrefix("Q").toIntOrNull() ?: return null
        return if (trimmed.startsWith("Q", ignoreCase = true)) numeric - 1 else numeric
    }

    private fun normalizeNumber(value: String?): String =
        value?.trim().orEmpty().trimStart('0').ifEmpty { "0" }

    private fun displayPosition(value: Any?): String = when (value) {
        is Number -> value.toInt().toString()
        else -> value?.toString()?.trim()?.removeSuffix(".0") ?: "-"
    }
}
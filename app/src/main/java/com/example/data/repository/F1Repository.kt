package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.api.F1ApiService
import com.example.data.model.CalendarRound
import com.example.data.model.ConstructorStanding
import com.example.data.model.DetailedResultResponse
import com.example.data.model.DriverStanding
import com.example.data.model.HistoryDriverInfo
import com.example.data.model.HistoryMeeting
import com.example.data.model.HistoryTimingLine
import com.example.data.model.ResultFileItem
import com.example.data.model.SnapshotResponse
import com.example.data.model.StatusResponse
import com.example.data.model.TimingResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

data class MergedHistoryClassification(
    val position: String,
    val driverNumber: String,
    val fullName: String,
    val broadcastName: String,
    val tla: String,
    val teamName: String,
    val teamColour: String,
    val headshotUrl: String?,
    val countryCode: String?,
    val gapToLeader: String?,
    val intervalToAhead: String?,
    val timeDiffToFastest: String?,
    val bestLapTime: String?,
    val totalRaceTime: String?,
    val q1Time: String?,
    val q2Time: String?,
    val q3Time: String?,
    val q1Diff: String?,
    val q2Diff: String?,
    val q3Diff: String?,
    val knockedOut: Boolean?,
    val numberOfLaps: Int?,
    val numberOfPitStops: Int?,
    val isRetired: Boolean,
    val inPit: Boolean,
    val stopped: Boolean
)

class F1Repository(
    private val api: F1ApiService = ApiClient.apiService
) {

    suspend fun getStatus(): Result<StatusResponse> = withContext(Dispatchers.IO) {
        runCatching { api.getStatus() }
    }

    suspend fun getSessionDrivers(): Result<List<com.example.data.model.SessionDriver>> = withContext(Dispatchers.IO) {
        runCatching { api.getSessionDrivers() }
    }

    suspend fun getSnapshot(): Result<SnapshotResponse> = withContext(Dispatchers.IO) {
        runCatching { api.getSnapshot() }
    }

    suspend fun getTiming(): Result<TimingResponse> = withContext(Dispatchers.IO) {
        runCatching { api.getTiming() }
    }

    suspend fun getCalendar(): Result<List<CalendarRound>> = withContext(Dispatchers.IO) {
        runCatching { api.getCalendar() }
    }

    suspend fun getDriverStandings(): Result<List<DriverStanding>> = withContext(Dispatchers.IO) {
        runCatching {
            val res = api.getDriverStandings()
            res.standings ?: emptyList()
        }
    }

    suspend fun getConstructorStandings(): Result<List<ConstructorStanding>> = withContext(Dispatchers.IO) {
        runCatching {
            val res = api.getConstructorStandings()
            res.standings ?: emptyList()
        }
    }

    suspend fun getResults(): Result<List<ResultFileItem>> = withContext(Dispatchers.IO) {
        runCatching { api.getResults() }
    }

    suspend fun getResultDetail(filename: String): Result<DetailedResultResponse> = withContext(Dispatchers.IO) {
        runCatching { api.getResultDetail(filename) }
    }

    suspend fun getHistoryMeetings(year: Int): Result<List<HistoryMeeting>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getHistoryYear(year)
            response.meetings ?: emptyList()
        }
    }

    suspend fun getHistorySessionClassification(path: String): Result<List<MergedHistoryClassification>> =
        withContext(Dispatchers.IO) {
            runCatching {
                coroutineScope {
                    val timingDeferred = async { runCatching { api.getHistoryTimingData(path) }.getOrNull() }
                    val driversDeferred = async { runCatching { api.getHistoryDriverList(path) }.getOrNull() }
                    val sessionDataDeferred = async {
                        if (path.contains("Race", ignoreCase = true)) {
                            runCatching { api.getHistorySessionData(path) }.getOrNull()
                        } else null
                    }

                    val timingData = timingDeferred.await()
                    val driverMap = driversDeferred.await() ?: emptyMap()
                    val sessionData = sessionDataDeferred.await()

                    // Compute total race time for winner if available
                    var totalRaceTime: String? = null
                    if (sessionData != null) {
                        val startUtc = sessionData.statusSeries?.find {
                            it.sessionStatus.equals("Started", ignoreCase = true)
                        }?.utc ?: sessionData.series?.firstOrNull()?.utc
                        val finishUtc = sessionData.series?.lastOrNull()?.utc
                        if (startUtc != null && finishUtc != null) {
                            totalRaceTime = calculateDuration(startUtc, finishUtc)
                        }
                    }

                    val lines = timingData?.lines ?: emptyMap()
                    val items = mutableListOf<MergedHistoryClassification>()

                    for ((driverNumber, line) in lines) {
                        val driverInfo = driverMap[driverNumber]
                        val posStr = line.position?.toString() ?: "-"
                        val isP1 = posStr == "1"

                        // Extract Q1, Q2, Q3 lap times
                        val q1 = line.bestLapTimes?.getOrNull(0)?.value?.takeIf { it.isNotBlank() }
                        val q2 = line.bestLapTimes?.getOrNull(1)?.value?.takeIf { it.isNotBlank() }
                        val q3 = line.bestLapTimes?.getOrNull(2)?.value?.takeIf { it.isNotBlank() }

                        // Extract Q1, Q2, Q3 gaps to leader
                        val q1Diff = line.stats?.getOrNull(0)?.timeDiffToFastest?.takeIf { it.isNotBlank() }
                        val q2Diff = line.stats?.getOrNull(1)?.timeDiffToFastest?.takeIf { it.isNotBlank() }
                        val q3Diff = line.stats?.getOrNull(2)?.timeDiffToFastest?.takeIf { it.isNotBlank() }

                        items.add(
                            MergedHistoryClassification(
                                position = posStr,
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
                                totalRaceTime = if (isP1) totalRaceTime else null,
                                q1Time = q1,
                                q2Time = q2,
                                q3Time = q3,
                                q1Diff = q1Diff,
                                q2Diff = q2Diff,
                                q3Diff = q3Diff,
                                knockedOut = line.knockedOut,
                                numberOfLaps = line.numberOfLaps,
                                numberOfPitStops = line.numberOfPitStops,
                                isRetired = line.retired == true,
                                inPit = line.inPit == true,
                                stopped = line.stopped == true
                            )
                        )
                    }

                    items.sortedBy { item ->
                        item.position.toIntOrNull() ?: 999
                    }
                }
            }
        }

    private fun calculateDuration(startUtc: String, endUtc: String): String? {
        return try {
            val start = Instant.parse(startUtc)
            val end = Instant.parse(endUtc)
            val diff = Duration.between(start, end)
            val hours = diff.toHours()
            val minutes = diff.toMinutesPart()
            val seconds = diff.toSecondsPart()
            val millis = diff.toMillisPart()
            if (hours > 0) {
                String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, millis)
            } else {
                String.format("%02d:%02d.%03d", minutes, seconds, millis)
            }
        } catch (_: Exception) {
            null
        }
    }
}

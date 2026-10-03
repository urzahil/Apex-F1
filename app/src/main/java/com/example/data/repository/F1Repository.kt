package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.api.F1ApiService
import com.example.data.model.CalendarRound
import com.example.data.model.ConstructorStanding
import com.example.data.model.DetailedResultResponse
import com.example.data.model.DriverStanding
import com.example.data.model.HistoryMeeting
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import com.squareup.moshi.Types

ackage com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.api.F1ApiService
import com.example.data.model.CalendarRound
import com.example.data.model.ConstructorStanding
import com.example.data.model.DetailedResultResponse
import com.example.data.model.DriverStanding
import com.example.data.model.HistoryMeeting
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import com.squareup.moshi.Types

data class MergedHistoryClassification(
    val position: String, val driverNumber: String, val fullName: String, val broadcastName: String, val tla: String,
    val teamName: String, val teamColour: String, val headshotUrl: String?, val countryCode: String?,
    val gapToLeader: String?, val intervalToAhead: String?, val timeDiffToFastest: String?, val bestLapTime: String?,
    val totalRaceTime: String?, val q1Time: String?, val q2Time: String?, val q3Time: String?,
    val q1Diff: String?, val q2Diff: String?, val q3Diff: String?, val knockedOut: Boolean?,
    val numberOfLaps: Int?, val numberOfPitStops: Int?, val isRetired: Boolean, val inPit: Boolean, val stopped: Boolean
)

class F1Repository(private val api: F1ApiService = ApiClient.apiService, context: Context? = null) {\n    private val historyDao = context?.let { ApexDatabase.get(it).historyClassificationDao() }
    private val historyClassificationCache = ConcurrentHashMap<String, List<MergedHistoryClassification>>()
    private val historyYearsCache = AtomicReference<List<Int>?>(null)\n    private val classificationListType = Types.newParameterizedType(List::class.java, com.example.data.model.MergedHistoryClassification::class.java)\n    private val classificationAdapter = ApiClient.moshi.adapter<List<com.example.data.model.MergedHistoryClassification>>(classificationListType)

    suspend fun getStatus(): Result<StatusResponse> = withContext(Dispatchers.IO) { runCatching { api.getStatus() } }
    suspend fun getSessionDrivers(): Result<List<com.example.data.model.SessionDriver>> = withContext(Dispatchers.IO) { runCatching { api.getSessionDrivers() } }
    suspend fun getSnapshot(): Result<SnapshotResponse> = withContext(Dispatchers.IO) { runCatching { api.getSnapshot() } }
    suspend fun getTiming(): Result<TimingResponse> = withContext(Dispatchers.IO) { runCatching { api.getTiming() } }
    suspend fun getCalendar(): Result<List<CalendarRound>> = withContext(Dispatchers.IO) { runCatching { api.getCalendar() } }
    suspend fun getDriverStandings(): Result<List<DriverStanding>> = withContext(Dispatchers.IO) { runCatching { api.getDriverStandings().standings.orEmpty() } }
    suspend fun getConstructorStandings(): Result<List<ConstructorStanding>> = withContext(Dispatchers.IO) { runCatching { api.getConstructorStandings().standings.orEmpty() } }
    suspend fun getResults(): Result<List<ResultFileItem>> = withContext(Dispatchers.IO) { runCatching { api.getResults() } }
    suspend fun getResultDetail(filename: String): Result<DetailedResultResponse> = withContext(Dispatchers.IO) { runCatching { api.getResultDetail(filename) } }

    suspend fun getHistoryYears(): Result<List<Int>> = withContext(Dispatchers.IO) {
        historyYearsCache.get()?.let { return@withContext Result.success(it) }
        runCatching {
            api.getHistoryYears().years.orEmpty().filter { it > 0 }.distinct().sortedDescending().also { historyYearsCache.set(it) }
        }
    }

    suspend fun getHistoryMeetings(year: Int): Result<List<HistoryMeeting>> = withContext(Dispatchers.IO) {
        runCatching { api.getHistoryYear(year).meetings.orEmpty() }
    }

    suspend fun getHistorySessionClassification(path: String): Result<List<MergedHistoryClassification>> = withContext(Dispatchers.IO) {
        historyClassificationCache[path]?.let { return@withContext Result.success(it) }\n        historyDao?.get(path)?.let { entity ->\n            runCatching { classificationAdapter.fromJson(entity.payload) }.getOrNull()?.let { cached ->\n                historyClassificationCache[path] = cached\n                return@withContext Result.success(cached)\n            }\n        }
        runCatching {
            coroutineScope {
                val timingDeferred = async { runCatching { api.getHistoryTimingData(path) }.getOrNull() }
                val driversDeferred = async { runCatching { api.getHistoryDriverList(path) }.getOrNull() }
                val sessionDataDeferred = async {
                    if (path.contains("Race", ignoreCase = true)) runCatching { api.getHistorySessionData(path) }.getOrNull() else null
                }
                val timingData = timingDeferred.await()
                val driverMap = driversDeferred.await().orEmpty()
                val sessionData = sessionDataDeferred.await()

                val totalRaceTime = sessionData?.let {
                    val startUtc = it.statusSeries?.find { s -> s.sessionStatus.equals("Started", true) }?.utc ?: it.series?.firstOrNull()?.utc
                    val finishUtc = it.series?.lastOrNull()?.utc
                    if (startUtc != null && finishUtc != null) calculateDuration(startUtc, finishUtc) else null
                }

                HistoryClassificationMapper.map(timingData?.lines.orEmpty(), driverMap, totalRaceTime).also {
                    historyClassificationCache[path] = it
                }
            }
        }
    }

    private fun calculateDuration(startUtc: String, endUtc: String): String? = try {
        val diff = Duration.between(Instant.parse(startUtc), Instant.parse(endUtc))
        val hours = diff.toHours()
        val minutes = diff.toMinutesPart()
        val seconds = diff.toSecondsPart()
        val millis = diff.toMillisPart()
        if (hours > 0) String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, millis)
        else String.format("%02d:%02d.%03d", minutes, seconds, millis)
    } catch (_: Exception) { null }
}
package com.example.data.repository

import com.example.ApexApplication
import com.example.data.api.ApiClient
import com.example.data.api.F1ApiService
import com.example.data.cache.ApiCache
import com.example.data.cache.ApexDatabase
import com.example.data.model.*
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

data class MergedHistoryClassification(
    val position: String, val driverNumber: String, val fullName: String, val broadcastName: String,
    val tla: String, val teamName: String, val teamColour: String, val headshotUrl: String?,
    val countryCode: String?, val gapToLeader: String?, val intervalToAhead: String?,
    val timeDiffToFastest: String?, val bestLapTime: String?, val totalRaceTime: String?,
    val q1Time: String?, val q2Time: String?, val q3Time: String?, val q1Diff: String?,
    val q2Diff: String?, val q3Diff: String?, val knockedOut: Boolean?, val numberOfLaps: Int?,
    val numberOfPitStops: Int?, val isRetired: Boolean, val inPit: Boolean, val stopped: Boolean
)

class F1Repository(
    private val api: F1ApiService = ApiClient.apiService,
    private val cache: ApiCache? = defaultCache()
) {
    private companion object {
        const val CALENDAR_TTL = 6 * 60 * 60 * 1000L
        const val HISTORY_TTL = 7 * 24 * 60 * 60 * 1000L
        const val STANDINGS_TTL = 5 * 60 * 1000L
        const val RESULTS_TTL = 24 * 60 * 60 * 1000L
        fun defaultCache(): ApiCache? = runCatching {
            ApiCache(ApexDatabase.getInstance(ApexApplication.instance).apiCacheDao())
        }.getOrNull()
    }

    suspend fun getStatus(): Result<StatusResponse> = withContext(Dispatchers.IO) { runCatching { api.getStatus() } }
    suspend fun getSessionDrivers(): Result<List<SessionDriver>> = withContext(Dispatchers.IO) { runCatching { api.getSessionDrivers() } }
    suspend fun getSnapshot(): Result<SnapshotResponse> = withContext(Dispatchers.IO) { runCatching { api.getSnapshot() } }
    suspend fun getTiming(): Result<TimingResponse> = withContext(Dispatchers.IO) { runCatching { api.getTiming() } }

    suspend fun getCalendar(forceRefresh: Boolean = false): Result<List<CalendarRound>> = withContext(Dispatchers.IO) {
        runCatching {
            val type = Types.newParameterizedType(List::class.java, CalendarRound::class.java)
            if (!forceRefresh) cache?.read<List<CalendarRound>>("calendar", type, CALENDAR_TTL)?.let { return@runCatching it }
            api.getCalendar().also { cache?.write("calendar", it, type) }
        }.recoverCatching {
            val type = Types.newParameterizedType(List::class.java, CalendarRound::class.java)
            cache?.read<List<CalendarRound>>("calendar", type, Long.MAX_VALUE)?.getOrThrow() ?: throw it
        }
    }

    suspend fun getDriverStandings(): Result<List<DriverStanding>> = withContext(Dispatchers.IO) {
        runCatching {
            val type = Types.newParameterizedType(List::class.java, DriverStanding::class.java)
            cache?.read<List<DriverStanding>>("standings_drivers", type, STANDINGS_TTL)?.let { return@runCatching it }
            api.getDriverStandings().standings.orEmpty().also { cache?.write("standings_drivers", it, type) }
        }
    }

    suspend fun getConstructorStandings(): Result<List<ConstructorStanding>> = withContext(Dispatchers.IO) {
        runCatching {
            val type = Types.newParameterizedType(List::class.java, ConstructorStanding::class.java)
            cache?.read<List<ConstructorStanding>>("standings_constructors", type, STANDINGS_TTL)?.let { return@runCatching it }
            api.getConstructorStandings().standings.orEmpty().also { cache?.write("standings_constructors", it, type) }
        }
    }

    suspend fun getResults(): Result<List<ResultFileItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val type = Types.newParameterizedType(List::class.java, ResultFileItem::class.java)
            cache?.read<List<ResultFileItem>>("results", type, RESULTS_TTL)?.let { return@runCatching it }
            api.getResults().also { cache?.write("results", it, type) }
        }
    }

    suspend fun getResultDetail(filename: String): Result<DetailedResultResponse> =
        withContext(Dispatchers.IO) { runCatching { api.getResultDetail(filename) } }

    suspend fun getHistoryMeetings(year: Int): Result<List<HistoryMeeting>> = withContext(Dispatchers.IO) {
        runCatching {
            val type = Types.newParameterizedType(List::class.java, HistoryMeeting::class.java)
            val key = "history_year_" + year
            cache?.read<List<HistoryMeeting>>(key, type, HISTORY_TTL)?.let { return@runCatching it }
            api.getHistoryYear(year).meetings.orEmpty().also { cache?.write(key, it, type) }
        }
    }

    suspend fun getHistorySessionClassification(path: String, sessionType: String? = null): Result<List<MergedHistoryClassification>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val key = "history_session_" + path.hashCode()
                val type = Types.newParameterizedType(List::class.java, MergedHistoryClassification::class.java)
                cache?.read<List<MergedHistoryClassification>>(key, type, HISTORY_TTL)?.let { return@runCatching it }

                coroutineScope {
                    val timing = async { runCatching { api.getHistoryTimingData(path) }.getOrNull() }
                    val drivers = async { runCatching { api.getHistoryDriverList(path) }.getOrNull() }
                    val sessionData = async {
                        if (sessionType?.contains("Race", ignoreCase = true) == true) {
                            runCatching { api.getHistorySessionData(path) }.getOrNull()
                        } else null
                    }
                    val timingData = timing.await()
                    val driverMap = drivers.await().orEmpty()
                    val sd = sessionData.await()

                    val totalRaceTime = sd?.let {
                        val start = it.statusSeries?.firstOrNull { s -> s.sessionStatus.equals("Started", true) }?.utc
                            ?: it.series?.firstOrNull()?.utc
                        val finish = it.statusSeries?.firstOrNull { s -> s.sessionStatus.equals("Finished", true) }?.utc
                            ?: it.series?.lastOrNull()?.utc
                        if (start != null && finish != null) calculateDuration(start, finish) else null
                    }

                    val items = timingData?.lines.orEmpty().map { (driverNumber, line) ->
                        val info = driverMap[driverNumber]
                        val pos = line.position?.toString() ?: "-"
                        MergedHistoryClassification(
                            position = pos, driverNumber = driverNumber,
                            fullName = info?.fullName ?: info?.broadcastName ?: "Driver #$driverNumber",
                            broadcastName = info?.broadcastName.orEmpty(), tla = info?.tla.orEmpty(),
                            teamName = info?.teamName.orEmpty(), teamColour = info?.teamColour ?: "E10600",
                            headshotUrl = info?.headshotUrl, countryCode = info?.countryCode,
                            gapToLeader = line.gapToLeader, intervalToAhead = line.intervalToPositionAhead?.value,
                            timeDiffToFastest = line.timeDiffToFastest, bestLapTime = line.bestLapTime?.value,
                            totalRaceTime = if (pos == "1") totalRaceTime else null,
                            q1Time = line.bestLapTimes?.getOrNull(0)?.value,
                            q2Time = line.bestLapTimes?.getOrNull(1)?.value,
                            q3Time = line.bestLapTimes?.getOrNull(2)?.value,
                            q1Diff = line.stats?.getOrNull(0)?.timeDiffToFastest,
                            q2Diff = line.stats?.getOrNull(1)?.timeDiffToFastest,
                            q3Diff = line.stats?.getOrNull(2)?.timeDiffToFastest,
                            knockedOut = line.knockedOut, numberOfLaps = line.numberOfLaps,
                            numberOfPitStops = line.numberOfPitStops, isRetired = line.retired == true,
                            inPit = line.inPit == true, stopped = line.stopped == true
                        )
                    }.sortedBy { it.position.toIntOrNull() ?: 999 }
                    cache?.write(key, items, type)
                    items
                }
            }
        }

    private fun calculateDuration(startUtc: String, endUtc: String): String? = runCatching {
        val diff = Duration.between(Instant.parse(startUtc), Instant.parse(endUtc))
        val h = diff.toHours(); val m = diff.toMinutesPart(); val s = diff.toSecondsPart(); val ms = diff.toMillisPart()
        if (h > 0) String.format("%d:%02d:%02d.%03d", h, m, s, ms)
        else String.format("%02d:%02d.%03d", m, s, ms)
    }.getOrNull()
}

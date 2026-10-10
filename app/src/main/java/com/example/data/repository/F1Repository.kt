package com.example.data.repository

import com.example.ApexApplication
import com.example.data.api.ApiClient
import com.example.data.api.F1ApiService
import com.example.data.cache.ApexDatabase
import com.example.data.cache.ApiCache
import com.example.data.model.CalendarRound
import com.example.data.model.ConstructorStanding
import com.example.data.model.DetailedResultResponse
import com.example.data.model.DriverStanding
import com.example.data.model.HistoryMeeting
import com.example.data.model.ResultFileItem
import com.example.data.model.SessionDriver
import com.example.data.model.SnapshotResponse
import com.example.data.model.StatusResponse
import com.example.data.model.TimingResponse
import com.example.data.model.TyreStint
import com.squareup.moshi.Types
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Duration
import java.time.Instant

suspend inline fun <T> apiCall(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

suspend inline fun <T> Result<T>.recoverApi(crossinline block: suspend (Throwable) -> T): Result<T> {
    if (isSuccess) return this
    return try {
        Result.success(block(exceptionOrNull() ?: IOException("Unknown API failure")))
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        this
    }
}

data class MergedHistoryClassification(
    val position: String, val driverNumber: String, val fullName: String, val broadcastName: String,
    val tla: String, val teamName: String, val teamColour: String, val headshotUrl: String?,
    val countryCode: String?, val gapToLeader: String?, val intervalToAhead: String?,
    val timeDiffToFastest: String?, val bestLapTime: String?, val totalRaceTime: String?,
    val q1Time: String?, val q2Time: String?, val q3Time: String?, val q1Diff: String?,
    val q2Diff: String?, val q3Diff: String?, val knockedOut: Boolean?, val numberOfLaps: Int?,
    val numberOfPitStops: Int?, val isRetired: Boolean, val inPit: Boolean, val stopped: Boolean,
    val stints: Map<String, TyreStint> = emptyMap()
)

class F1Repository(
    private val api: F1ApiService = ApiClient.apiService,
    private val cache: ApiCache? = defaultCache()
) {
    private companion object {
        // const val CALENDAR_TTL = 6 * 60 * 60 * 1000L
        const val CALENDAR_TTL = 60 * 60 * 1000L
        const val HISTORY_TTL = 24 * 60 * 60 * 1000L
        const val STANDINGS_TTL = 5 * 60 * 1000L
        // const val RESULTS_TTL = 24 * 60 * 60 * 1000L
        const val RESULTS_TTL = 60 * 60 * 1000L
        fun defaultCache(): ApiCache? = runCatching {
            ApiCache(ApexDatabase.getInstance(ApexApplication.instance).apiCacheDao())
        }.getOrNull()
    }

    suspend fun getStatus(): Result<StatusResponse> =
        withContext(Dispatchers.IO) { apiCall { api.getStatus() } }

    suspend fun getSessionDrivers(): Result<List<SessionDriver>> =
        withContext(Dispatchers.IO) { apiCall { api.getSessionDrivers() } }

    suspend fun getSnapshot(): Result<SnapshotResponse> =
        withContext(Dispatchers.IO) { apiCall { api.getSnapshot() } }

    suspend fun getTiming(): Result<TimingResponse> =
        withContext(Dispatchers.IO) { apiCall { api.getTiming() } }

    suspend fun getCalendar(
        forceRefresh: Boolean = false,
        allowCacheFallback: Boolean = true,
        cacheResults: Boolean = true
    ): Result<List<CalendarRound>> =
        withContext(Dispatchers.IO) {
            apiCall {
                val type = Types.newParameterizedType(List::class.java, CalendarRound::class.java)
                if (!forceRefresh && cacheResults) cache?.read<List<CalendarRound>>("calendar", type, CALENDAR_TTL)
                    ?.let { return@apiCall it }
                api.getCalendar().also { if (cacheResults) cache?.write("calendar", it, type) }
            }.recoverApi { error ->
                val type = Types.newParameterizedType(List::class.java, CalendarRound::class.java)
                if (allowCacheFallback && cacheResults) {
                    cache?.read<List<CalendarRound>>("calendar", type, Long.MAX_VALUE) ?: throw error
                } else throw error
            }
        }

    suspend fun getDriverStandings(forceRefresh: Boolean = false): Result<List<DriverStanding>> = withContext(Dispatchers.IO) {
        apiCall {
            val type = Types.newParameterizedType(List::class.java, DriverStanding::class.java)
            if (!forceRefresh) cache?.read<List<DriverStanding>>("standings_drivers", type, STANDINGS_TTL)
                ?.let { return@apiCall it }
            api.getDriverStandings().standings.orEmpty()
                .also { cache?.write("standings_drivers", it, type) }
        }.recoverApi { error ->
            val type = Types.newParameterizedType(List::class.java, DriverStanding::class.java)
            cache?.read<List<DriverStanding>>("standings_drivers", type, Long.MAX_VALUE)
                ?: throw error
        }
    }

    suspend fun getConstructorStandings(forceRefresh: Boolean = false): Result<List<ConstructorStanding>> =
        withContext(Dispatchers.IO) {
            apiCall {
                val type =
                    Types.newParameterizedType(List::class.java, ConstructorStanding::class.java)
                if (!forceRefresh) cache?.read<List<ConstructorStanding>>(
                    "standings_constructors",
                    type,
                    STANDINGS_TTL
                )?.let { return@apiCall it }
                api.getConstructorStandings().standings.orEmpty()
                    .also { cache?.write("standings_constructors", it, type) }
            }.recoverApi { error ->
                val type =
                    Types.newParameterizedType(List::class.java, ConstructorStanding::class.java)
                cache?.read<List<ConstructorStanding>>(
                    "standings_constructors",
                    type,
                    Long.MAX_VALUE
                ) ?: throw error
            }
        }

    suspend fun getResults(): Result<List<ResultFileItem>> = withContext(Dispatchers.IO) {
        apiCall {
            val type = Types.newParameterizedType(List::class.java, ResultFileItem::class.java)
            cache?.read<List<ResultFileItem>>("results", type, RESULTS_TTL)
                ?.let { return@apiCall it }
            api.getResults().also { cache?.write("results", it, type) }
        }.recoverApi { error ->
            val type = Types.newParameterizedType(List::class.java, ResultFileItem::class.java)
            cache?.read<List<ResultFileItem>>("results", type, Long.MAX_VALUE) ?: throw error
        }
    }

    suspend fun getResultDetail(filename: String): Result<DetailedResultResponse> =
        withContext(Dispatchers.IO) { apiCall { api.getResultDetail(filename) } }

    suspend fun getHistoryMeetings(year: Int, forceRefresh: Boolean = false): Result<List<HistoryMeeting>> =
        withContext(Dispatchers.IO) {
            apiCall {
                val type = Types.newParameterizedType(List::class.java, HistoryMeeting::class.java)
                val key = "history_year_" + year
                if (!forceRefresh) cache?.read<List<HistoryMeeting>>(key, type, HISTORY_TTL)?.let { return@apiCall it }
                api.getHistoryYear(year).meetings.orEmpty().also { cache?.write(key, it, type) }
            }.recoverApi { error ->
                val type = Types.newParameterizedType(List::class.java, HistoryMeeting::class.java)
                cache?.read<List<HistoryMeeting>>("history_year_" + year, type, Long.MAX_VALUE)
                    ?: throw error
            }
        }

    suspend fun getHistorySessionClassification(
        path: String,
        sessionType: String? = null,
        forceRefresh: Boolean = false,
        allowCacheFallback: Boolean = true,
        cacheResults: Boolean = true,
        cacheClassificationWithoutTyreData: Boolean = false
    ): Result<List<MergedHistoryClassification>> =
        withContext(Dispatchers.IO) {
            apiCall {
                val key = "history_session_v3_" + path
                val type = Types.newParameterizedType(
                    List::class.java,
                    MergedHistoryClassification::class.java
                )
                if (!forceRefresh && cacheResults) cache?.read<List<MergedHistoryClassification>>(key, type, HISTORY_TTL)
                    ?.let { return@apiCall it }

                coroutineScope {
                    val timing = async { apiCall { api.getHistoryTimingData(path) } }
                    val drivers = async { apiCall { api.getHistoryDriverList(path) } }
                    val sessionData = async {
                        if (sessionType?.let {
                                it.contains(
                                    "Race",
                                    ignoreCase = true
                                ) || it.contains("Sprint", ignoreCase = true)
                            } == true) {
                            apiCall { api.getHistorySessionData(path) }.getOrNull()
                        } else null
                    }
                    val timingData = timing.await().getOrNull()
                    val driverMap = drivers.await().getOrNull().orEmpty()
                    val sd = sessionData.await()

                    val totalRaceTime = sd?.let {
                        val start = it.statusSeries?.firstOrNull { s ->
                            s.sessionStatus.equals(
                                "Started",
                                true
                            )
                        }?.utc
                            ?: it.series?.firstOrNull()?.utc
                        val finish = it.statusSeries?.firstOrNull { s ->
                            s.sessionStatus.equals(
                                "Finished",
                                true
                            )
                        }?.utc
                            ?: it.series?.lastOrNull()?.utc
                        if (start != null && finish != null) calculateDuration(
                            start,
                            finish
                        ) else null
                    }

                    val lines = timingData?.lines
                        ?: throw IOException("History timing data was unavailable")
                    val items = lines.map { (driverNumber, line) ->
                        val info = driverMap[driverNumber]
                        val pos = normalizePosition(line.position)
                        MergedHistoryClassification(
                            position = pos,
                            driverNumber = driverNumber,
                            fullName = info?.fullName ?: info?.broadcastName
                            ?: "Driver #$driverNumber",
                            broadcastName = info?.broadcastName.orEmpty(),
                            tla = info?.tla.orEmpty(),
                            teamName = info?.teamName.orEmpty(),
                            teamColour = info?.teamColour ?: "E10600",
                            headshotUrl = info?.headshotUrl,
                            countryCode = info?.countryCode,
                            gapToLeader = line.gapToLeader ?: line.timeDiffToFastest,
                            intervalToAhead = line.intervalToPositionAhead?.value,
                            timeDiffToFastest = line.timeDiffToFastest,
                            bestLapTime = line.bestLapTime?.value,
                            totalRaceTime = if (pos == "1") totalRaceTime else null,
                            q1Time = line.bestLapTimes?.getOrNull(0)?.value,
                            q2Time = line.bestLapTimes?.getOrNull(1)?.value,
                            q3Time = line.bestLapTimes?.getOrNull(2)?.value,
                            q1Diff = line.stats?.getOrNull(0)?.timeDiffToFastest,
                            q2Diff = line.stats?.getOrNull(1)?.timeDiffToFastest,
                            q3Diff = line.stats?.getOrNull(2)?.timeDiffToFastest,
                            knockedOut = line.knockedOut,
                            numberOfLaps = line.numberOfLaps,
                            numberOfPitStops = line.numberOfPitStops,
                            isRetired = line.retired == true,
                            inPit = line.inPit == true,
                            stopped = line.stopped == true,
                            stints = line.stints
                        )
                    }.sortedBy { it.position.toIntOrNull() ?: 999 }
                    if (items.isEmpty()) throw IOException("History classification was empty")
                    // Do not cache a successful classification as the tyre source unless
                    // at least one driver actually contains stint data. The history endpoint
                    // may return valid classification rows while omitting Stints temporarily.
                    val hasTyreData = items.any { it.stints.isNotEmpty() }
                    if (cacheResults && (hasTyreData || cacheClassificationWithoutTyreData)) {
                        cache?.write(key, items, type)
                    }
                    items
                }
            }.recoverApi { error ->
                val type = Types.newParameterizedType(
                    List::class.java,
                    MergedHistoryClassification::class.java
                )
                if (allowCacheFallback && cacheResults) {
                    cache?.read<List<MergedHistoryClassification>>(
                        "history_session_v3_" + path,
                        type,
                        Long.MAX_VALUE
                    ) ?: throw error
                } else throw error
            }
        }

    private fun normalizePosition(position: Any?): String = when (position) {
        null -> "-"
        is Number -> position.toDouble()
            .let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }

        is String -> position.toDoubleOrNull()
            ?.let { if (it % 1.0 == 0.0) it.toInt().toString() else position } ?: position

        else -> position.toString()
    }

    private fun calculateDuration(startUtc: String, endUtc: String): String? = runCatching {
        val diff = Duration.between(Instant.parse(startUtc), Instant.parse(endUtc))
        val h = diff.toHours();
        val m = diff.toMinutesPart();
        val s = diff.toSecondsPart();
        val ms = diff.toMillisPart()
        if (h > 0) String.format("%d:%02d:%02d.%03d", h, m, s, ms)
        else String.format("%02d:%02d.%03d", m, s, ms)
    }.getOrNull()
}

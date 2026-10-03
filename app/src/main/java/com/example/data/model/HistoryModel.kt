package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HistorySeasonListResponse(
    @Json(name = "description") val description: String? = null,
    @Json(name = "years") val years: List<Int>? = null
)

@JsonClass(generateAdapter = true)
data class HistoryYearResponse(
    @Json(name = "year") val year: Any? = null,
    @Json(name = "total") val total: Int? = null,
    @Json(name = "meetings") val meetings: List<HistoryMeeting>? = null
)

@JsonClass(generateAdapter = true)
data class HistoryMeeting(
    @Json(name = "key") val key: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "path") val path: String? = null,
    @Json(name = "sessions") val sessions: List<HistorySession>? = null
)

@JsonClass(generateAdapter = true)
data class HistorySession(
    @Json(name = "key") val key: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "path") val path: String? = null,
    @Json(name = "start_date") val startDate: String? = null,
    @Json(name = "end_date") val endDate: String? = null
)

@JsonClass(generateAdapter = true)
data class HistoryTimingDataResponse(
    @Json(name = "Lines") val lines: Map<String, HistoryTimingLine>? = null
)

@JsonClass(generateAdapter = true)
data class HistoryTimingLine(
    @Json(name = "Position") val position: Any? = null,
    @Json(name = "RacingNumber") val racingNumber: String? = null,
    @Json(name = "GapToLeader") val gapToLeader: String? = null,
    @Json(name = "IntervalToPositionAhead") val intervalToPositionAhead: HistoryInterval? = null,
    @Json(name = "TimeDiffToFastest") val timeDiffToFastest: String? = null,
    @Json(name = "Stats") val stats: List<HistoryQualiStat>? = null,
    @Json(name = "NumberOfLaps") val numberOfLaps: Int? = null,
    @Json(name = "NumberOfPitStops") val numberOfPitStops: Int? = null,
    @Json(name = "Retired") val retired: Boolean? = null,
    @Json(name = "InPit") val inPit: Boolean? = null,
    @Json(name = "Stopped") val stopped: Boolean? = null,
    @Json(name = "BestLapTime") val bestLapTime: HistoryBestLap? = null,
    @Json(name = "LastLapTime") val lastLapTime: HistoryBestLap? = null,
    @Json(name = "BestLapTimes") val bestLapTimes: List<HistoryBestLap>? = null,
    @Json(name = "KnockedOut") val knockedOut: Boolean? = null
) {
    fun displayPosition(): String = position?.toString() ?: "-"
}

@JsonClass(generateAdapter = true)
data class HistoryQualiStat(
    @Json(name = "TimeDiffToFastest") val timeDiffToFastest: String? = null,
    @Json(name = "TimeDifftoPositionAhead") val timeDiffToPositionAhead: String? = null
)

@JsonClass(generateAdapter = true)
data class HistorySessionDataResponse(
    @Json(name = "Series") val series: List<HistoryLapSeriesItem>? = null,
    @Json(name = "StatusSeries") val statusSeries: List<HistoryStatusSeriesItem>? = null
)

@JsonClass(generateAdapter = true)
data class HistoryLapSeriesItem(
    @Json(name = "Utc") val utc: String? = null,
    @Json(name = "Lap") val lap: Int? = null
)

@JsonClass(generateAdapter = true)
data class HistoryStatusSeriesItem(
    @Json(name = "Utc") val utc: String? = null,
    @Json(name = "SessionStatus") val sessionStatus: String? = null,
    @Json(name = "TrackStatus") val trackStatus: String? = null
)

@JsonClass(generateAdapter = true)
data class HistoryInterval(
    @Json(name = "Value") val value: String? = null,
    @Json(name = "Catching") val catching: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class HistoryBestLap(
    @Json(name = "Value") val value: String? = null,
    @Json(name = "Lap") val lap: Int? = null
)

@JsonClass(generateAdapter = true)
data class HistoryDriverInfo(
    @Json(name = "RacingNumber") val racingNumber: String? = null,
    @Json(name = "BroadcastName") val broadcastName: String? = null,
    @Json(name = "FullName") val fullName: String? = null,
    @Json(name = "FirstName") val firstName: String? = null,
    @Json(name = "LastName") val lastName: String? = null,
    @Json(name = "Tla") val tla: String? = null,
    @Json(name = "TeamName") val teamName: String? = null,
    @Json(name = "TeamColour") val teamColour: String? = null,
    @Json(name = "HeadshotUrl") val headshotUrl: String? = null,
    @Json(name = "CountryCode") val countryCode: String? = null
)

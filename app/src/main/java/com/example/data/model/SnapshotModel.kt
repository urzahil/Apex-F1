package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LapCountData(
    @Json(name = "CurrentLap") val currentLap: Any? = null,
    @Json(name = "TotalLaps") val totalLaps: Any? = null,
    @Json(name = "_kf") val kf: Boolean? = null
) {
    fun getCurrentLapInt(): Int? {
        return when (val v = currentLap) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull()
            else -> null
        }
    }

    fun getTotalLapsInt(): Int? {
        return when (val v = totalLaps) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull()
            else -> null
        }
    }
}

@JsonClass(generateAdapter = true)
data class SnapshotResponse(
    @Json(name = "weather") val weather: WeatherInfo? = null,
    @Json(name = "track_status") val trackStatus: TrackStatusInfo? = null,
    @Json(name = "race_control") val raceControl: RaceControlData? = null,
    @Json(name = "top_three") val topThree: TopThreeData? = null,
    @Json(name = "clock") val clock: ClockInfo? = null,
    @Json(name = "timing") val timing: SnapshotTimingData? = null,
    @Json(name = "timing_app") val timingApp: SnapshotTimingData? = null,
    @Json(name = "lap_count") val lapCount: LapCountData? = null
)

data class SnapshotTimingData(
    val lines: List<SnapshotTimingDriverLine> = emptyList()
)

data class SnapshotTimingDriverLine(
    val racingNumber: String,
    val position: String? = null,
    val bestLapTime: String? = null,
    val lastLapTime: String? = null,
    val gapToLeader: String? = null,
    val intervalToAhead: String? = null,
    val inPit: Boolean = false,
    val pitOut: Boolean = false,
    val retired: Boolean = false,
    val stopped: Boolean = false,
    val knockedOut: Boolean = false,
    val stints: List<TyreStint> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WeatherInfo(
    @Json(name = "AirTemp") val airTemp: String? = null,
    @Json(name = "TrackTemp") val trackTemp: String? = null,
    @Json(name = "Humidity") val humidity: String? = null,
    @Json(name = "Pressure") val pressure: String? = null,
    @Json(name = "Rainfall") val rainfall: String? = null,
    @Json(name = "WindSpeed") val windSpeed: String? = null,
    @Json(name = "WindDirection") val windDirection: String? = null
)

@JsonClass(generateAdapter = true)
data class RaceControlData(
    @Json(name = "Messages") val messages: List<RaceControlMessage>? = null
)

@JsonClass(generateAdapter = true)
data class RaceControlMessage(
    @Json(name = "Utc") val utc: String? = null,
    @Json(name = "Category") val category: String? = null,
    @Json(name = "Flag") val flag: String? = null,
    @Json(name = "Scope") val scope: String? = null,
    @Json(name = "Sector") val sector: Int? = null,
    @Json(name = "Message") val message: String? = null,
    @Json(name = "RacingNumber") val racingNumber: String? = null
)

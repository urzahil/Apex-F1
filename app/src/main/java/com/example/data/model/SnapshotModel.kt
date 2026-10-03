package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SnapshotResponse(
    @Json(name = "weather") val weather: WeatherInfo? = null,
    @Json(name = "track_status") val trackStatus: TrackStatusInfo? = null,
    @Json(name = "race_control") val raceControl: RaceControlData? = null,
    @Json(name = "top_three") val topThree: TopThreeData? = null,
    @Json(name = "clock") val clock: ClockInfo? = null,
    @Json(name = "timing") val timing: SnapshotTimingData? = null
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
    val knockedOut: Boolean = false
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

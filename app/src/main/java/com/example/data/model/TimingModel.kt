package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TimingResponse(
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "session") val session: String? = null,
    @Json(name = "live") val live: Boolean? = null,
    @Json(name = "stale") val stale: Boolean? = null,
    @Json(name = "stale_reason") val staleReason: String? = null,
    @Json(name = "drivers") val drivers: List<TimingDriverLine>? = null
)

@JsonClass(generateAdapter = true)
data class TimingDriverLine(
    @Json(name = "position") val position: Any? = null,
    @Json(name = "racing_number") val racingNumber: String? = null,
    @Json(name = "driver_number") val driverNumber: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "tla") val tla: String? = null,
    @Json(name = "team") val team: String? = null,
    @Json(name = "team_colour") val teamColour: String? = null,
    @Json(name = "gap") val gap: String? = null,
    @Json(name = "interval") val interval: String? = null,
    @Json(name = "lap_time") val lapTime: String? = null,
    @Json(name = "laps") val laps: Int? = null,
    @Json(name = "in_pit") val inPit: Boolean? = null,
    @Json(name = "pit_stops") val pitStops: Int? = null,
    @Json(name = "retired") val retired: Boolean? = null,
    @Json(name = "stopped") val stopped: Boolean? = null
) {
    fun getDisplayPosition(): String {
        return position?.toString() ?: "-"
    }
    fun getDisplayNumber(): String {
        return driverNumber ?: racingNumber ?: "-"
    }
}

package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResultFileItem(
    @Json(name = "filename") val filename: String? = null,
    @Json(name = "round") val round: Int? = null,
    @Json(name = "meeting") val meeting: String? = null,
    @Json(name = "session_name") val sessionName: String? = null,
    @Json(name = "session_type") val sessionType: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "circuit") val circuit: String? = null
)

@JsonClass(generateAdapter = true)
data class DetailedResultResponse(
    @Json(name = "meta") val meta: ResultMeta? = null,
    @Json(name = "fastest_lap") val fastestLap: FastestLapInfo? = null,
    @Json(name = "drivers") val drivers: Map<String, ResultDriverMeta>? = null,
    @Json(name = "results") val results: List<ResultClassificationItem>? = null
)

@JsonClass(generateAdapter = true)
data class ResultMeta(
    @Json(name = "year") val year: String? = null,
    @Json(name = "round") val round: Int? = null,
    @Json(name = "meeting") val meeting: String? = null,
    @Json(name = "circuit") val circuit: String? = null,
    @Json(name = "session_name") val sessionName: String? = null,
    @Json(name = "session_type") val sessionType: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "total_laps") val totalLaps: Int? = null
)

@JsonClass(generateAdapter = true)
data class FastestLapInfo(
    @Json(name = "driver_number") val driverNumber: String? = null,
    @Json(name = "time") val time: String? = null,
    @Json(name = "lap") val lap: Int? = null
)

@JsonClass(generateAdapter = true)
data class ResultDriverMeta(
    @Json(name = "name") val name: String? = null,
    @Json(name = "acronym") val acronym: String? = null,
    @Json(name = "team") val team: String? = null,
    @Json(name = "team_color") val teamColor: String? = null
)

@JsonClass(generateAdapter = true)
data class ResultClassificationItem(
    @Json(name = "position") val position: Any? = null,
    @Json(name = "driver_number") val driverNumber: String? = null,
    @Json(name = "laps_completed") val lapsCompleted: Int? = null,
    @Json(name = "gap_to_leader") val gapToLeader: String? = null,
    @Json(name = "interval") val interval: String? = null,
    @Json(name = "best_lap_time") val bestLapTime: String? = null,
    @Json(name = "best_lap_number") val bestLapNumber: Int? = null,
    @Json(name = "retired") val retired: Boolean? = null,
    @Json(name = "stopped") val stopped: Boolean? = null,
    @Json(name = "in_pit") val inPit: Boolean? = null,
    @Json(name = "stints") val stints: List<StintInfo>? = null,
    @Json(name = "speed_traps") val speedTraps: SpeedTrapData? = null
) {
    fun displayPosition(): String = position?.toString() ?: "-"
}

@JsonClass(generateAdapter = true)
data class StintInfo(
    @Json(name = "compound") val compound: String? = null,
    @Json(name = "laps") val laps: Int? = null,
    @Json(name = "new") val isNew: Any? = null
)

@JsonClass(generateAdapter = true)
data class SpeedTrapData(
    @Json(name = "i1") val i1: String? = null,
    @Json(name = "i2") val i2: String? = null,
    @Json(name = "fl") val fl: String? = null,
    @Json(name = "st") val st: String? = null
)

package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class TimingResponse(
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "session") val session: String? = null,
    @Json(name = "live") val live: Boolean? = null,
    @Json(name = "stale") val stale: Boolean? = null,
    @Json(name = "stale_reason") val staleReason: String? = null,
    @Json(name = "drivers") val drivers: List<TimingDriverLine>? = null,
    @Json(name = "lap_count") val lapCount: LapCountData? = null
)

@JsonClass(generateAdapter = true)
data class TyreStint(
    @Json(name = "Compound") val compound: String? = null,
    @Json(name = "New") val isNew: String? = null,
    @Json(name = "TyresNotChanged") val tyresNotChanged: String? = null,
    @Json(name = "TotalLaps") val totalLaps: Int? = null,
    @Json(name = "StartLaps") val startLaps: Int? = null
)

@JsonClass(generateAdapter = true)
data class TimingDriverLine(
    @Json(name = "position") val position: String? = null,
    @Json(name = "racing_number") val racingNumber: String? = null,
    @Json(name = "driver_number") val driverNumber: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "tla") val tla: String? = null,
    @Json(name = "acronym") val acronym: String? = null,
    @Json(name = "team") val team: String? = null,
    @Json(name = "team_colour") val teamColour: String? = null,
    @Json(name = "gap") val gap: String? = null,
    @Json(name = "gap_to_leader") val gapToLeader: String? = null,
    @Json(name = "time_diff_to_fastest") val timeDiffToFastest: String? = null,
    @Json(name = "interval") val interval: String? = null,
    @Json(name = "lap_time") val lapTime: String? = null,
    @Json(name = "best_lap_time") val bestLapTime: String? = null,
    @Json(name = "last_lap_time") val lastLapTime: String? = null,
    @Json(name = "laps") val laps: Int? = null,
    @Json(name = "laps_completed") val lapsCompleted: Int? = null,
    @Json(name = "in_pit") val inPit: Boolean? = null,
    @Json(name = "pit_out") val pitOut: Boolean? = null,
    @Json(name = "pit_stops") val pitStops: Int? = null,
    @Json(name = "retired") val retired: Boolean? = null,
    @Json(name = "stopped") val stopped: Boolean? = null,
    @Json(name = "knock_out") val knockOut: Boolean? = null,
    @Json(name = "headshot_url") val headshotUrl: String? = null,
    @Json(name = "HeadshotUrl") val headshotUrlAlt: String? = null,
    @Json(name = "total_race_time") val totalRaceTime: String? = null,
    @Json(name = "Stints") val stints: Map<String, TyreStint> = emptyMap()
) {
    fun getEffectiveHeadshotUrl(): String? = headshotUrl ?: headshotUrlAlt
    fun getSortedStints(): List<TyreStint> =
        stints.entries.sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }.map { it.value }

    fun getCurrentTyreCompound(): String? = getSortedStints().lastOrNull()?.compound
    fun getDisplayPosition(): String = position?.let { value ->
        value.toDoubleOrNull()?.let { number ->
            if (number % 1.0 == 0.0) number.toInt().toString() else value
        } ?: value
    } ?: "-"

    fun getDisplayNumber(): String {
        return driverNumber ?: racingNumber ?: "-"
    }

    fun getDisplayTla(): String {
        return tla ?: acronym ?: "-"
    }

    fun getEffectiveLapTime(): String? {
        return bestLapTime ?: lapTime ?: lastLapTime
    }

    fun getStatusText(): String? {
        return when {
            retired == true -> "OUT"
            stopped == true -> "STOPPED"
            knockOut == true -> "KNOCKED OUT"
            inPit == true -> "IN PIT"
            pitOut == true -> "PIT OUT"
            else -> null
        }
    }
}

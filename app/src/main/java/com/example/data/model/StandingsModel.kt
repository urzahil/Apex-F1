package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DriverStandingsResponse(
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "season") val season: Int? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "standings") val standings: List<DriverStanding>? = null
)

@JsonClass(generateAdapter = true)
data class DriverStanding(
    @Json(name = "position") val position: Int? = null,
    @Json(name = "driver_number") val driverNumber: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "acronym") val acronym: String? = null,
    @Json(name = "nationality") val nationality: String? = null,
    @Json(name = "team") val team: String? = null,
    @Json(name = "team_colour") val teamColour: String? = null,
    @Json(name = "points") val points: Double? = null,
    @Json(name = "wins") val wins: Int? = null,
    @Json(name = "podiums") val podiums: Int? = null,
    @Json(name = "headshot_url") val headshotUrl: String? = null,
    @Json(name = "HeadshotUrl") val headshotUrlAlt: String? = null
) {
    fun getEffectiveHeadshotUrl(): String? = headshotUrl ?: headshotUrlAlt

    fun displayPoints(): String {
        val pts = points ?: 0.0
        return if (pts % 1.0 == 0.0) pts.toInt().toString() else pts.toString()
    }
}

@JsonClass(generateAdapter = true)
data class ConstructorStandingsResponse(
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "season") val season: Int? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "standings") val standings: List<ConstructorStanding>? = null
)

@JsonClass(generateAdapter = true)
data class ConstructorStanding(
    @Json(name = "position") val position: Int? = null,
    @Json(name = "team") val team: String? = null,
    @Json(name = "team_colour") val teamColour: String? = null,
    @Json(name = "points") val points: Double? = null,
    @Json(name = "wins") val wins: Int? = null
) {
    fun displayPoints(): String {
        val pts = points ?: 0.0
        return if (pts % 1.0 == 0.0) pts.toInt().toString() else pts.toString()
    }
}

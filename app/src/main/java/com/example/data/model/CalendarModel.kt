package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CalendarRound(
    @Json(name = "round") val round: Int? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "hasSprint") val hasSprint: Boolean? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "circuit") val circuit: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "track") val track: TrackDetails? = null,
    @Json(name = "sessions") val sessions: CalendarSessions? = null
)

@JsonClass(generateAdapter = true)
data class TrackDetails(
    @Json(name = "length_km") val lengthKm: Double? = null,
    @Json(name = "laps") val laps: Int? = null,
    @Json(name = "corners") val corners: Int? = null,
    @Json(name = "first_gp") val firstGp: Int? = null,
    @Json(name = "lap_record") val lapRecord: String? = null
)

@JsonClass(generateAdapter = true)
data class CalendarSessions(
    @Json(name = "fp1") val fp1: String? = null,
    @Json(name = "fp2") val fp2: String? = null,
    @Json(name = "fp3") val fp3: String? = null,
    @Json(name = "sprint_qualifying") val sprintQualifying: String? = null,
    @Json(name = "sprint") val sprint: String? = null,
    @Json(name = "qualifying") val qualifying: String? = null,
    @Json(name = "race") val race: String? = null
)

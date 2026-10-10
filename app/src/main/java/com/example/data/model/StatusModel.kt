package com.example.data.model

import com.example.ui.components.LiveBadgeStatus
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

fun StatusResponse?.getBadgeStatus(): LiveBadgeStatus {
    if (this == null) return LiveBadgeStatus.FINALISED

    val sessionStatus = session?.sessionStatus
    val isLiveSession = live == true || sessionStatus?.let {
        it.equals("Started", true) || it.equals("Active", true) || it.equals("Running", true)
    } == true

    if (isLiveSession) return LiveBadgeStatus.LIVE

    if (sessionStatus?.let {
            it.equals("Finalised", true) || it.equals("Finished", true) ||
                    it.equals("Ended", true) || it.equals("Completed", true) || it.equals(
                "Ends",
                true
            )
        } == true) {
        return LiveBadgeStatus.FINALISED
    }

    if (stale == true || sessionStatus?.let {
            it.equals("Inactive", true) || it.equals("Upcoming", true) ||
                    it.equals("Not Started", true) || it.equals(
                "Scheduled",
                true
            ) || it.equals("Created", true)
        } == true || staleReason?.contains("not started", ignoreCase = true) == true ||
        staleReason?.contains("upcoming", ignoreCase = true) == true) {
        return LiveBadgeStatus.NOT_STARTED
    }

    return LiveBadgeStatus.FINALISED
}

@JsonClass(generateAdapter = true)
data class StatusResponse(
    @Json(name = "connected") val connected: Boolean? = null,
    @Json(name = "live") val live: Boolean? = null,
    @Json(name = "last_update") val lastUpdate: String? = null,
    @Json(name = "stale") val stale: Boolean? = null,
    @Json(name = "stale_reason") val staleReason: String? = null,
    @Json(name = "session") val session: LiveSessionInfo? = null,
    @Json(name = "clock") val clock: ClockInfo? = null,
    @Json(name = "track_status") val trackStatus: TrackStatusInfo? = null,
    @Json(name = "top_three") val topThree: TopThreeData? = null,
    @Json(name = "lap_count") val lapCount: LapCountData? = null
)

@JsonClass(generateAdapter = true)
data class LiveSessionInfo(
    @Json(name = "Meeting") val meeting: MeetingInfo? = null,
    @Json(name = "SessionStatus") val sessionStatus: String? = null,
    @Json(name = "Key") val key: Int? = null,
    @Json(name = "Type") val type: String? = null,
    @Json(name = "Number") val number: Int? = null,
    @Json(name = "Name") val name: String? = null,
    @Json(name = "StartDate") val startDate: String? = null,
    @Json(name = "EndDate") val endDate: String? = null,
    @Json(name = "GmtOffset") val gmtOffset: String? = null,
    @Json(name = "Path") val path: String? = null
)

@JsonClass(generateAdapter = true)
data class MeetingInfo(
    @Json(name = "Key") val key: Int? = null,
    @Json(name = "Name") val name: String? = null,
    @Json(name = "OfficialName") val officialName: String? = null,
    @Json(name = "Location") val location: String? = null,
    @Json(name = "Number") val number: Int? = null,
    @Json(name = "Country") val country: CountryInfo? = null,
    @Json(name = "Circuit") val circuit: CircuitInfo? = null
)

@JsonClass(generateAdapter = true)
data class CountryInfo(
    @Json(name = "Key") val key: Int? = null,
    @Json(name = "Code") val code: String? = null,
    @Json(name = "Name") val name: String? = null
)

@JsonClass(generateAdapter = true)
data class CircuitInfo(
    @Json(name = "Key") val key: Int? = null,
    @Json(name = "ShortName") val shortName: String? = null
)

@JsonClass(generateAdapter = true)
data class ClockInfo(
    @Json(name = "Utc") val utc: String? = null,
    @Json(name = "Remaining") val remaining: String? = null,
    @Json(name = "Extrapolating") val extrapolating: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class TrackStatusInfo(
    @Json(name = "Status") val status: String? = null,
    @Json(name = "Message") val message: String? = null
)

data class TopThreeData(
    @Json(name = "SessionPart") val sessionPart: Int? = null,
    @Json(name = "Withheld") val withheld: Boolean? = null,
    @Json(name = "Lines") val lines: List<TopThreeDriver>? = null
)

@JsonClass(generateAdapter = true)
data class TopThreeDriver(
    @Json(name = "Position") val position: String? = null,
    @Json(name = "RacingNumber") val racingNumber: String? = null,
    @Json(name = "Tla") val tla: String? = null,
    @Json(name = "BroadcastName") val broadcastName: String? = null,
    @Json(name = "FullName") val fullName: String? = null,
    @Json(name = "FirstName") val firstName: String? = null,
    @Json(name = "LastName") val lastName: String? = null,
    @Json(name = "Team") val team: String? = null,
    @Json(name = "TeamColour") val teamColour: String? = null,
    @Json(name = "LapTime") val lapTime: String? = null,
    @Json(name = "DiffToAhead") val diffToAhead: String? = null,
    @Json(name = "DiffToLeader") val diffToLeader: String? = null,
    @Json(name = "OverallFastest") val overallFastest: Boolean? = null,
    @Json(name = "PersonalFastest") val personalFastest: Boolean? = null,
    @Json(name = "HeadshotUrl") val headshotUrl: String? = null,
    @Json(name = "headshot_url") val headshotUrlAlt: String? = null
) {
    fun getEffectiveHeadshotUrl(): String? = headshotUrl ?: headshotUrlAlt
}

@JsonClass(generateAdapter = true)
data class SessionDriver(
    @Json(name = "driver_number") val driverNumber: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "acronym") val acronym: String? = null,
    @Json(name = "team") val team: String? = null,
    @Json(name = "team_colour") val teamColour: String? = null,
    @Json(name = "headshot_url") val headshotUrl: String? = null,
    @Json(name = "country_code") val countryCode: String? = null
)

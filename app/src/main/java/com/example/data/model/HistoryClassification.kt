package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MergedHistoryClassification(
    val position: String,
    val driverNumber: String,
    val fullName: String,
    val broadcastName: String,
    val tla: String,
    val teamName: String,
    val teamColour: String,
    val headshotUrl: String?,
    val countryCode: String?,
    val gapToLeader: String?,
    val intervalToAhead: String?,
    val timeDiffToFastest: String?,
    val bestLapTime: String?,
    val totalRaceTime: String?,
    val q1Time: String?,
    val q2Time: String?,
    val q3Time: String?,
    val q1Diff: String?,
    val q2Diff: String?,
    val q3Diff: String?,
    val knockedOut: Boolean?,
    val numberOfLaps: Int?,
    val numberOfPitStops: Int?,
    val isRetired: Boolean,
    val inPit: Boolean,
    val stopped: Boolean
)
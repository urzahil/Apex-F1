package com.example.ui.live

import java.util.Locale

object LiveTimeUtils {

    /**
     * Parses lap times formatted as "M:SS.mmm", "SS.mmm", or "H:MM:SS.mmm" into milliseconds.
     */
    fun parseLapTimeToMillis(timeStr: String?): Long? {
        if (timeStr.isNullOrBlank()) return null
        val clean = timeStr.trim().removePrefix("+").removeSuffix("s")
        val parts = clean.split(":")
        return try {
            when (parts.size) {
                1 -> (parts[0].toDouble() * 1000).toLong()
                2 -> {
                    val min = parts[0].toLong()
                    val sec = parts[1].toDouble()
                    (min * 60 * 1000 + (sec * 1000).toLong())
                }
                3 -> {
                    val hours = parts[0].toLong()
                    val min = parts[1].toLong()
                    val sec = parts[2].toDouble()
                    (hours * 3600 * 1000 + min * 60 * 1000 + (sec * 1000).toLong())
                }
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Calculates the time gap between P1 and another driver given their lap time strings.
     * Returns formatted string like "+0.250s".
     */
    fun calculateLapGap(leaderTimeStr: String?, driverTimeStr: String?): String? {
        if (leaderTimeStr.isNullOrBlank() || driverTimeStr.isNullOrBlank()) return null
        val leaderMillis = parseLapTimeToMillis(leaderTimeStr) ?: return null
        val driverMillis = parseLapTimeToMillis(driverTimeStr) ?: return null
        val diffMillis = driverMillis - leaderMillis
        if (diffMillis < 0) return null
        val seconds = diffMillis / 1000.0
        return String.format(Locale.US, "+%.3fs", seconds)
    }

    /**
     * Parses clock remaining string "HH:MM:SS" or "MM:SS" into total seconds.
     */
    fun parseClockSeconds(clockStr: String?): Long? {
        if (clockStr.isNullOrBlank()) return null
        val clean = clockStr.trim()
        val parts = clean.split(":")
        return try {
            when (parts.size) {
                1 -> parts[0].toLong()
                2 -> parts[0].toLong() * 60 + parts[1].toLong()
                3 -> parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Formats total seconds into "MM:SS" or "HH:MM:SS".
     */
    fun formatClockSeconds(totalSeconds: Long): String {
        if (totalSeconds <= 0) return "00:00"
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    /**
     * Formats session name into detailed stage (e.g., "Qualifying - Q2", "Sprint Qualifying - SQ1").
     */
    fun getDetailedSessionName(
        sessionName: String?,
        sessionType: String?,
        sessionPart: Int?,
        raceControlMessages: List<String>? = null
    ): String {
        val base = sessionName ?: sessionType ?: "Session"
        val isQuali = base.contains("Qualifying", ignoreCase = true) || sessionType?.contains("Qualifying", ignoreCase = true) == true
        val isSprintShootout = base.contains("Sprint Shootout", ignoreCase = true) || base.contains("Sprint Qualifying", ignoreCase = true)

        val resolvedPart = sessionPart ?: run {
            if (isQuali) {
                val text = raceControlMessages?.joinToString(" ") ?: ""
                when {
                    text.contains("Q3", ignoreCase = true) -> 3
                    text.contains("Q2", ignoreCase = true) -> 2
                    text.contains("Q1", ignoreCase = true) -> 1
                    else -> null
                }
            } else null
        }

        if (resolvedPart != null && resolvedPart in 1..3) {
            return if (isSprintShootout) {
                "Sprint Qualifying - SQ$resolvedPart"
            } else if (isQuali) {
                "Qualifying - Q$resolvedPart"
            } else {
                "$base - Part $resolvedPart"
            }
        }
        return base
    }
}

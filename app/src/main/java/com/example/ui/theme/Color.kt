package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Formula 1 Official & Inspired Palette
val F1Red = Color(0xFFE10600)
val F1RedDark = Color(0xFFB00400)
val F1RedBright = Color(0xFFFF1801)

// Dark Asphalt & Carbon
val CarbonBackground = Color(0xFF101014)
val CarbonSurface = Color(0xFF17171C)
val CarbonCard = Color(0xFF1E1E24)
val CarbonCardElevated = Color(0xFF26262E)
val CarbonDivider = Color(0xFF2E2E38)

// Text Colors
val F1TextPrimary = Color(0xFFFFFFFF)
val F1TextSecondary = Color(0xFFA0A0AC)
val F1TextTertiary = Color(0xFF6B6B78)

// Accent & Racing Flags
val FlagYellow = Color(0xFFFFD12E)
val FlagGreen = Color(0xFF00D154)
val FlagRed = Color(0xFFFF3333)
val FlagBlue = Color(0xFF2E8BFF)
val ElectricCyan = Color(0xFF00D7B6)

// Tyre Compounds
val TyreSoft = Color(0xFFFF2A2A)
val TyreMedium = Color(0xFFFFD12E)
val TyreHard = Color(0xFFFFFFFF)
val TyreInter = Color(0xFF33C25D)
val TyreWet = Color(0xFF2185FF)

// Team Colors Fallbacks
val TeamMercedes = Color(0xFF00D7B6)
val TeamFerrari = Color(0xFFED1131)
val TeamRedBull = Color(0xFF4781D7)
val TeamMcLaren = Color(0xFFF47600)
val TeamAstonMartin = Color(0xFF229971)
val TeamAlpine = Color(0xFF00A1E8)
val TeamWilliams = Color(0xFF1868DB)
val TeamRacingBulls = Color(0xFF6C98FF)
val TeamHaas = Color(0xFF9C9FA2)
val TeamAudi = Color(0xFFF50537)
val TeamCadillac = Color(0xFF909090)

fun parseHexColor(hexString: String?, fallback: Color = F1Red): Color {
    if (hexString.isNullOrBlank()) return fallback
    val cleanHex = hexString.trim().removePrefix("#")
    return try {
        when (cleanHex.length) {
            6 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            8 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            3 -> {
                val r = cleanHex[0]
                val g = cleanHex[1]
                val b = cleanHex[2]
                Color(android.graphics.Color.parseColor("#$r$r$g$g$b$b"))
            }
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

fun getTeamColor(teamName: String?, rawColour: String? = null): Color {
    if (!rawColour.isNullOrBlank()) {
        val parsed = parseHexColor(rawColour, Color.Unspecified)
        if (parsed != Color.Unspecified) return parsed
    }
    val name = teamName?.lowercase() ?: ""
    return when {
        name.contains("mercedes") -> TeamMercedes
        name.contains("ferrari") -> TeamFerrari
        name.contains("red bull") -> TeamRedBull
        name.contains("mclaren") -> TeamMcLaren
        name.contains("aston martin") -> TeamAstonMartin
        name.contains("alpine") -> TeamAlpine
        name.contains("williams") -> TeamWilliams
        name.contains("racing bulls") || name.contains("rb") || name.contains("alphatauri") || name.contains("toro rosso") -> TeamRacingBulls
        name.contains("haas") -> TeamHaas
        name.contains("audi") || name.contains("sauber") || name.contains("kick") || name.contains("alfa romeo") -> TeamAudi
        name.contains("cadillac") -> TeamCadillac
        name.contains("renault") -> Color(0xFFFFF500)
        name.contains("force india") || name.contains("racing point") -> Color(0xFFF596C8)
        else -> F1Red
    }
}

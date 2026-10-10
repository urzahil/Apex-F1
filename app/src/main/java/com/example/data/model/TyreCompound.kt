package com.example.data.model

enum class TyreCompound {
    SOFT,
    MEDIUM,
    HARD,
    INTERMEDIATE,
    WET,
    UNKNOWN;

    companion object {
        fun fromApiValue(value: String?): TyreCompound {
            val upper = value?.trim()?.uppercase().orEmpty()
            return when {
                upper.contains("INTER") -> INTERMEDIATE
                upper.contains("SOFT") -> SOFT
                upper.contains("MED") -> MEDIUM
                upper.contains("HARD") -> HARD
                upper.contains("WET") -> WET
                else -> UNKNOWN
            }
        }
    }
}

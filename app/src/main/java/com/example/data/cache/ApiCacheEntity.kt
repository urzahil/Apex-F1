package com.example.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_cache")
data class ApiCacheEntity(
    @PrimaryKey val key: String,
    val payload: String,
    val updatedAtEpochMs: Long
)

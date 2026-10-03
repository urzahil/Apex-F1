package com.example.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_classification")
data class HistoryClassificationEntity(
    @PrimaryKey val path: String,
    val payload: String,
    val cachedAt: Long
)
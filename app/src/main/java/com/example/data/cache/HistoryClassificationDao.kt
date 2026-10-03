package com.example.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HistoryClassificationDao {
    @Query("SELECT * FROM history_classification WHERE path = :path LIMIT 1")
    suspend fun get(path: String): HistoryClassificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: HistoryClassificationEntity)
}
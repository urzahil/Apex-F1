package com.example.data.cache

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ApiCacheDao {
    @Query("SELECT * FROM api_cache WHERE key = :key LIMIT 1")
    suspend fun get(key: String): ApiCacheEntity?

    @Upsert
    suspend fun upsert(entity: ApiCacheEntity)

    @Query("""
        DELETE FROM api_cache
        WHERE (key = 'calendar' AND updatedAtEpochMs < :calendarCutoff)
           OR (key = 'results' AND updatedAtEpochMs < :resultsCutoff)
           OR (key LIKE 'standings_%' AND updatedAtEpochMs < :standingsCutoff)
           OR ((key LIKE 'history_year_%' OR key LIKE 'history_session_%') AND updatedAtEpochMs < :historyCutoff)
    """)
    suspend fun deleteExpired(
        calendarCutoff: Long,
        resultsCutoff: Long,
        standingsCutoff: Long,
        historyCutoff: Long
    )

    @Query("DELETE FROM api_cache WHERE key = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM api_cache")
    suspend fun clear()
}

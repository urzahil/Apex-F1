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

    @Query("DELETE FROM api_cache WHERE updatedAtEpochMs < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM api_cache WHERE key = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM api_cache")
    suspend fun clear()
}

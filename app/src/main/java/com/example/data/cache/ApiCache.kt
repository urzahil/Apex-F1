package com.example.data.cache

import com.example.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Type

class ApiCache(private val dao: ApiCacheDao) {
    private val moshi = ApiClient.moshi

    suspend fun <T> read(key: String, type: Type, maxAgeMs: Long): T? =
        withContext(Dispatchers.IO) {
            val entry = dao.get(key) ?: return@withContext null
            if (System.currentTimeMillis() - entry.updatedAtEpochMs > maxAgeMs) return@withContext null
            runCatching { moshi.adapter<T>(type).fromJson(entry.payload) }.getOrNull()
        }

    suspend fun <T> write(key: String, value: T, type: Type) =
        withContext(Dispatchers.IO) {
            val adapter = moshi.adapter<T>(type)
            dao.upsert(
                ApiCacheEntity(
                    key = key,
                    payload = adapter.toJson(value),
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
}

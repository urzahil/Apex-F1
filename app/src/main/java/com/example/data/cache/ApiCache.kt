package com.example.data.cache

import com.example.data.api.ApiClient
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApiCache(
    private val dao: ApiCacheDao
) {
    private val moshi = ApiClient.moshi

    suspend inline fun <reified T> read(key: String, maxAgeMs: Long): T? =
        withContext(Dispatchers.IO) {
            val entry = dao.get(key) ?: return@withContext null
            if (System.currentTimeMillis() - entry.updatedAtEpochMs > maxAgeMs) return@withContext null
            runCatching {
                moshi.adapter<T>(Types.newParameterizedType(List::class.java, Any::class.java))
                moshi.adapter(T::class.java).fromJson(entry.payload)
            }.getOrNull()
        }

    suspend fun <T> readList(key: String, type: java.lang.reflect.Type, maxAgeMs: Long): T? =
        withContext(Dispatchers.IO) {
            val entry = dao.get(key) ?: return@withContext null
            if (System.currentTimeMillis() - entry.updatedAtEpochMs > maxAgeMs) return@withContext null
            runCatching { moshi.adapter<T>(type).fromJson(entry.payload) }.getOrNull()
        }

    suspend fun <T> write(key: String, value: T, type: java.lang.reflect.Type? = null) =
        withContext(Dispatchers.IO) {
            val adapter = if (type != null) moshi.adapter<T>(type) else moshi.adapter(value!!::class.java)
            val payload = adapter.toJson(value)
            dao.upsert(ApiCacheEntity(key, payload, System.currentTimeMillis()))
        }
}

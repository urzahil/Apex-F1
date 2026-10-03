package com.example.data.cache

import com.example.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.reflect.Type

class ApiCache(private val dao: ApiCacheDao) {
    companion object {
        private const val CALENDAR_TTL_MS = 6 * 60 * 60 * 1000L
        private const val RESULTS_TTL_MS = 24 * 60 * 60 * 1000L
        private const val STANDINGS_TTL_MS = 5 * 60 * 1000L
        private const val HISTORY_TTL_MS = 7 * 24 * 60 * 60 * 1000L
    }
    private val purgeMutex = Mutex()
    private var didPurge = false
    private val moshi = ApiClient.moshi

    suspend fun purgeExpired() {
        purgeMutex.withLock {
            if (didPurge) return
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                dao.deleteExpired(
                    calendarCutoff = now - CALENDAR_TTL_MS,
                    resultsCutoff = now - RESULTS_TTL_MS,
                    standingsCutoff = now - STANDINGS_TTL_MS,
                    historyCutoff = now - HISTORY_TTL_MS
                )
            }
            didPurge = true
        }
    }

    suspend fun <T> read(key: String, type: Type, maxAgeMs: Long): T? =
        withContext(Dispatchers.IO) {
            purgeExpired()

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

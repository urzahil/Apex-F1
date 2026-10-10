package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://f1api.urzahil.rocks/"

    private val authInterceptor = Interceptor { chain ->
        val key = BuildConfig.F1_API_KEY.takeIf { it.isNotBlank() }
            ?: error("F1_API_KEY is not configured. Provide it through the local .env file or CI secret.")

        val path = chain.request().url.encodedPath
        val liveEndpoint =
            path == "/status" || path == "/timing" || path == "/snapshot" || path == "/drivers"

        val request = chain.request().newBuilder()
            .addHeader("x-api-key", key)
            .addHeader("Accept", "application/json")
            .apply {
                if (liveEndpoint) {
                    addHeader("Cache-Control", "no-cache")
                    addHeader("Pragma", "no-cache")
                }
            }
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
        else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(StringOrNumberAdapterFactory())
            .add(TopThreeDataFactory())
            .add(TimingResponseFactory())
            .add(SnapshotTimingDataFactory())
            .add(HistoryTimingDataFactory())
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val apiService: F1ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(F1ApiService::class.java)
    }
}

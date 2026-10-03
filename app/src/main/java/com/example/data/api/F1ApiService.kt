package com.example.data.api

import com.example.data.model.CalendarRound
import com.example.data.model.ConstructorStandingsResponse
import com.example.data.model.DetailedResultResponse
import com.example.data.model.DriverStandingsResponse
import com.example.data.model.HistoryDriverInfo
import com.example.data.model.HistorySeasonListResponse
import com.example.data.model.HistoryTimingDataResponse
import com.example.data.model.HistoryYearResponse
import com.example.data.model.ResultFileItem
import com.example.data.model.SnapshotResponse
import com.example.data.model.StatusResponse
import com.example.data.model.TimingResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface F1ApiService {

    @GET("status")
    suspend fun getStatus(): StatusResponse

    @GET("drivers")
    suspend fun getSessionDrivers(): List<com.example.data.model.SessionDriver>

    @GET("snapshot")
    suspend fun getSnapshot(): SnapshotResponse

    @GET("timing")
    suspend fun getTiming(): TimingResponse

    @GET("calendar")
    suspend fun getCalendar(): List<CalendarRound>

    @GET("standings/drivers")
    suspend fun getDriverStandings(): DriverStandingsResponse

    @GET("standings/constructors")
    suspend fun getConstructorStandings(): ConstructorStandingsResponse

    @GET("results")
    suspend fun getResults(): List<ResultFileItem>

    @GET("results/{filename}")
    suspend fun getResultDetail(@Path("filename") filename: String): DetailedResultResponse

    @GET("history")
    suspend fun getHistoryYears(): HistorySeasonListResponse

    @GET("history/{year}")
    suspend fun getHistoryYear(@Path("year") year: Int): HistoryYearResponse

    @GET("history/session")
    suspend fun getHistoryTimingData(
        @Query("path") path: String,
        @Query("topic") topic: String = "TimingData"
    ): HistoryTimingDataResponse

    @GET("history/session")
    suspend fun getHistorySessionData(
        @Query("path") path: String,
        @Query("topic") topic: String = "SessionData"
    ): com.example.data.model.HistorySessionDataResponse

    @GET("history/session")
    suspend fun getHistoryDriverList(
        @Query("path") path: String,
        @Query("topic") topic: String = "DriverList"
    ): Map<String, HistoryDriverInfo>
}

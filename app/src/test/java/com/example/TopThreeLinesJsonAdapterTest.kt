package com.example

import com.example.data.api.TopThreeLinesJsonAdapter
import com.example.data.model.TopThreeDriver
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class TopThreeLinesJsonAdapterTest {
    private val moshi = Moshi.Builder()
        .add(TopThreeLinesJsonAdapter.factory())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter<List<TopThreeDriver>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, TopThreeDriver::class.java)
    )

    @Test
    fun parsesArray() {
        val result = adapter.fromJson("""[{"Position":"1","RacingNumber":"1","Tla":"VER"}]""")
        assertEquals(1, result?.size)
        assertEquals("VER", result?.first()?.tla)
    }

    @Test
    fun parsesObject() {
        val result = adapter.fromJson("""{"1":{"Position":"1","RacingNumber":"1","Tla":"VER"},"2":{"Position":"2","RacingNumber":"4","Tla":"NOR"}}""")
        assertEquals(2, result?.size)
        assertEquals("VER", result?.first()?.tla)
        assertEquals("NOR", result?.last()?.tla)
    }
}
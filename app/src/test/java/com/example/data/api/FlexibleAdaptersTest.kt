package com.example.data.api

import com.example.data.model.StatusResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FlexibleAdaptersTest {
    private val adapter = ApiClient.moshi.adapter(StatusResponse::class.java)

    @Test
    fun topThreeLines_acceptsArray() {
        val json = """
            {"top_three":{"SessionPart":1,"Withheld":false,"Lines":[
              {"Position":"1","RacingNumber":"1","Tla":"VER"},
              {"Position":"2","RacingNumber":"4","Tla":"NOR"}
            ]}}
        """.trimIndent()

        val result = adapter.fromJson(json)
        assertEquals(2, result?.topThree?.lines?.size)
        assertEquals("VER", result?.topThree?.lines?.first()?.tla)
    }

    @Test
    fun timingPosition_numericJson_isNormalizedForDisplay() {
        val json = """{"drivers":[{"position":1,"driver_number":"1","name":"VER"}]}"""
        val adapter = ApiClient.moshi.adapter(com.example.data.model.TimingResponse::class.java)
        val result = adapter.fromJson(json)
        assertEquals("1", result?.drivers?.first()?.getDisplayPosition())
    }

    @Test
    fun topThreeLines_acceptsObject() {
        val json = """
            {"top_three":{"SessionPart":1,"Withheld":false,"Lines":{
              "0":{"RacingNumber":"1","Tla":"VER"},
              "1":{"RacingNumber":"4","Tla":"NOR"}
            }}}
        """.trimIndent()

        val result = adapter.fromJson(json)
        assertNotNull(result?.topThree)
        assertEquals(2, result?.topThree?.lines?.size)
        assertEquals("1", result?.topThree?.lines?.first()?.position)
        assertEquals("4", result?.topThree?.lines?.get(1)?.racingNumber)
    }
}

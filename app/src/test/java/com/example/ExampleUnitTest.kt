package com.example

import com.example.data.api.ApiClient
import com.example.data.model.StatusResponse
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testStatusResponse_withTopThreeLinesAsObject() {
    val json = """
      {
        "connected": true,
        "live": true,
        "session": {
          "Name": "Practice 3",
          "Path": "2026/test/"
        },
        "top_three": {
          "Withheld": false,
          "Lines": {
            "0": {
              "Position": "1",
              "RacingNumber": "1",
              "Tla": "VER",
              "FullName": "Max Verstappen",
              "LapTime": "1:36.000"
            },
            "1": {
              "Position": "2",
              "RacingNumber": "4",
              "Tla": "NOR",
              "FullName": "Lando Norris",
              "LapTime": "1:36.200"
            }
          }
        }
      }
    """.trimIndent()

    val adapter = ApiClient.moshi.adapter(StatusResponse::class.java)
    val parsed = adapter.fromJson(json)
    assertNotNull(parsed)
    assertEquals(true, parsed?.live)
    val lines = parsed?.topThree?.lines
    assertNotNull(lines)
    assertEquals(2, lines?.size)
    assertEquals("1", lines?.get(0)?.position)
    assertEquals("VER", lines?.get(0)?.tla)
    assertEquals("4", lines?.get(1)?.racingNumber)
  }

  @Test
  fun testStatusResponse_withTopThreeLinesAsArray() {
    val json = """
      {
        "connected": true,
        "live": false,
        "top_three": {
          "Withheld": false,
          "Lines": [
            {
              "Position": "1",
              "RacingNumber": "12",
              "Tla": "ANT",
              "FullName": "Kimi Antonelli"
            }
          ]
        }
      }
    """.trimIndent()

    val adapter = ApiClient.moshi.adapter(StatusResponse::class.java)
    val parsed = adapter.fromJson(json)
    assertNotNull(parsed)
    val lines = parsed?.topThree?.lines
    assertNotNull(lines)
    assertEquals(1, lines?.size)
    assertEquals("ANT", lines?.get(0)?.tla)
  }

  @Test
  fun testLiveTimeUtils_calculateLapGap() {
    val gap = com.example.ui.live.LiveTimeUtils.calculateLapGap("1:36.000", "1:36.250")
    assertEquals("+0.250s", gap)

    val gap2 = com.example.ui.live.LiveTimeUtils.calculateLapGap("1:37.980", "1:38.233")
    assertEquals("+0.253s", gap2)

    val invalidGap = com.example.ui.live.LiveTimeUtils.calculateLapGap("1:38.000", "1:37.000")
    assertNull(invalidGap)
  }

  @Test
  fun testLiveTimeUtils_clockParsingAndFormatting() {
    val seconds = com.example.ui.live.LiveTimeUtils.parseClockSeconds("00:14:59")
    assertEquals(899L, seconds)

    val formatted = com.example.ui.live.LiveTimeUtils.formatClockSeconds(899L)
    assertEquals("14:59", formatted)

    val formattedZero = com.example.ui.live.LiveTimeUtils.formatClockSeconds(0L)
    assertEquals("00:00", formattedZero)
  }

  @Test
  fun testLiveTimeUtils_getDetailedSessionName() {
    val q2 = com.example.ui.live.LiveTimeUtils.getDetailedSessionName(
        sessionName = "Qualifying",
        sessionType = "Qualifying",
        sessionPart = 2
    )
    assertEquals("Qualifying - Q2", q2)

    val q1 = com.example.ui.live.LiveTimeUtils.getDetailedSessionName(
        sessionName = "Qualifying",
        sessionType = "Qualifying",
        sessionPart = 1
    )
    assertEquals("Qualifying - Q1", q1)

    val fp1 = com.example.ui.live.LiveTimeUtils.getDetailedSessionName(
        sessionName = "Practice 1",
        sessionType = "Practice",
        sessionPart = null
    )
    assertEquals("Practice 1", fp1)
  }

  @Test
  fun testTimingDriverLine_getStatusText() {
    val inPitDriver = com.example.data.model.TimingDriverLine(inPit = true)
    assertEquals("IN PIT", inPitDriver.getStatusText())

    val outDriver = com.example.data.model.TimingDriverLine(retired = true)
    assertEquals("OUT", outDriver.getStatusText())

    val pitOutDriver = com.example.data.model.TimingDriverLine(pitOut = true)
    assertEquals("PIT OUT", pitOutDriver.getStatusText())

    val onTrackDriver = com.example.data.model.TimingDriverLine(inPit = false, pitOut = false)
    assertNull(onTrackDriver.getStatusText())
  }

  @Test
  fun testSnapshotTimingDataAdapter() {
    val json = """
      {
        "Lines": {
          "3": {
            "Position": "1",
            "RacingNumber": "3",
            "InPit": true,
            "BestLapTime": { "Value": "1:35.130" }
          },
          "44": {
            "Position": "2",
            "RacingNumber": "44",
            "InPit": false,
            "BestLapTime": { "Value": "1:35.428" }
          }
        }
      }
    """.trimIndent()

    val adapter = com.example.data.api.SnapshotTimingDataAdapter()
    val reader = com.squareup.moshi.JsonReader.of(okio.Buffer().writeUtf8(json))
    val result = adapter.fromJson(reader)

    org.junit.Assert.assertNotNull(result)
    assertEquals(2, result?.lines?.size)
    val p1 = result?.lines?.find { it.racingNumber == "3" }
    assertEquals("1", p1?.position)
    assertEquals("1:35.130", p1?.bestLapTime)
    org.junit.Assert.assertTrue(p1?.inPit == true)
  }
}


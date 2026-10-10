package com.example

import com.example.data.api.ApiClient
import com.example.data.model.StatusResponse
import com.example.data.model.getBadgeStatus
import com.example.ui.components.LiveBadgeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTimingResponse_parsesTyreStints() {
        val json = """
      {
        "live": true,
        "drivers": {
          "14": {
            "RacingNumber": "14",
            "Line": 11,
            "GridPos": "12",
            "Stints": {
              "1": {
                "LapFlags": 0,
                "Compound": "INTERMEDIATE",
                "New": "true",
                "TyresNotChanged": "1",
                "TotalLaps": 0,
                "StartLaps": 0
              },
              "2": {
                "LapFlags": 0,
                "Compound": "MEDIUM",
                "New": "true",
                "TyresNotChanged": "0",
                "TotalLaps": 0,
                "StartLaps": 0
              }
            }
          }
        }
      }
    """.trimIndent()

        val adapter = ApiClient.moshi.adapter(com.example.data.model.TimingResponse::class.java)
        val parsed = adapter.fromJson(json)
        val driver = parsed?.drivers?.single()

        assertNotNull(driver)
        assertEquals("14", driver?.getDisplayNumber())
        assertEquals(2, driver?.getSortedStints()?.size)
        assertEquals("INTERMEDIATE", driver?.getSortedStints()?.get(0)?.compound)
        assertEquals("MEDIUM", driver?.getCurrentTyreCompound())
    }

    @Test
    fun testHistoryTimingData_parsesDirectDriverMapWithTyreStints() {
        val json = """
      {
        "14": {
          "Position": 1,
          "RacingNumber": "14",
          "GapToLeader": "0.000",
          "Stints": {
            "1": {
              "LapFlags": 0,
              "Compound": "INTERMEDIATE",
              "New": "true",
              "TyresNotChanged": "1",
              "TotalLaps": 0,
              "StartLaps": 0
            },
            "2": {
              "LapFlags": 0,
              "Compound": "MEDIUM",
              "New": "true",
              "TyresNotChanged": "0",
              "TotalLaps": 0,
              "StartLaps": 0
            }
          }
        }
      }
    """.trimIndent()

        val parsed =
            ApiClient.moshi.adapter(com.example.data.model.HistoryTimingDataResponse::class.java)
                .fromJson(json)
        val line = parsed?.lines?.get("14")

        assertNotNull(line)
        assertEquals("14", line?.racingNumber)
        assertEquals(2, line?.stints?.size)
        assertEquals("INTERMEDIATE", line?.stints?.get("1")?.compound)
        assertEquals("MEDIUM", line?.stints?.get("2")?.compound)
    }

    @Test
    fun testSnapshotResponse_parsesExactTyreStintsForDriver5() {
        val json = """
      {
        "timing": {
          "Lines": {
            "5": {
              "RacingNumber": "5",
              "Line": 18,
              "GridPos": "10",
              "Stints": {
                "0": {"Compound": "SOFT", "TotalLaps": 2},
                "1": {"LapFlags": 0, "Compound": "INTERMEDIATE", "New": "true", "TyresNotChanged": "0", "TotalLaps": 7, "StartLaps": 0, "LapTime": "2:40.620", "LapNumber": 2},
                "2": {"LapFlags": 0, "Compound": "SOFT", "New": "false", "TyresNotChanged": "0", "TotalLaps": 8, "StartLaps": 2, "LapTime": "1:51.116", "LapNumber": 4},
                "3": {"LapFlags": 0, "Compound": "HARD", "New": "true", "TyresNotChanged": "0", "TotalLaps": 19, "StartLaps": 0, "LapTime": "1:47.565", "LapNumber": 14},
                "4": {"LapFlags": 1, "Compound": "SOFT", "New": "false", "TyresNotChanged": "0", "TotalLaps": 18, "StartLaps": 8, "LapTime": "1:41.382", "LapNumber": 36},
                "5": {"LapFlags": 0, "Compound": "SOFT", "New": "false", "TyresNotChanged": "0", "TotalLaps": 17, "StartLaps": 6, "LapTime": "1:42.037", "LapNumber": 48}
              }
            }
          }
        }
      }
    """.trimIndent()

        val parsed = ApiClient.moshi.adapter(com.example.data.model.SnapshotResponse::class.java)
            .fromJson(json)
        val driver = parsed?.timing?.lines?.single()

        assertNotNull(driver)
        assertEquals("5", driver?.racingNumber)
        assertEquals(6, driver?.stints?.size)
        assertEquals("SOFT", driver?.stints?.get(0)?.compound)
        assertEquals("INTERMEDIATE", driver?.stints?.get(1)?.compound)
        assertEquals("HARD", driver?.stints?.get(3)?.compound)
        assertEquals("SOFT", driver?.stints?.get(5)?.compound)
    }

    @Test
    fun testTyreCompoundMapping() {
        assertEquals(
            com.example.data.model.TyreCompound.HARD,
            com.example.data.model.TyreCompound.fromApiValue("HARD")
        )
        assertEquals(
            com.example.data.model.TyreCompound.MEDIUM,
            com.example.data.model.TyreCompound.fromApiValue("MEDIUM")
        )
        assertEquals(
            com.example.data.model.TyreCompound.SOFT,
            com.example.data.model.TyreCompound.fromApiValue("SOFT")
        )
        assertEquals(
            com.example.data.model.TyreCompound.INTERMEDIATE,
            com.example.data.model.TyreCompound.fromApiValue("INTERMEDIATE")
        )
        assertEquals(
            com.example.data.model.TyreCompound.WET,
            com.example.data.model.TyreCompound.fromApiValue("WET")
        )
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

        val adapter =
            com.example.data.api.SnapshotTimingDataAdapter(ApiClient.moshi.adapter(com.example.data.model.TyreStint::class.java))
        val reader = com.squareup.moshi.JsonReader.of(okio.Buffer().writeUtf8(json))
        val result = adapter.fromJson(reader)

        org.junit.Assert.assertNotNull(result)
        assertEquals(2, result?.lines?.size)
        val p1 = result?.lines?.find { it.racingNumber == "3" }
        assertEquals("1", p1?.position)
        assertEquals("1:35.130", p1?.bestLapTime)
        org.junit.Assert.assertTrue(p1?.inPit == true)
    }

    @Test
    fun testSnapshotResponse_parsesExactTyreStints() {
        val json = """
      {
        "timing": {
          "Lines": {
            "5": {
              "RacingNumber": "5",
              "Line": 18,
              "GridPos": "10",
              "Stints": {
                "0": { "Compound": "SOFT", "TotalLaps": 2 },
                "1": {
                  "LapFlags": 0,
                  "Compound": "INTERMEDIATE",
                  "New": "true",
                  "TyresNotChanged": "0",
                  "TotalLaps": 7,
                  "StartLaps": 0,
                  "LapTime": "2:40.620",
                  "LapNumber": 2
                },
                "2": {
                  "LapFlags": 0,
                  "Compound": "SOFT",
                  "New": "false",
                  "TyresNotChanged": "0",
                  "TotalLaps": 8,
                  "StartLaps": 2,
                  "LapTime": "1:51.116",
                  "LapNumber": 4
                },
                "3": {
                  "LapFlags": 0,
                  "Compound": "HARD",
                  "New": "true",
                  "TyresNotChanged": "0",
                  "TotalLaps": 19,
                  "StartLaps": 0,
                  "LapTime": "1:47.565",
                  "LapNumber": 14
                },
                "4": {
                  "LapFlags": 1,
                  "Compound": "SOFT",
                  "New": "false",
                  "TyresNotChanged": "0",
                  "TotalLaps": 18,
                  "StartLaps": 8,
                  "LapTime": "1:41.382",
                  "LapNumber": 36
                },
                "5": {
                  "LapFlags": 0,
                  "Compound": "SOFT",
                  "New": "false",
                  "TyresNotChanged": "0",
                  "TotalLaps": 17,
                  "StartLaps": 6,
                  "LapTime": "1:42.037",
                  "LapNumber": 48
                }
              }
            }
          }
        }
      }
    """.trimIndent()

        val adapter = ApiClient.moshi.adapter(com.example.data.model.SnapshotResponse::class.java)
        val parsed = adapter.fromJson(json)
        val driver = parsed?.timing?.lines?.single()

        assertNotNull(driver)
        assertEquals("5", driver?.racingNumber)
        assertEquals(6, driver?.stints?.size)
        assertEquals("SOFT", driver?.stints?.get(0)?.compound)
        assertEquals("SOFT", driver?.stints?.get(5)?.compound)
    }

    @Test
    fun testSnapshotResponse_parsesRealTimingAppArrayTyreStints() {
        val json = """
      {
        "timing_app": {
          "Lines": {
            "5": {
              "RacingNumber": "5",
              "Line": 18,
              "Stints": [
                {
                  "LapFlags": 0,
                  "Compound": "SOFT",
                  "New": "true",
                  "TyresNotChanged": "0",
                  "TotalLaps": 2,
                  "StartLaps": 0
                },
                {
                  "LapFlags": 0,
                  "Compound": "INTERMEDIATE",
                  "New": "true",
                  "TyresNotChanged": "0",
                  "TotalLaps": 7,
                  "StartLaps": 0
                },
                {
                  "LapFlags": 0,
                  "Compound": "HARD",
                  "New": "true",
                  "TyresNotChanged": "0",
                  "TotalLaps": 19,
                  "StartLaps": 0
                },
                {
                  "LapFlags": 0,
                  "Compound": "SOFT",
                  "New": "false",
                  "TyresNotChanged": "0",
                  "TotalLaps": 17,
                  "StartLaps": 6
                }
              ]
            }
          }
        }
      }
    """.trimIndent()

        val parsed = ApiClient.moshi.adapter(com.example.data.model.SnapshotResponse::class.java)
            .fromJson(json)
        val driver = parsed?.timingApp?.lines?.single()

        assertNotNull(driver)
        assertEquals("5", driver?.racingNumber)
        assertEquals("18", driver?.position)
        assertEquals(4, driver?.stints?.size)
        assertEquals("SOFT", driver?.stints?.get(0)?.compound)
        assertEquals("INTERMEDIATE", driver?.stints?.get(1)?.compound)
        assertEquals("HARD", driver?.stints?.get(2)?.compound)
        assertEquals("SOFT", driver?.stints?.get(3)?.compound)
        assertEquals(17, driver?.stints?.get(3)?.totalLaps)
    }

    @Test
    fun testGetBadgeStatus() {
        val liveStatus = StatusResponse(
            live = true,
            session = com.example.data.model.LiveSessionInfo(sessionStatus = "Started")
        )
        assertEquals(LiveBadgeStatus.LIVE, liveStatus.getBadgeStatus())

        val notStartedStatus = StatusResponse(
            live = false,
            stale = true,
            staleReason = "Bahrain Grand Prix has not started yet",
            session = com.example.data.model.LiveSessionInfo(sessionStatus = "Inactive")
        )
        assertEquals(LiveBadgeStatus.NOT_STARTED, notStartedStatus.getBadgeStatus())

        val finalisedStatus = StatusResponse(
            live = false,
            stale = false,
            session = com.example.data.model.LiveSessionInfo(sessionStatus = "Finalised")
        )
        assertEquals(LiveBadgeStatus.FINALISED, finalisedStatus.getBadgeStatus())
    }

    @Test
    fun testLapCountData_parsingAndFormatting() {
        val json = """
      {
        "lap_count": {
          "CurrentLap": 1,
          "TotalLaps": 56,
          "_kf": true
        }
      }
    """.trimIndent()

        val adapter = ApiClient.moshi.adapter(com.example.data.model.SnapshotResponse::class.java)
        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        val lapCount = parsed?.lapCount
        assertNotNull(lapCount)
        assertEquals(1, lapCount?.getCurrentLapInt())
        assertEquals(56, lapCount?.getTotalLapsInt())
    }

    @Test
    fun testRaceControlMessages_lastFiveNewestFirst() {
        val messages = listOf(
            com.example.data.model.RaceControlMessage(message = "Msg 1"),
            com.example.data.model.RaceControlMessage(message = "Msg 2"),
            com.example.data.model.RaceControlMessage(message = "Msg 3"),
            com.example.data.model.RaceControlMessage(message = "Msg 4"),
            com.example.data.model.RaceControlMessage(message = "Msg 5"),
            com.example.data.model.RaceControlMessage(message = "Msg 6")
        )
        val lastFiveNewestFirst =
            messages.filterNot { it.message.isNullOrBlank() }.takeLast(5).reversed()
        assertEquals(5, lastFiveNewestFirst.size)
        assertEquals("Msg 6", lastFiveNewestFirst[0].message)
        assertEquals("Msg 2", lastFiveNewestFirst[4].message)
    }
}


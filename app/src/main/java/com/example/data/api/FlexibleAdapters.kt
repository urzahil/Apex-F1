package com.example.data.api

import com.example.data.model.SnapshotTimingData
import com.example.data.model.SnapshotTimingDriverLine
import com.example.data.model.TimingDriverLine
import com.example.data.model.TimingResponse
import com.example.data.model.TopThreeData
import com.example.data.model.TopThreeDriver
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

class TopThreeDataAdapter(
    private val driverAdapter: JsonAdapter<TopThreeDriver>
) : JsonAdapter<TopThreeData>() {

    override fun fromJson(reader: JsonReader): TopThreeData? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }
        if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }

        var sessionPart: Int? = null
        var withheld: Boolean? = null
        val lines = mutableListOf<TopThreeDriver>()

        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            when {
                name.equals("SessionPart", ignoreCase = true) -> {
                    if (reader.peek() == JsonReader.Token.NUMBER) {
                        sessionPart = reader.nextInt()
                    } else {
                        reader.skipValue()
                    }
                }
                name.equals("Withheld", ignoreCase = true) -> {
                    if (reader.peek() == JsonReader.Token.BOOLEAN) {
                        withheld = reader.nextBoolean()
                    } else {
                        reader.skipValue()
                    }
                }
                name.equals("Lines", ignoreCase = true) || name.equals("lines", ignoreCase = true) -> {
                    when (reader.peek()) {
                        JsonReader.Token.BEGIN_ARRAY -> {
                            reader.beginArray()
                            var arrayIdx = 0
                            while (reader.hasNext()) {
                                if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                    val drv = driverAdapter.fromJson(reader)
                                    if (drv != null) {
                                        val resolvedPos = drv.position?.takeIf { it.isNotBlank() }
                                            ?: (arrayIdx + 1).toString()
                                        lines.add(drv.copy(position = resolvedPos))
                                    }
                                    arrayIdx++
                                } else {
                                    reader.skipValue()
                                }
                            }
                            reader.endArray()
                        }
                        JsonReader.Token.BEGIN_OBJECT -> {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                val entryKey = reader.nextName() // entry key, e.g. "0", "1", "2"
                                if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                    val drv = driverAdapter.fromJson(reader)
                                    if (drv != null) {
                                        val resolvedPos = drv.position?.takeIf { it.isNotBlank() }
                                            ?: entryKey.toIntOrNull()?.let { (it + 1).toString() }
                                        lines.add(drv.copy(position = resolvedPos))
                                    }
                                } else {
                                    reader.skipValue()
                                }
                            }
                            reader.endObject()
                        }
                        else -> {
                            reader.skipValue()
                        }
                    }
                }
                else -> {
                    reader.skipValue()
                }
            }
        }
        reader.endObject()

        return TopThreeData(
            sessionPart = sessionPart,
            withheld = withheld,
            lines = lines.sortedBy { it.position?.toIntOrNull() ?: 99 }
        )
    }

    override fun toJson(writer: JsonWriter, value: TopThreeData?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        if (value.sessionPart != null) {
            writer.name("SessionPart").value(value.sessionPart)
        }
        writer.name("Withheld").value(value.withheld)
        writer.name("Lines")
        writer.beginArray()
        value.lines?.forEach { driverAdapter.toJson(writer, it) }
        writer.endArray()
        writer.endObject()
    }
}

class TopThreeDataFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) == TopThreeData::class.java) {
            val driverAdapter = moshi.adapter(TopThreeDriver::class.java)
            return TopThreeDataAdapter(driverAdapter)
        }
        return null
    }
}

class TimingResponseAdapter(
    private val driverAdapter: JsonAdapter<TimingDriverLine>
) : JsonAdapter<TimingResponse>() {

    override fun fromJson(reader: JsonReader): TimingResponse? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }
        if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }

        var timestamp: String? = null
        var session: String? = null
        var live: Boolean? = null
        var stale: Boolean? = null
        var staleReason: String? = null
        val drivers = mutableListOf<TimingDriverLine>()

        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            when {
                name.equals("timestamp", ignoreCase = true) -> {
                    timestamp = if (reader.peek() == JsonReader.Token.STRING) reader.nextString() else { reader.skipValue(); null }
                }
                name.equals("session", ignoreCase = true) -> {
                    session = if (reader.peek() == JsonReader.Token.STRING) reader.nextString() else { reader.skipValue(); null }
                }
                name.equals("live", ignoreCase = true) -> {
                    live = if (reader.peek() == JsonReader.Token.BOOLEAN) reader.nextBoolean() else { reader.skipValue(); null }
                }
                name.equals("stale", ignoreCase = true) -> {
                    stale = if (reader.peek() == JsonReader.Token.BOOLEAN) reader.nextBoolean() else { reader.skipValue(); null }
                }
                name.equals("stale_reason", ignoreCase = true) -> {
                    staleReason = if (reader.peek() == JsonReader.Token.STRING) reader.nextString() else { reader.skipValue(); null }
                }
                name.equals("drivers", ignoreCase = true) || name.equals("Lines", ignoreCase = true) || name.equals("lines", ignoreCase = true) -> {
                    when (reader.peek()) {
                        JsonReader.Token.BEGIN_ARRAY -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                    driverAdapter.fromJson(reader)?.let { drivers.add(it) }
                                } else {
                                    reader.skipValue()
                                }
                            }
                            reader.endArray()
                        }
                        JsonReader.Token.BEGIN_OBJECT -> {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                val key = reader.nextName()
                                if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                    val drv = driverAdapter.fromJson(reader)
                                    if (drv != null) {
                                        val withKey = if (drv.driverNumber == null && drv.racingNumber == null && key.toIntOrNull() != null) {
                                            drv.copy(driverNumber = key, racingNumber = key)
                                        } else drv
                                        drivers.add(withKey)
                                    }
                                } else {
                                    reader.skipValue()
                                }
                            }
                            reader.endObject()
                        }
                        else -> {
                            reader.skipValue()
                        }
                    }
                }
                else -> {
                    reader.skipValue()
                }
            }
        }
        reader.endObject()

        return TimingResponse(
            timestamp = timestamp,
            session = session,
            live = live,
            stale = stale,
            staleReason = staleReason,
            drivers = drivers.sortedBy { it.getDisplayPosition().toIntOrNull() ?: 999 }
        )
    }

    override fun toJson(writer: JsonWriter, value: TimingResponse?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("timestamp").value(value.timestamp)
        writer.name("session").value(value.session)
        writer.name("live").value(value.live)
        writer.name("stale").value(value.stale)
        writer.name("stale_reason").value(value.staleReason)
        writer.name("drivers")
        writer.beginArray()
        value.drivers?.forEach { driverAdapter.toJson(writer, it) }
        writer.endArray()
        writer.endObject()
    }
}

class TimingResponseFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) == TimingResponse::class.java) {
            val driverAdapter = moshi.adapter(TimingDriverLine::class.java)
            return TimingResponseAdapter(driverAdapter)
        }
        return null
    }
}

class SnapshotTimingDataAdapter : JsonAdapter<SnapshotTimingData>() {
    override fun fromJson(reader: JsonReader): SnapshotTimingData? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }
        if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }

        val lines = mutableListOf<SnapshotTimingDriverLine>()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (name.equals("Lines", ignoreCase = true)) {
                if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        val lineKey = reader.nextName()
                        if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                            var pos: String? = null
                            var racingNum = lineKey
                            var bestLap: String? = null
                            var lastLap: String? = null
                            var gapToLeader: String? = null
                            var intervalToAhead: String? = null
                            var inPit = false
                            var pitOut = false
                            var retired = false
                            var stopped = false
                            var knockedOut = false

                            reader.beginObject()
                            while (reader.hasNext()) {
                                val fieldName = reader.nextName()
                                when {
                                    fieldName.equals("Position", ignoreCase = true) -> {
                                        pos = if (reader.peek() == JsonReader.Token.STRING) {
                                            reader.nextString()
                                        } else if (reader.peek() == JsonReader.Token.NUMBER) {
                                            reader.nextInt().toString()
                                        } else {
                                            reader.skipValue(); null
                                        }
                                    }
                                    fieldName.equals("RacingNumber", ignoreCase = true) -> {
                                        racingNum = if (reader.peek() == JsonReader.Token.STRING) {
                                            reader.nextString()
                                        } else if (reader.peek() == JsonReader.Token.NUMBER) {
                                            reader.nextInt().toString()
                                        } else {
                                            reader.skipValue(); lineKey
                                        }
                                    }
                                    fieldName.equals("InPit", ignoreCase = true) -> {
                                        inPit = if (reader.peek() == JsonReader.Token.BOOLEAN) reader.nextBoolean() else { reader.skipValue(); false }
                                    }
                                    fieldName.equals("PitOut", ignoreCase = true) -> {
                                        pitOut = if (reader.peek() == JsonReader.Token.BOOLEAN) reader.nextBoolean() else { reader.skipValue(); false }
                                    }
                                    fieldName.equals("Retired", ignoreCase = true) -> {
                                        retired = if (reader.peek() == JsonReader.Token.BOOLEAN) reader.nextBoolean() else { reader.skipValue(); false }
                                    }
                                    fieldName.equals("Stopped", ignoreCase = true) -> {
                                        stopped = if (reader.peek() == JsonReader.Token.BOOLEAN) reader.nextBoolean() else { reader.skipValue(); false }
                                    }
                                    fieldName.equals("KnockedOut", ignoreCase = true) -> {
                                        knockedOut = if (reader.peek() == JsonReader.Token.BOOLEAN) reader.nextBoolean() else { reader.skipValue(); false }
                                    }
                                    fieldName.equals("BestLapTime", ignoreCase = true) -> {
                                        if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                            reader.beginObject()
                                            while (reader.hasNext()) {
                                                val k = reader.nextName()
                                                if (k.equals("Value", ignoreCase = true) && reader.peek() == JsonReader.Token.STRING) {
                                                    bestLap = reader.nextString().takeIf { it.isNotBlank() }
                                                } else {
                                                    reader.skipValue()
                                                }
                                            }
                                            reader.endObject()
                                        } else {
                                            reader.skipValue()
                                        }
                                    }
                                    fieldName.equals("LastLapTime", ignoreCase = true) -> {
                                        if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                            reader.beginObject()
                                            while (reader.hasNext()) {
                                                val k = reader.nextName()
                                                if (k.equals("Value", ignoreCase = true) && reader.peek() == JsonReader.Token.STRING) {
                                                    lastLap = reader.nextString().takeIf { it.isNotBlank() }
                                                } else {
                                                    reader.skipValue()
                                                }
                                            }
                                            reader.endObject()
                                        } else {
                                            reader.skipValue()
                                        }
                                    }
                                    fieldName.equals("Stats", ignoreCase = true) -> {
                                        if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                                            reader.beginArray()
                                            while (reader.hasNext()) {
                                                if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                                    reader.beginObject()
                                                    while (reader.hasNext()) {
                                                        val sk = reader.nextName()
                                                        if (sk.equals("TimeDiffToFastest", ignoreCase = true) && reader.peek() == JsonReader.Token.STRING) {
                                                            val s = reader.nextString()
                                                            if (s.isNotBlank()) gapToLeader = s
                                                        } else if (sk.equals("TimeDifftoPositionAhead", ignoreCase = true) && reader.peek() == JsonReader.Token.STRING) {
                                                            val s = reader.nextString()
                                                            if (s.isNotBlank()) intervalToAhead = s
                                                        } else {
                                                            reader.skipValue()
                                                        }
                                                    }
                                                    reader.endObject()
                                                } else {
                                                    reader.skipValue()
                                                }
                                            }
                                            reader.endArray()
                                        } else if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                            reader.beginObject()
                                            while (reader.hasNext()) {
                                                reader.nextName()
                                                if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                                    reader.beginObject()
                                                    while (reader.hasNext()) {
                                                        val sk = reader.nextName()
                                                        if (sk.equals("TimeDiffToFastest", ignoreCase = true) && reader.peek() == JsonReader.Token.STRING) {
                                                            val s = reader.nextString()
                                                            if (s.isNotBlank()) gapToLeader = s
                                                        } else if (sk.equals("TimeDifftoPositionAhead", ignoreCase = true) && reader.peek() == JsonReader.Token.STRING) {
                                                            val s = reader.nextString()
                                                            if (s.isNotBlank()) intervalToAhead = s
                                                        } else {
                                                            reader.skipValue()
                                                        }
                                                    }
                                                    reader.endObject()
                                                } else {
                                                    reader.skipValue()
                                                }
                                            }
                                            reader.endObject()
                                        } else {
                                            reader.skipValue()
                                        }
                                    }
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()

                            lines.add(
                                SnapshotTimingDriverLine(
                                    racingNumber = racingNum,
                                    position = pos,
                                    bestLapTime = bestLap,
                                    lastLapTime = lastLap,
                                    gapToLeader = gapToLeader,
                                    intervalToAhead = intervalToAhead,
                                    inPit = inPit,
                                    pitOut = pitOut,
                                    retired = retired,
                                    stopped = stopped,
                                    knockedOut = knockedOut
                                )
                            )
                        } else {
                            reader.skipValue()
                        }
                    }
                    reader.endObject()
                } else {
                    reader.skipValue()
                }
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()

        return SnapshotTimingData(lines = lines)
    }

    override fun toJson(writer: JsonWriter, value: SnapshotTimingData?) {
        writer.nullValue()
    }
}

class SnapshotTimingDataFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) == SnapshotTimingData::class.java) {
            return SnapshotTimingDataAdapter()
        }
        return null
    }
}

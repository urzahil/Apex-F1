package com.example.data.api

import com.example.data.model.LapCountData
import com.example.data.model.SnapshotTimingData
import com.example.data.model.SnapshotTimingDriverLine
import com.example.data.model.TimingDriverLine
import com.example.data.model.TimingResponse
import com.example.data.model.TopThreeData
import com.example.data.model.TopThreeDriver
import com.example.data.model.TyreStint
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

class StringOrNumberAdapter : JsonAdapter<String>() {
    override fun fromJson(reader: JsonReader): String? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> reader.nextNull()
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.NUMBER -> reader.nextString()
            JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    override fun toJson(writer: JsonWriter, value: String?) {
        if (value == null) writer.nullValue() else writer.value(value)
    }
}

class StringOrNumberAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        return if (type == String::class.java && annotations.isEmpty()) StringOrNumberAdapter() else null
    }
}

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

                name.equals("Lines", ignoreCase = true) || name.equals(
                    "lines",
                    ignoreCase = true
                ) -> {
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
    private val driverAdapter: JsonAdapter<TimingDriverLine>,
    private val lapCountAdapter: JsonAdapter<LapCountData>
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
        var lapCount: LapCountData? = null
        val drivers = mutableListOf<TimingDriverLine>()

        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            when {
                name.equals("timestamp", ignoreCase = true) -> {
                    timestamp =
                        if (reader.peek() == JsonReader.Token.STRING) reader.nextString() else {
                            reader.skipValue(); null
                        }
                }

                name.equals("session", ignoreCase = true) -> {
                    session =
                        if (reader.peek() == JsonReader.Token.STRING) reader.nextString() else {
                            reader.skipValue(); null
                        }
                }

                name.equals("live", ignoreCase = true) -> {
                    live =
                        if (reader.peek() == JsonReader.Token.BOOLEAN) reader.nextBoolean() else {
                            reader.skipValue(); null
                        }
                }

                name.equals("stale", ignoreCase = true) -> {
                    stale =
                        if (reader.peek() == JsonReader.Token.BOOLEAN) reader.nextBoolean() else {
                            reader.skipValue(); null
                        }
                }

                name.equals("stale_reason", ignoreCase = true) -> {
                    staleReason =
                        if (reader.peek() == JsonReader.Token.STRING) reader.nextString() else {
                            reader.skipValue(); null
                        }
                }

                name.equals("lap_count", ignoreCase = true) || name.equals(
                    "lapCount",
                    ignoreCase = true
                ) -> {
                    lapCount = if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                        lapCountAdapter.fromJson(reader)
                    } else {
                        reader.skipValue()
                        null
                    }
                }

                name.equals("drivers", ignoreCase = true) || name.equals(
                    "Lines",
                    ignoreCase = true
                ) || name.equals("lines", ignoreCase = true) -> {
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
                                        val withKey =
                                            if (drv.driverNumber == null && drv.racingNumber == null && key.toIntOrNull() != null) {
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
            drivers = drivers.sortedBy { it.getDisplayPosition().toIntOrNull() ?: 999 },
            lapCount = lapCount
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
        if (value.lapCount != null) {
            writer.name("lap_count")
            lapCountAdapter.toJson(writer, value.lapCount)
        }
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
            val lapCountAdapter = moshi.adapter(LapCountData::class.java)
            return TimingResponseAdapter(driverAdapter, lapCountAdapter)
        }
        return null
    }
}

class HistoryTimingDataAdapter(
    private val lineAdapter: JsonAdapter<com.example.data.model.HistoryTimingLine>
) : JsonAdapter<com.example.data.model.HistoryTimingDataResponse>() {

    override fun fromJson(reader: JsonReader): com.example.data.model.HistoryTimingDataResponse? {
        if (reader.peek() == JsonReader.Token.NULL) return reader.nextNull()
        if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }

        val lines = linkedMapOf<String, com.example.data.model.HistoryTimingLine>()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (name.equals(
                    "Lines",
                    ignoreCase = true
                ) && reader.peek() == JsonReader.Token.BEGIN_OBJECT
            ) {
                reader.beginObject()
                while (reader.hasNext()) {
                    val driverNumber = reader.nextName()
                    if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                        lineAdapter.fromJson(reader)?.let { lines[driverNumber] = it }
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            } else if (reader.peek() == JsonReader.Token.BEGIN_OBJECT && name.toIntOrNull() != null) {
                // Some /history/session?topic=TimingData responses expose the driver
                // map directly at the root rather than under "Lines".
                lineAdapter.fromJson(reader)?.let { lines[name] = it }
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
        return com.example.data.model.HistoryTimingDataResponse(lines = lines)
    }

    override fun toJson(
        writer: JsonWriter,
        value: com.example.data.model.HistoryTimingDataResponse?
    ) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("Lines")
        writer.beginObject()
        value.lines?.forEach { (driverNumber, line) ->
            writer.name(driverNumber)
            lineAdapter.toJson(writer, line)
        }
        writer.endObject()
        writer.endObject()
    }
}

class HistoryTimingDataFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) == com.example.data.model.HistoryTimingDataResponse::class.java) {
            return HistoryTimingDataAdapter(moshi.adapter(com.example.data.model.HistoryTimingLine::class.java))
        }
        return null
    }
}

class SnapshotTimingDataAdapter(
    private val moshiTyreStintAdapter: JsonAdapter<TyreStint>
) : JsonAdapter<SnapshotTimingData>() {
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
                            val stints = mutableListOf<TyreStint>()

                            reader.beginObject()
                            while (reader.hasNext()) {
                                val fieldName = reader.nextName()
                                when {
                                    fieldName.equals(
                                        "Position",
                                        ignoreCase = true
                                    ) || fieldName.equals("Line", ignoreCase = true) -> {
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
                                        inPit =
                                            if (reader.peek() == JsonReader.Token.BOOLEAN) reader.nextBoolean() else {
                                                reader.skipValue(); false
                                            }
                                    }

                                    fieldName.equals("PitOut", ignoreCase = true) -> {
                                        pitOut =
                                            if (reader.peek() == JsonReader.Token.BOOLEAN) reader.nextBoolean() else {
                                                reader.skipValue(); false
                                            }
                                    }

                                    fieldName.equals("Retired", ignoreCase = true) -> {
                                        retired =
                                            if (reader.peek() == JsonReader.Token.BOOLEAN) reader.nextBoolean() else {
                                                reader.skipValue(); false
                                            }
                                    }

                                    fieldName.equals("Stopped", ignoreCase = true) -> {
                                        stopped =
                                            if (reader.peek() == JsonReader.Token.BOOLEAN) reader.nextBoolean() else {
                                                reader.skipValue(); false
                                            }
                                    }

                                    fieldName.equals("KnockedOut", ignoreCase = true) -> {
                                        knockedOut =
                                            if (reader.peek() == JsonReader.Token.BOOLEAN) reader.nextBoolean() else {
                                                reader.skipValue(); false
                                            }
                                    }

                                    fieldName.equals("BestLapTime", ignoreCase = true) -> {
                                        if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                            reader.beginObject()
                                            while (reader.hasNext()) {
                                                val k = reader.nextName()
                                                if (k.equals(
                                                        "Value",
                                                        ignoreCase = true
                                                    ) && reader.peek() == JsonReader.Token.STRING
                                                ) {
                                                    bestLap = reader.nextString()
                                                        .takeIf { it.isNotBlank() }
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
                                                if (k.equals(
                                                        "Value",
                                                        ignoreCase = true
                                                    ) && reader.peek() == JsonReader.Token.STRING
                                                ) {
                                                    lastLap = reader.nextString()
                                                        .takeIf { it.isNotBlank() }
                                                } else {
                                                    reader.skipValue()
                                                }
                                            }
                                            reader.endObject()
                                        } else {
                                            reader.skipValue()
                                        }
                                    }

                                    fieldName.equals("Stints", ignoreCase = true) -> {
                                        when (reader.peek()) {
                                            JsonReader.Token.BEGIN_OBJECT -> {
                                                reader.beginObject()
                                                while (reader.hasNext()) {
                                                    reader.nextName()
                                                    if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                                        moshiTyreStintAdapter.fromJson(reader)
                                                            ?.let { stints.add(it) }
                                                    } else {
                                                        reader.skipValue()
                                                    }
                                                }
                                                reader.endObject()
                                            }

                                            JsonReader.Token.BEGIN_ARRAY -> {
                                                reader.beginArray()
                                                while (reader.hasNext()) {
                                                    if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                                        moshiTyreStintAdapter.fromJson(reader)
                                                            ?.let { stints.add(it) }
                                                    } else {
                                                        reader.skipValue()
                                                    }
                                                }
                                                reader.endArray()
                                            }

                                            else -> reader.skipValue()
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
                                                        if (sk.equals(
                                                                "TimeDiffToFastest",
                                                                ignoreCase = true
                                                            ) && reader.peek() == JsonReader.Token.STRING
                                                        ) {
                                                            val s = reader.nextString()
                                                            if (s.isNotBlank()) gapToLeader = s
                                                        } else if (sk.equals(
                                                                "TimeDifftoPositionAhead",
                                                                ignoreCase = true
                                                            ) && reader.peek() == JsonReader.Token.STRING
                                                        ) {
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
                                                        if (sk.equals(
                                                                "TimeDiffToFastest",
                                                                ignoreCase = true
                                                            ) && reader.peek() == JsonReader.Token.STRING
                                                        ) {
                                                            val s = reader.nextString()
                                                            if (s.isNotBlank()) gapToLeader = s
                                                        } else if (sk.equals(
                                                                "TimeDifftoPositionAhead",
                                                                ignoreCase = true
                                                            ) && reader.peek() == JsonReader.Token.STRING
                                                        ) {
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
                                    knockedOut = knockedOut,
                                    stints = stints
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
            return SnapshotTimingDataAdapter(moshi.adapter(TyreStint::class.java))
        }
        return null
    }
}

package com.example.data.api

import com.example.data.model.TopThreeDriver
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

/**
 * The live feed has used both an array and a keyed object for top-three lines.
 * Accept both representations so a live event cannot crash the refresh.
 */
class TopThreeLinesJsonAdapter private constructor(
    private val driverAdapter: JsonAdapter<TopThreeDriver>
) : JsonAdapter<List<TopThreeDriver>>() {

    override fun fromJson(reader: JsonReader): List<TopThreeDriver> = when (reader.peek()) {
        JsonReader.Token.NULL -> {
            reader.nextNull<Unit>()
            emptyList()
        }
        JsonReader.Token.BEGIN_ARRAY -> {
            reader.beginArray()
            buildList {
                while (reader.hasNext()) driverAdapter.fromJson(reader)?.let(::add)
            }.also { reader.endArray() }
        }
        JsonReader.Token.BEGIN_OBJECT -> {
            reader.beginObject()
            buildList {
                while (reader.hasNext()) {
                    reader.nextName()
                    driverAdapter.fromJson(reader)?.let(::add)
                }
            }.also { reader.endObject() }
        }
        else -> {
            reader.skipValue()
            emptyList()
        }
    }

    override fun toJson(writer: JsonWriter, value: List<TopThreeDriver>?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginArray()
        value.forEach { driverAdapter.toJson(writer, it) }
        writer.endArray()
    }

    companion object {
        fun factory(): JsonAdapter.Factory = object : JsonAdapter.Factory {
            override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
                if (annotations.isNotEmpty() || Types.getRawType(type) != List::class.java) return null
                val arguments = Types.getParameterUpperBounds(type)
                if (arguments.size != 1 || arguments[0] != TopThreeDriver::class.java) return null
                return TopThreeLinesJsonAdapter(
                    moshi.nextAdapter(this, arguments[0], emptySet())
                )
            }
        }
    }
}
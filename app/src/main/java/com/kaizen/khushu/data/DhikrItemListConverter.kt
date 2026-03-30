package com.kaizen.khushu.data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DhikrItemListConverter {
    @TypeConverter
    fun fromJson(value: String): List<DhikrItem> = Json.decodeFromString(value)

    @TypeConverter
    fun toJson(items: List<DhikrItem>): String = Json.encodeToString(items)
}

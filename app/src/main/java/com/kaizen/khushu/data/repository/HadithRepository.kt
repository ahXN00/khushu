package com.kaizen.khushu.data.repository

import android.content.Context
import com.kaizen.khushu.data.model.HadithBook
import com.kaizen.khushu.data.model.HadithSection
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

object HadithRepository {
    private val bookCache = ConcurrentHashMap<String, JsonObject>()
    private val json = Json { ignoreUnknownKeys = true }

    private fun loadBook(context: Context, bookId: String): JsonObject {
        bookCache[bookId]?.let { return it }
        val filename = "en.$bookId.json"
        return try {
            val raw = context.assets.open("hadith/$filename").bufferedReader().use { it.readText() }
            val obj = json.parseToJsonElement(raw).jsonObject
            bookCache[bookId] = obj
            obj
        } catch (_: Exception) {
            JsonObject(emptyMap())
        }
    }

    fun getSections(context: Context, bookId: String): List<HadithSection> {
        val book = loadBook(context, bookId)
        val metadata = book["metadata"]?.jsonObject ?: return emptyList()
        val sections = metadata["sections"]?.jsonObject ?: return emptyList()
        
        return sections.entries.mapNotNull { (key, value) ->
            val num = key.toIntOrNull() ?: return@mapNotNull null
            val title = value.jsonPrimitive.content
            if (title.isBlank()) return@mapNotNull null
            HadithSection(num, title)
        }.sortedBy { it.number }
    }

    fun getHadiths(context: Context, bookId: String, sectionNumber: Int): List<JsonObject> {
        val book = loadBook(context, bookId)
        val hadiths = book["hadiths"]?.jsonArray ?: return emptyList()
        
        return hadiths.map { it.jsonObject }.filter { 
            it["reference"]?.jsonObject?.get("book")?.jsonPrimitive?.intOrNull == sectionNumber
        }
    }
}

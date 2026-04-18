package com.kaizen.khushu.data.repository

import android.content.Context
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

object QuranAudioRepository {
    private val manifestCache = ConcurrentHashMap<String, Map<Int, String>>()
    private val json = Json { ignoreUnknownKeys = true }

    fun getManifest(context: Context, reciterId: String): Map<Int, String> {
        manifestCache[reciterId]?.let { return it }
        
        val raw = try {
            context.assets.open("quran/audio/$reciterId.json")
                .bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            return emptyMap()
        }

        return try {
            val stringMap = json.decodeFromString<Map<String, String>>(raw)
            val intMap = stringMap.mapKeys { it.key.toInt() }
            manifestCache[reciterId] = intMap
            intMap
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun getUrl(context: Context, reciterId: String, surahNumber: Int): String? {
        val manifest = getManifest(context, reciterId)
        return manifest[surahNumber]
    }
}

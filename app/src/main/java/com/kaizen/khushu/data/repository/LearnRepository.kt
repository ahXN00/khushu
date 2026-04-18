package com.kaizen.khushu.data.repository

import android.content.Context
import com.kaizen.khushu.data.model.ContentBlock
import com.kaizen.khushu.data.model.LearnSection
import com.kaizen.khushu.data.model.LearnTopic
import com.kaizen.khushu.data.model.TopicJson
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

object LearnRepository {
    private val blockCache = ConcurrentHashMap<String, List<ContentBlock>>()

    fun getSections(): List<LearnSection> {
        return listOf(
            LearnSection(
                id = "foundations",
                sectionTitle = "Foundations",
                color = 0xFF2A4B7CL,
                topics = listOf(
                    LearnTopic(id = "foundations_intention", title = "The Role of Intention (Niyyah)", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "foundations_sincerity", title = "Sincerity (Ikhlas) in Worship", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "foundations_knowledge", title = "Seeking Islamic Knowledge", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "foundations_pillars", title = "The Five Pillars of Islam", arabicText = "", translations = mapOf("en" to ""))
                )
            ),
            LearnSection(
                id = "purification",
                sectionTitle = "Purification",
                color = 0xFF357B83L,
                topics = listOf(
                    LearnTopic(id = "purification_types", title = "Types of Purification in Islam", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "purification_istinja", title = "Istinja and Istijmar", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "wudu_step_by_step", title = "Step-by-Step Wudu", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "wudu_invalidators", title = "What Breaks Wudu", arabicText = "", translations = mapOf("en" to ""))
                )
            ),
            LearnSection(
                id = "prayer",
                sectionTitle = "The Prayer",
                color = 0xFF2D5A4CL,
                topics = listOf(
                    LearnTopic(id = "salah_obligation", title = "The Obligation of Salah", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "salah_times", title = "The Five Prayer Times", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "salah_rakaat", title = "Rakaat Count for Each Prayer", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "salah_khushoo", title = "Achieving Khushoo in Salah", arabicText = "", translations = mapOf("en" to ""))
                )
            ),
            LearnSection(
                id = "duas_adhkar",
                sectionTitle = "Duas & Adhkar",
                color = 0xFFA87B61L,
                topics = listOf(
                    LearnTopic(id = "surah_fatiha", title = "Surah Al-Fatiha", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "dua_morning_evening", title = "Morning and Evening Adhkar", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "dua_before_sleep", title = "Duas Before Sleep", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "duas_after_fard", title = "Duas After Obligatory Prayer", arabicText = "", translations = mapOf("en" to ""))
                )
            )
        )
    }

    private val blockJson = Json { ignoreUnknownKeys = true }

    suspend fun getBlocks(topicId: String, context: Context): List<ContentBlock> {
        blockCache[topicId]?.let { return it }

        val raw = try {
            context.assets.open("learn/$topicId.json")
                .bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            return emptyList()
        }

        return try {
            blockJson.decodeFromString<TopicJson>(raw).blocks
        } catch (_: Exception) {
            emptyList()
        }.also { blockCache[topicId] = it }
    }
}

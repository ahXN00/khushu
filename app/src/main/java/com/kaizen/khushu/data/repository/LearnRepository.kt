package com.kaizen.khushu.data.repository

import android.content.Context
import com.kaizen.khushu.data.local.KhushuDatabase
import com.kaizen.khushu.data.model.AyahBlock
import com.kaizen.khushu.data.model.ContentBlock
import com.kaizen.khushu.data.model.HadithBlock
import com.kaizen.khushu.data.model.LearnSection
import com.kaizen.khushu.data.model.LearnTopic
import com.kaizen.khushu.data.model.TopicJson
import com.kaizen.khushu.data.model.WordData
import kotlinx.serialization.json.Json

object LearnRepository {
    fun getSections(): List<LearnSection> {
        return listOf(
            LearnSection(
                id = "foundations",
                sectionTitle = "Foundations",
                color = 0xFF4A3B6BL,
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
                color = 0xFF8B5A4BL,
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
                color = 0xFF2E5A4EL,
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
                color = 0xFFD4AF37L,
                topics = listOf(
                    LearnTopic(id = "surah_fatiha", title = "Surah Al-Fatiha", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "dua_morning_evening", title = "Morning and Evening Adhkar", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "dua_before_sleep", title = "Duas Before Sleep", arabicText = "", translations = mapOf("en" to "")),
                    LearnTopic(id = "duas_after_fard", title = "Duas After Obligatory Prayer", arabicText = "", translations = mapOf("en" to ""))
                )
            )
        )
    }

    // ── Block-based content loading ────────────────────────────────────────────

    private val blockJson = Json { ignoreUnknownKeys = true }

    /**
     * Loads the JSON asset for [topicId] from assets/learn/<topicId>.json,
     * deserializes blocks, and resolves [AyahBlock]/[HadithBlock] references
     * from Room. Returns an empty list if the file is missing or parsing fails.
     *
     * Must be called from a coroutine (suspend).
     */
    suspend fun getBlocks(topicId: String, context: Context): List<ContentBlock> {
        val raw = try {
            context.assets.open("learn/$topicId.json")
                .bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            return emptyList()
        }

        val topicJson = try {
            blockJson.decodeFromString<TopicJson>(raw)
        } catch (_: Exception) {
            return emptyList()
        }

        val db = try {
            KhushuDatabase.getInstance(context)
        } catch (_: Exception) {
            return topicJson.blocks // DB unavailable — return blocks without enrichment
        }

        return topicJson.blocks.map { block ->
            try {
                when (block) {
                    is AyahBlock -> {
                        val entity = db.ayahDao().getAyah(block.surah, block.ayah)
                        if (entity != null) block.copy(
                            textUthmani = entity.textUthmani,
                            tajweedMarkup = entity.tajweedMarkup,
                        ) else block
                    }
                    is HadithBlock -> {
                        val entity = db.hadithDao().getHadith(block.collection, block.number)
                        if (entity != null) block.copy(
                            textArabic = entity.textArabic,
                            textEn = entity.textEn,
                            grade = entity.grade,
                            narrator = entity.narrator,
                        ) else block
                    }
                    else -> block
                }
            } catch (_: Exception) {
                block // DB query failed — render block without enrichment
            }
        }
    }
}
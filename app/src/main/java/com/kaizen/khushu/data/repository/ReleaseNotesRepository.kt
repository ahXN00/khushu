package com.kaizen.khushu.data.repository

import android.content.Context
import com.kaizen.khushu.data.model.ReleaseNote
import kotlinx.serialization.json.Json

class ReleaseNotesRepository(
    private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun noteForVersion(versionCode: Int): ReleaseNote? =
        loadAll().firstOrNull { it.versionCode == versionCode }

    private fun loadAll(): List<ReleaseNote> =
        runCatching {
            context.assets.open(RELEASE_NOTES_ASSET).bufferedReader().use { reader ->
                json.decodeFromString<List<ReleaseNote>>(reader.readText())
            }
        }.getOrDefault(emptyList())

    private companion object {
        const val RELEASE_NOTES_ASSET = "release_notes.json"
    }
}

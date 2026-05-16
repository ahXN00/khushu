package com.kaizen.khushu.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseNote(
    val versionCode: Int,
    val versionName: String,
    val headline: String,
    val highlights: List<String> = emptyList(),
    val sections: List<ReleaseNoteSection> = emptyList(),
)

@Serializable
data class ReleaseNoteSection(
    val title: String,
    val items: List<String> = emptyList(),
)

package com.kaizen.khushu.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SurahMeta(
    val id: Int,
    val nameSimple: String,
    val nameArabic: String,
    val versesCount: Int,
    val revelationPlace: String,
    val revelationOrder: Int,
)

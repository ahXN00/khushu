package com.kaizen.khushu.data

import kotlinx.serialization.Serializable

@Serializable
data class DhikrItem(
    val name: String,
    val targetCount: Int,
)

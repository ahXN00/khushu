package com.kaizen.khushu.data.model

data class LearnSection(
    val id: String,
    val sectionTitle: String,
    val topics: List<LearnTopic>,
    val color: Long = 0xFF3B4A6BL,
)
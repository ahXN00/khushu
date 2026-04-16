package com.kaizen.khushu.data.model

data class WordData(
    val id: String,
    val arabic: String,
    val transliteration: String? = null,
    val translation: String = ""
)
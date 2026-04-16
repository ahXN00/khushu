package com.kaizen.khushu.data.model

data class LearnTopic(
    val id: String,
    val title: String,
    val arabicText: String,
    val arabicScripts: Map<String, String> = emptyMap(), // "indopak", "noDiacritics", "mushaf"
    val tajweedMarkup: String? = null,                  // phase 3: tagged markup string
    val words: List<WordData> = emptyList(),
    val translations: Map<String, String> = emptyMap(),
    val transliteration: Map<String, String> = emptyMap(), // "en_latin", "ur_roman"
    val referenceSource: String? = null,
    val referenceNumber: String? = null,
    val audioFilename: String? = null,
    var isMastered: Boolean = false
)
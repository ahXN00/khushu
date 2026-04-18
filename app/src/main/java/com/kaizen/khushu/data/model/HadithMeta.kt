package com.kaizen.khushu.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HadithBook(
    val id: String,          // e.g. "bukhari"
    val name: String,        // e.g. "Sahih al-Bukhari"
    val description: String,
)

@Serializable
data class HadithSection(
    val number: Int,
    val title: String,
)

val BUNDLED_HADITH_BOOKS = listOf(
    HadithBook("bukhari",  "Sahih al-Bukhari",   "The most authentic collection of Prophetic traditions."),
    HadithBook("muslim",   "Sahih Muslim",       "A highly respected collection focusing on authentic chains."),
    HadithBook("abudawud", "Sunan Abi Dawud",    "Collected by Abu Dawud, focusing on legal traditions."),
    HadithBook("tirmidhi", "Jami` at-Tirmidhi",  "A collection with detailed analysis of hadith grades."),
    HadithBook("ibnmajah", "Sunan Ibn Majah",    "One of the six major collections of Prophetic hadith."),
)

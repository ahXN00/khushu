package com.kaizen.khushu.ui.theme

import androidx.compose.ui.graphics.Color

object KhushuColors {
    // Card Accents (Shared across Learn and Tasbeeh)
    val SalahBasics = Color(0xFF3B4A6B)
    val Fatiha = Color(0xFF4A3B6B)
    val PrayerTimes = Color(0xFF2E5A4E) // Deep Emerald
    val Wudu = Color(0xFF8B5A4B) // Burnished Terracotta
    val Qibla = Color(0xFF3B6B6B)
    val Friday = Color(0xFF6B4C7A) // Muted Amethyst
    
    val MorningAdhkar = Color(0xFF4A5568) // Steel Slate
    val EveningAdhkar = Color(0xFFD4AF37) // Metallic Gold
    val AfterSalahDua = Color(0xFF3B4D61) // Twilight Blue

    // Collection Accents (Tasbeeh & Learn Lists)
    val Palette = listOf(
        Color(0xFFD4AF37), // Metallic Gold
        Color(0xFF2E5A4E), // Deep Emerald
        Color(0xFF4A5568), // Steel Slate
        Color(0xFF6B4C7A), // Muted Amethyst
        Color(0xFF8B5A4B), // Burnished Terracotta
        Color(0xFF3B4D61), // Twilight Blue
        Color(0xFF3B4A6B), // Deep Blue
        Color(0xFF4A3B6B), // Deep Purple
    )

    fun getAccent(index: Int): Color = Palette[index % Palette.size]
}

val prayerCardPalette = listOf(
    "Salah Basics" to KhushuColors.SalahBasics,
    "Surah Al-Fatiha" to KhushuColors.Fatiha,
    "Prayer Times" to KhushuColors.PrayerTimes,
    "Wudu Guide" to KhushuColors.Wudu,
    "Qibla Direction" to KhushuColors.Qibla,
    "Friday Prayer" to KhushuColors.Friday,
)

val duaCardPalette = listOf(
    "Morning Adhkar" to KhushuColors.MorningAdhkar,
    "Evening Adhkar" to KhushuColors.EveningAdhkar,
    "Dua After Salah" to KhushuColors.AfterSalahDua,
)

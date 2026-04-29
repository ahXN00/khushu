package com.kaizen.khushu.data.repository

data class ExtraPrayerTimingDefinition(
    val id: String,
    val label: String,
    val shortLabel: String,
    val arabicLabel: String,
)

val EXTRA_PRAYER_TIMINGS = listOf(
    ExtraPrayerTimingDefinition(
        id = "IMSAK",
        label = "Imsak",
        shortLabel = "Imsak",
        arabicLabel = "إمساك",
    ),
    ExtraPrayerTimingDefinition(
        id = "SUNRISE",
        label = "Sunrise",
        shortLabel = "Sunrise",
        arabicLabel = "شروق",
    ),
    ExtraPrayerTimingDefinition(
        id = "SUNSET",
        label = "Sunset",
        shortLabel = "Sunset",
        arabicLabel = "غروب",
    ),
    ExtraPrayerTimingDefinition(
        id = "FIRST_THIRD",
        label = "First Third",
        shortLabel = "1st Third",
        arabicLabel = "ثلث أول",
    ),
    ExtraPrayerTimingDefinition(
        id = "MIDNIGHT",
        label = "Midnight",
        shortLabel = "Midnight",
        arabicLabel = "منتصف الليل",
    ),
    ExtraPrayerTimingDefinition(
        id = "LAST_THIRD",
        label = "Last Third",
        shortLabel = "Last Third",
        arabicLabel = "الثلث الأخير",
    ),
)

fun extraPrayerTimingLabel(id: String): String {
    return EXTRA_PRAYER_TIMINGS.firstOrNull { it.id == id }?.label ?: id
}

fun extraPrayerTimingShortLabel(id: String): String {
    return EXTRA_PRAYER_TIMINGS.firstOrNull { it.id == id }?.shortLabel ?: id
}

fun extraPrayerTimingArabicLabel(id: String): String {
    return EXTRA_PRAYER_TIMINGS.firstOrNull { it.id == id }?.arabicLabel.orEmpty()
}

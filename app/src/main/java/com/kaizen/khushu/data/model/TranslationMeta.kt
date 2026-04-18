package com.kaizen.khushu.data.model

data class TranslationMeta(
    val id: String,           // e.g. "en_20" (matches asset/file name)
    val langCode: String,     // ISO 639-1: "en", "ur", "tr", etc.
    val langName: String,     // "English"
    val translatorName: String, // "Sahih International"
    val isRtl: Boolean,
    val downloadUrl: String,
    val sizeKb: Int,
)

val AVAILABLE_TRANSLATIONS = listOf(
    TranslationMeta("en_20", "en", "English", "Sahih International", false,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/eng-abdelhaleem.json", 380),
    TranslationMeta("ur_54", "ur", "Urdu", "Maulana Fateh Muhammad Jalandhari", true,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/urd-fatehmuhammadja.json", 420),
    TranslationMeta("tur-diyanetisleri", "tr", "Turkish", "Diyanet İşleri", false,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/tur-diyanetisleri.json", 390),
    TranslationMeta("fra-muhammadhamidul", "fr", "French", "Muhammad Hamidullah", false,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/fra-muhammadhamidul.json", 400),
    TranslationMeta("deu-asfbubenheimand", "de", "German", "Bubenheim & Elyas", false,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/deu-asfbubenheimand.json", 410),
    TranslationMeta("ind-indonesianislam", "id", "Indonesian", "Kementerian Agama RI", false,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/ind-indonesianislam.json", 350),
    TranslationMeta("ben-muhiuddinkhan", "bn", "Bengali", "Muhiuddin Khan", false,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/ben-muhiuddinkhan.json", 430),
    TranslationMeta("rus-elmirkuliev", "ru", "Russian", "Elmir Kuliev", false,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/rus-elmirkuliev.json", 420),
)

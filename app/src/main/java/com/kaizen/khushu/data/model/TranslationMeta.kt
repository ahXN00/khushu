package com.kaizen.khushu.data.model

data class TranslationMeta(
    val id: String,           // matches filename in assets/translations/ or filesDir/translations/
    val langCode: String,     // ISO 639-1: "en", "ur", "tr", etc.
    val langName: String,     // "English"
    val translatorName: String,
    val isRtl: Boolean,
    val downloadUrl: String,  // empty = bundled in assets
    val sizeKb: Int,
)

// Bundled (no download): en_20, ur_54
// Downloaded on demand (Fawaz Ahmed CDN): all others
val AVAILABLE_TRANSLATIONS = listOf(
    TranslationMeta("en_20",  "en", "English",    "Sahih International",      false, "",    926),
    TranslationMeta("en_19",  "en", "English",    "Pickthall",                false,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/eng-mpickthall.json", 370),
    TranslationMeta("ur_54",  "ur", "Urdu",       "Muhammad Junagarhi",       true,  "",    1500),
    TranslationMeta("ur_234", "ur", "Urdu",       "Fateh Muhammad Jalandhry", true,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/urd-fatehmuhammadja.json", 420),
    TranslationMeta("tr_77",  "tr", "Turkish",    "Diyanet İşleri",           false,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/tur-diyanetisleri.json", 390),
    TranslationMeta("fr_31",  "fr", "French",     "Muhammad Hamidullah",      false,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/fra-muhammadhamidul.json", 400),
    TranslationMeta("de_27",  "de", "German",     "Bubenheim & Elyas",        false,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/deu-asfbubenheimand.json", 410),
    TranslationMeta("id_33",  "id", "Indonesian", "Kementerian Agama RI",     false,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/ind-indonesianislam.json", 350),
    TranslationMeta("bn_213", "bn", "Bengali",    "Muhiuddin Khan",           false,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/ben-muhiuddinkhan.json", 430),
    TranslationMeta("ru_45",  "ru", "Russian",    "Elmir Kuliev",             false,
        "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/rus-elmirkuliev.json", 420),
)

package com.kaizen.khushu.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ReciterMeta(
    val id: String,           // matches filename: "mishari", "abdulbaset", etc.
    val name: String,         // display name
    val style: String,        // "Murattal", "Mujawwad", etc.
)

val AVAILABLE_RECITERS = listOf(
    ReciterMeta("mishari",    "Mishari Al-Afasy",          "Murattal"),
    ReciterMeta("abdulbaset", "Abdul Baset",               "Mujawwad"),
    ReciterMeta("sudais",     "Abdurrahman Al-Sudais",     "Murattal"),
    ReciterMeta("husary",     "Mahmoud Khalil Al-Husary",  "Murattal"),
    ReciterMeta("minshawi",   "Mohamed Siddiq Al-Minshawi","Murattal"),
)

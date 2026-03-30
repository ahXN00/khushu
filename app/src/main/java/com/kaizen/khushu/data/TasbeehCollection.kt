package com.kaizen.khushu.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasbeeh_collections")
data class TasbeehCollection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String?,
    val colorInt: Int,
    val items: List<DhikrItem>,
)

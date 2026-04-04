package com.kaizen.khushu.data

data class CanvasPreset(
    val id: String,
    val name: String,
    val backgroundColor: Int,
    val widgets: List<CanvasWidget>,
    val isDeletable: Boolean
)
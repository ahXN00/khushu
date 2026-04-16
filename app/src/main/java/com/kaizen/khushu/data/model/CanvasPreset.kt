package com.kaizen.khushu.data.model
import com.kaizen.khushu.ui.screens.salah.CanvasWidget

data class CanvasPreset(
    val id: String,
    val name: String,
    val backgroundColor: Int,
    val widgets: List<CanvasWidget>,
    val isDeletable: Boolean
)
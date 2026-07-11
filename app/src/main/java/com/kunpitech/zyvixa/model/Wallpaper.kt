package com.kunpitech.zyvixa.model

import androidx.compose.ui.graphics.Color

data class LiveWallpaper(
    val id: String,
    val title: String,
    val description: String,
    val source: String,
    val isRemote: Boolean,
    val gradientColors: List<Color>,
    val imageUrl: String? = null,
    val category: String = "All"
)

data class StaticWallpaper(
    val id: String,
    val name: String,
    val url: String,
    val category: String = "All"
)

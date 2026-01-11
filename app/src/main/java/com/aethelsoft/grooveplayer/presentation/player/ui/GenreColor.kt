package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.ui.graphics.Color

fun genreColor(genre: String?): Color =
    when (genre?.lowercase()?.trim()) {
        "rock" -> Color(0xFFD32F2F)
        "pop" -> Color(0xFF1976D2)
        "jazz" -> Color(0xFF8E44AD)
        "metal" -> Color(0xFF424242)
        "edm" -> Color(0xFF00BCD4)
        "classical" -> Color(0xFF795548)
        else -> Color(0xFF616161)
    }
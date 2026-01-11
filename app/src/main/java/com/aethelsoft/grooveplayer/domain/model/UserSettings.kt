package com.aethelsoft.grooveplayer.domain.model

/**
 * Domain model representing user_category settings/preferences.
 */
data class UserSettings(
    val id: Int = 0,
    val lastPlayedSongsTimer: Int = 30,  // Default 30 days
    val fadeTimer: Int = 0                // Default 0 seconds (no fade)
)

package com.aethelsoft.grooveplayer.domain.model

/**
 * Domain model representing user settings/preferences.
 */
data class UserSettings(
    val id: Int = 0,
    val lastPlayedSongsTimer: Int = 3,  // Default 3 days
    val fadeTimer: Int = 0                // Default 0 seconds (no fade)
)

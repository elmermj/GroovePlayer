package com.aethelsoft.grooveplayer.domain.model

/**
 * Domain model representing user settings/preferences.
 */
data class UserSettings(
    val id: Int = 0,
    val lastPlayedSongsTimer: Int = 3,  // Default 3 days
    val fadeTimer: Int = 0,              // Default 0 seconds (no fade)
    val equalizerEnabled: Boolean = false,
    val equalizerPreset: Int = -1,       // -1 means custom
    val equalizerBandLevels: List<Int> = emptyList(), // Band levels in millibels
    val lastPlayedSongId: String? = null, // Last played song ID
    val lastPlayedPosition: Long = 0L,     // Last played position in milliseconds
    val shuffleEnabled: Boolean = false,    // Shuffle state
    val repeatMode: String = "OFF",        // Repeat mode: "OFF", "ALL", "ONE"
    val queueSongIds: List<String> = emptyList(), // Queue song IDs
    val queueStartIndex: Int = 0,          // Queue start index
    val isEndlessQueue: Boolean = false,   // Whether queue is endless
    val visualizationMode: VisualizationMode = VisualizationMode.SIMULATED, // OFF, SIMULATED, REAL_TIME
    val showMiniPlayerOnStart: Boolean = false,  // Whether to show mini player when app launches
    val excludedFolders: List<String> = emptyList()  // Folders to ignore during music scanning
)

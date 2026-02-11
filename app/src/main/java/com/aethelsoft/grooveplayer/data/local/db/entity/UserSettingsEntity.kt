package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val lastPlayedSongsTimer: Int = 0,
    val fadeTimer: Int = 0,
    val equalizerEnabled: Boolean = false,
    val equalizerPreset: Int = -1,
    val equalizerBandLevels: String = "", // Stored as comma-separated values
    val lastPlayedSongId: String? = null, // Last played song ID
    val lastPlayedPosition: Long = 0L,     // Last played position in milliseconds
    val shuffleEnabled: Boolean = false,    // Shuffle state
    val repeatMode: String = "OFF",         // Repeat mode: "OFF", "ALL", "ONE"
    val queueSongIds: String = "",          // Queue song IDs as comma-separated string
    val queueStartIndex: Int = 0,           // Queue start index
    val isEndlessQueue: Boolean = false,    // Whether queue is endless
    val visualizationMode: String = "SIMULATED", // "OFF", "SIMULATED", "REAL_TIME"
    val showMiniPlayerOnStart: Boolean = false,
    val excludedFolders: String = ""  // Delimiter-separated paths ("||") to ignore during scanning
)

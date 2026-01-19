package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: String,
    val songTitle: String,
    val artist: String,
    val album: String,
    val genre: String,
    val uri: String,
    val artworkUrl: String?,
    val playedAt: Long = System.currentTimeMillis()
)


package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_likes")
data class SongLikeEntity(
    @PrimaryKey
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val uri: String,
    val artworkUrl: String?,
    val durationMs: Long,
    val likedAt: Long,
)

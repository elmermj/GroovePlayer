package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_metadata")
data class SongMetadataEntity(
    @PrimaryKey
    val songId: String,
    val title: String,
    val genres: String, // Comma-separated list
    val artists: String, // Comma-separated list
    val album: String?,
    val year: Int?,
    val useAlbumYear: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)


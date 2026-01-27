package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "QUERY", "SONG", "ARTIST", "ALBUM"
    val query: String? = null, // For query type
    val itemId: String? = null, // For item types (songId, artistName, albumName)
    val itemTitle: String, // Display title
    val itemSubtitle: String? = null, // Artist name for songs/albums
    val artworkUrl: String? = null, // Artwork URL
    val timestamp: Long = System.currentTimeMillis()
)

package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey
    val name: String,
    val artist: String,
    val artworkUrl: String? = null,
    val year: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)


package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "albums",
    indices = [Index(value = ["name"])]
)
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val albumId: Long = 0L,
    val name: String,
    val artworkUrl: String? = null,
    val year: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)



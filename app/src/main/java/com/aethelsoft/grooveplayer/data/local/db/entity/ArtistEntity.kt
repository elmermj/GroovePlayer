package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey
    val name: String,
    val imageUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)


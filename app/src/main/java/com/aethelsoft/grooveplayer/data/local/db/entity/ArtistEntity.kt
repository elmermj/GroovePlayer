package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "artists",
    indices = [Index(value = ["name"], unique = true)]
)
data class ArtistEntity(
    @PrimaryKey(autoGenerate = true)
    val artistId: Long = 0L,
    val name: String,
    val genre: String? = null,
    val desc: String? = null,
    val country: String? = null,
    @ColumnInfo(name = "artist_image_url")
    val imageUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)



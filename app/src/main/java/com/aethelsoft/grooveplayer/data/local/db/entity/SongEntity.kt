package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["albumId"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("albumId"),
        Index(value = ["uri"], unique = true)
    ]
)
data class SongEntity(
    @PrimaryKey
    val songId: String,
    val albumId: Long? = null,
    val uri: String,
    val title: String,
    val trackNumber: Int? = null,
    val durationMs: Long? = null
)


package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A playlist row. Song fields are stored on the row so playback and M3U export
 * still work if the library index is refreshed.
 */
@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playlistId")],
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val position: Int,
    val songId: String,
    val title: String,
    val artist: String,
    val uri: String,
    val filePath: String?,
    val durationMs: Long,
    val artworkUrl: String?,
    val albumName: String?,
    val genre: String,
)

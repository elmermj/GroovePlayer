package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Playlist membership is the song's [contentHash] (SCRUM-84), not a path or
 * MediaStore id. [songId] is the id that hash had when the row was saved.
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
    indices = [Index("playlistId"), Index("contentHash")],
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val position: Int,
    val contentHash: String,
    val songId: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val artworkUrl: String?,
    val albumName: String?,
    val genre: String,
)

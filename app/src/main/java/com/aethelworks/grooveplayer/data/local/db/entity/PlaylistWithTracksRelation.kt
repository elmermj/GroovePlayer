package com.aethelworks.grooveplayer.data.local.db.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PlaylistWithTracksRelation(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId",
    )
    val tracks: List<PlaylistTrackEntity>,
)

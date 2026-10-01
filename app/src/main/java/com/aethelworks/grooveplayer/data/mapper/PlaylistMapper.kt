package com.aethelworks.grooveplayer.data.mapper

import com.aethelworks.grooveplayer.data.local.db.entity.PlaylistTrackEntity
import com.aethelworks.grooveplayer.data.local.db.entity.PlaylistWithTracksRelation
import com.aethelworks.grooveplayer.domain.model.Album
import com.aethelworks.grooveplayer.domain.model.Playlist
import com.aethelworks.grooveplayer.domain.model.PlaylistTrack
import com.aethelworks.grooveplayer.domain.model.PlaylistWithTracks
import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.model.makeAlbumId

object PlaylistMapper {
    fun toSummary(relation: PlaylistWithTracksRelation): Playlist {
        val ordered = relation.tracks.sortedWith(compareBy({ it.position }, { it.id }))
        return Playlist(
            id = relation.playlist.id,
            name = relation.playlist.name,
            createdAt = relation.playlist.createdAt,
            updatedAt = relation.playlist.updatedAt,
            trackCount = ordered.size,
            artworkUrls = ordered.mapNotNull { it.artworkUrl?.takeIf(String::isNotBlank) }
                .distinct()
                .take(4),
        )
    }

    fun toDomain(relation: PlaylistWithTracksRelation): PlaylistWithTracks {
        val ordered = relation.tracks.sortedWith(compareBy({ it.position }, { it.id }))
        return PlaylistWithTracks(
            playlist = toSummary(relation),
            tracks = ordered.map { entity ->
                PlaylistTrack(
                    entryId = entity.id,
                    position = entity.position,
                    song = snapshot(entity),
                    contentHash = entity.contentHash,
                    available = false,
                )
            },
        )
    }

    fun snapshot(entity: PlaylistTrackEntity, filePath: String? = null): Song {
        val album = entity.albumName?.takeIf { it.isNotBlank() }?.let { albumName ->
            Album(
                id = makeAlbumId(entity.artist, albumName),
                name = albumName,
                artist = entity.artist,
                artworkUrl = entity.artworkUrl,
                songs = emptyList(),
            )
        }
        return Song(
            id = entity.songId,
            title = entity.title,
            artist = entity.artist,
            uri = "",
            genre = entity.genre,
            durationMs = entity.durationMs,
            artworkUrl = entity.artworkUrl,
            album = album,
            filePath = filePath,
        )
    }

    fun toEntity(
        playlistId: Long,
        position: Int,
        song: Song,
        contentHash: String,
    ): PlaylistTrackEntity =
        PlaylistTrackEntity(
            playlistId = playlistId,
            position = position,
            contentHash = contentHash.lowercase(),
            songId = song.id,
            title = song.title,
            artist = song.artist,
            durationMs = song.durationMs,
            artworkUrl = song.artworkUrl,
            albumName = song.album?.name,
            genre = song.genre,
        )
}

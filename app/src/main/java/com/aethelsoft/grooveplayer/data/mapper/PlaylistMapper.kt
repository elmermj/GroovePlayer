package com.aethelsoft.grooveplayer.data.mapper

import com.aethelsoft.grooveplayer.data.local.db.entity.PlaylistTrackEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.PlaylistWithTracksRelation
import com.aethelsoft.grooveplayer.domain.model.Album
import com.aethelsoft.grooveplayer.domain.model.Playlist
import com.aethelsoft.grooveplayer.domain.model.PlaylistTrack
import com.aethelsoft.grooveplayer.domain.model.PlaylistWithTracks
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.makeAlbumId

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
                    song = toSong(entity),
                )
            },
        )
    }

    fun toSong(entity: PlaylistTrackEntity): Song {
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
            uri = entity.uri,
            genre = entity.genre,
            durationMs = entity.durationMs,
            artworkUrl = entity.artworkUrl,
            album = album,
            filePath = entity.filePath,
        )
    }

    fun toEntity(playlistId: Long, position: Int, song: Song): PlaylistTrackEntity =
        PlaylistTrackEntity(
            playlistId = playlistId,
            position = position,
            songId = song.id,
            title = song.title,
            artist = song.artist,
            uri = song.uri,
            filePath = song.filePath?.takeIf { it.isNotBlank() },
            durationMs = song.durationMs,
            artworkUrl = song.artworkUrl,
            albumName = song.album?.name,
            genre = song.genre,
        )
}

package com.aethelsoft.grooveplayer.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val trackCount: Int,
    val artworkUrls: List<String> = emptyList(),
)

data class PlaylistTrack(
    val entryId: Long,
    val position: Int,
    val song: Song,
)

data class PlaylistWithTracks(
    val playlist: Playlist,
    val tracks: List<PlaylistTrack>,
)

fun playlistCountLabel(count: Int): String =
    if (count == 1) "1 playlist" else "$count playlists"

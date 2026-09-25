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
    /** SCRUM-84 songs.contentHash. Empty only for rows that could not be keyed. */
    val contentHash: String = "",
    /** False when the private-library file for [contentHash] is missing. */
    val available: Boolean = true,
)

data class PlaylistWithTracks(
    val playlist: Playlist,
    val tracks: List<PlaylistTrack>,
)

fun playlistCountLabel(count: Int): String =
    if (count == 1) "1 playlist" else "$count playlists"

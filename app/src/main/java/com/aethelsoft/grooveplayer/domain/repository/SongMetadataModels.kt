package com.aethelsoft.grooveplayer.domain.repository

/**
 * Lightweight metadata used for editing and searching, separate from full Song/Album/Artist models.
 */
data class SongMetadata(
    val songId: String,
    val title: String,
    val genres: List<String>,
    val artists: List<String>,
    val album: String?,
    val year: Int?,
    val useAlbumYear: Boolean
)

data class AlbumMetadata(
    val name: String,
    val artist: String,
    val artworkUrl: String?,
    val year: Int?
)

data class ArtistMetadata(
    val name: String,
    val imageUrl: String?
)


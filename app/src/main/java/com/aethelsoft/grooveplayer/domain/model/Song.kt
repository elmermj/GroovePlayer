package com.aethelsoft.grooveplayer.domain.model

data class Song (
    override val id: String,
    val title: String,
    val artist: String,
    val uri: String,
    val genre: String,
    val durationMs: Long,
    val artworkUrl: String? = null,
    val album: Album? = null,
    val useAlbumYear: Boolean = false,
    val genres: List<Genre> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val year: Int? = null
) : Library
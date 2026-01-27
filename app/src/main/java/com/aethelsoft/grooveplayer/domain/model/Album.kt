package com.aethelsoft.grooveplayer.domain.model

/**
 * Simple domain model for albums used by UI lists.
 */
data class Album(
    override val id: String,
    val name: String,
    val artist: String,
    val artworkUrl: String?,
    val songs: List<Song>,
    val year: Int? = null
) : Library
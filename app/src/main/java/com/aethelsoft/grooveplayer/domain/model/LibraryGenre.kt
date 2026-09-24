package com.aethelsoft.grooveplayer.domain.model

/**
 * A genre present on at least one local library track.
 */
data class LibraryGenre(
    val name: String,
    val trackCount: Int,
    val artworkUrl: String? = null,
)

fun trackCountLabel(trackCount: Int): String =
    if (trackCount == 1) "1 track" else "$trackCount tracks"

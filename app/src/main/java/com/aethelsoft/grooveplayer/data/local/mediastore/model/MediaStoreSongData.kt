package com.aethelsoft.grooveplayer.data.local.mediastore.model

/**
 * Data layer model representing a song from MediaStore.
 * This is separate from the domain Song model to maintain layer separation.
 */
internal data class MediaStoreSongData(
    val id: String,
    val title: String,
    val artist: String,
    val uri: String,
    val genre: String,
    val durationMs: Long,
    val artworkUrl: String? = null,
    val album: String? = null
)

package com.aethelsoft.grooveplayer.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val uri: String,
    val genre: String,
    val durationMs: Long,
    val artworkUrl: String? = null,
    val album: String? = null
)
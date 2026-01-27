package com.aethelsoft.grooveplayer.domain.model

/**
 * Simple domain model for artists used by UI lists.
 */
data class Artist(
    override val id: String,
    val name: String,
    val imageUrl: String?,
    val albums: List<Album>,
) : Library
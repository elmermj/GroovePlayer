package com.aethelsoft.grooveplayer.domain.model

/**
 * Represents a song/file offered for sharing.
 * Serialized over the wire for the share protocol.
 */
data class ShareableItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val sizeBytes: Long,
    val mimeType: String,
    /** Original file name, extension included. */
    val fileName: String? = null,
    /** SHA-256 from [com.aethelsoft.grooveplayer.domain.backup.ContentHash]. */
    val checksum: String? = null,
)

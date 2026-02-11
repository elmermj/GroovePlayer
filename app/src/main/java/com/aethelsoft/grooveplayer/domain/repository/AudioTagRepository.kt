package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.domain.model.AudioTags

/**
 * Reads and writes audio file metadata (ID3, MP4 atoms, etc.) for MP3, M4A, FLAC, OGG, WAV.
 */
interface AudioTagRepository {
    /**
     * Reads tags from the file at the given content URI.
     * Returns null if the format is unsupported or read fails.
     */
    suspend fun readTags(contentUri: String): AudioTags?

    /**
     * Writes tags to the file at the given content URI.
     * Uses copy-edit-write flow for content URIs (scoped storage).
     * Returns Result.failure on error.
     */
    suspend fun writeTags(contentUri: String, tags: AudioTags): Result<Unit>
}

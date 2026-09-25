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
     * Picture frames are left alone unless [replaceFrontCover] is true.
     * A format this library cannot write fails without changing the file.
     */
    suspend fun writeTags(
        contentUri: String,
        tags: AudioTags,
        replaceFrontCover: Boolean = false,
    ): Result<Unit>
}

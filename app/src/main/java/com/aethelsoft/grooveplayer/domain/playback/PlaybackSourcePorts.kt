package com.aethelsoft.grooveplayer.domain.playback

import com.aethelsoft.grooveplayer.domain.model.Song

data class CloudAudioHit(
    val presence: CloudAudioPresence,
    val downloadUrl: String? = null,
    val streamUrl: String? = null,
)

interface LocalAudioAvailability {
    /** Original library URI or on-disk path is readable. Does not include the cloud cache. */
    fun isReadable(uri: String, filePath: String?): Boolean
}

interface CloudPlaybackCache {
    fun existingUri(songId: String): String?
    suspend fun storeFromUrl(songId: String, url: String): String?
    fun delete(songId: String)
}

interface SongCatalog {
    suspend fun contains(songId: String): Boolean
    suspend fun sourcePath(songId: String): String?
    suspend fun purge(songIds: List<String>)
}

interface CloudAudioLookup {
    /**
     * Confirm whether cloud storage has this song's audio.
     * [logicalPath] is the on-device path captured when the song was indexed.
     */
    suspend fun lookup(song: Song, logicalPath: String?): CloudAudioHit
}

interface CloudStreamEntitlement {
    /** True only for [com.aethelsoft.grooveplayer.domain.model.PrivilegeTier.PREMIUM]. */
    suspend fun canStreamFromCloud(): Boolean
}

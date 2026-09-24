package com.aethelsoft.grooveplayer.domain.playback

import com.aethelsoft.grooveplayer.domain.model.Song

data class CloudAudioHit(
    val presence: CloudAudioPresence,
    /**
     * Server `entitled` when [presence] is [CloudAudioPresence.PRESENT].
     * False for free, basic, and premium with an open grace window.
     * Never a purge signal.
     */
    val entitled: Boolean = false,
    val contentHash: String? = null,
    val logicalPath: String? = null,
    val sizeBytes: Long? = null,
)

/** Identity saved at existence check and sent to stream-url when the track opens. */
data class PlaybackStreamTicket(
    val songId: String,
    val contentHash: String? = null,
    val logicalPath: String? = null,
    val sizeBytes: Long? = null,
)

sealed class CloudStreamOpen {
    data class Play(val url: String) : CloudStreamOpen()
    /** Object exists; caller may not stream (403, or entitled false). Catalog stays. */
    data object NotEntitled : CloudStreamOpen()
    /** 404 exists:false. Catalog may be purged. */
    data object Absent : CloudStreamOpen()
    /** dry_run or an unusable URL. Not missing. */
    data object Unusable : CloudStreamOpen()
    /** Network, 401, 5xx. Not missing. */
    data object Unknown : CloudStreamOpen()
}

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
     * GET `/v1/playback/objects`. Does not mint a stream URL.
     * [logicalPath] is the on-device path captured when the song was indexed.
     */
    suspend fun lookup(song: Song, logicalPath: String?): CloudAudioHit

    /**
     * POST `/v1/playback/stream-url` for one track that is about to play.
     * Call again when the previous URL expires. Expiry is not absence.
     */
    suspend fun openStream(ticket: PlaybackStreamTicket): CloudStreamOpen
}

interface PlaybackStreamTickets {
    fun put(ticket: PlaybackStreamTicket)
    fun get(songId: String): PlaybackStreamTicket?
}

interface CloudStreamEntitlement {
    /**
     * Active premium: [com.aethelsoft.grooveplayer.domain.model.PrivilegeTier.PREMIUM]
     * and the grace window is closed. Open grace is not entitled to stream,
     * even while the stored tier is still premium.
     * False does not mean the object is missing and must not purge the catalog.
     * Playback still prefers the server `entitled` flag from `/v1/playback/objects`.
     */
    suspend fun canStreamFromCloud(): Boolean
}

const val PLAYBACK_STREAM_SCHEME = "groove-playback"

fun playbackStreamUri(songId: String): String = "$PLAYBACK_STREAM_SCHEME://$songId"

fun playbackStreamSongId(uri: String): String? {
    val prefix = "$PLAYBACK_STREAM_SCHEME://"
    if (!uri.startsWith(prefix)) return null
    val id = uri.removePrefix(prefix)
    return id.takeIf { it.isNotBlank() }
}

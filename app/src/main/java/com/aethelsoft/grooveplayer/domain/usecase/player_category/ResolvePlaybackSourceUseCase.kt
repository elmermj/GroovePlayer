package com.aethelsoft.grooveplayer.domain.usecase.player_category

import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.playback.CloudAudioHit
import com.aethelsoft.grooveplayer.domain.playback.CloudAudioPresence
import com.aethelsoft.grooveplayer.domain.playback.CloudAudioLookup
import com.aethelsoft.grooveplayer.domain.playback.CloudPlaybackCache
import com.aethelsoft.grooveplayer.domain.playback.CloudStreamEntitlement
import com.aethelsoft.grooveplayer.domain.playback.LocalAudioAvailability
import com.aethelsoft.grooveplayer.domain.playback.PlaybackDecision
import com.aethelsoft.grooveplayer.domain.playback.PlaybackDropReason
import com.aethelsoft.grooveplayer.domain.playback.SongCatalog
import com.aethelsoft.grooveplayer.domain.playback.adjustedQueueStartIndex
import com.aethelsoft.grooveplayer.domain.playback.looksLikeSignedObjectUrl
import com.aethelsoft.grooveplayer.domain.playback.playbackDecision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class ResolvedPlayback {
    data class Playable(val song: Song) : ResolvedPlayback()
    data class Dropped(val songId: String, val reason: PlaybackDropReason) : ResolvedPlayback() {
        /** True only when the cloud object is gone and the catalog row was removed. */
        val purged: Boolean get() = reason == PlaybackDropReason.PURGED
    }
}

data class ResolvedQueue(
    val songs: List<Song>,
    val startIndex: Int,
    /**
     * At least one song had cloud audio but the user is not Premium.
     * Those songs were skipped. Their catalog rows were not purged.
     */
    val premiumStreamBlocked: Boolean = false,
)

/**
 * Single resolve path for play and enqueue, before any player URI is opened.
 *
 * Local file, Premium whole-object cloud playback, or purge when the object is absent.
 * A non-Premium user who only has a cloud copy is skipped (upgrade), never deleted.
 */
@Singleton
class ResolvePlaybackSourceUseCase @Inject constructor(
    private val localAudio: LocalAudioAvailability,
    private val cache: CloudPlaybackCache,
    private val catalog: SongCatalog,
    private val cloud: CloudAudioLookup,
    private val entitlement: CloudStreamEntitlement,
) {
    suspend fun resolveOne(song: Song): ResolvedPlayback = withContext(Dispatchers.IO) {
        resolveOneLocked(song)
    }

    suspend fun resolveQueue(songs: List<Song>, startIndex: Int): ResolvedQueue =
        withContext(Dispatchers.IO) {
            if (songs.isEmpty()) return@withContext ResolvedQueue(emptyList(), 0)
            val playable = ArrayList<Song>(songs.size)
            val playableIds = ArrayList<String>(songs.size)
            val purgeIds = ArrayList<String>()
            var premiumStreamBlocked = false
            for (song in songs) {
                when (val resolved = resolveOneLocked(song, purgeNow = false)) {
                    is ResolvedPlayback.Playable -> {
                        playable += resolved.song
                        playableIds += song.id
                    }
                    is ResolvedPlayback.Dropped -> {
                        if (resolved.reason == PlaybackDropReason.PURGED) purgeIds += song.id
                        if (resolved.reason == PlaybackDropReason.NOT_ENTITLED) premiumStreamBlocked = true
                    }
                }
            }
            if (purgeIds.isNotEmpty()) {
                purgeQuietly(purgeIds)
            }
            ResolvedQueue(
                songs = playable,
                startIndex = adjustedQueueStartIndex(
                    originalIds = songs.map { it.id },
                    startIndex = startIndex,
                    playableIds = playableIds,
                ),
                premiumStreamBlocked = premiumStreamBlocked,
            )
        }

    private suspend fun resolveOneLocked(
        song: Song,
        purgeNow: Boolean = true,
    ): ResolvedPlayback {
        val logicalPath = song.filePath ?: catalog.sourcePath(song.id)
        val localUri = localPlayableUri(song, logicalPath)
        if (localUri != null) {
            return ResolvedPlayback.Playable(
                if (localUri == song.uri) song else song.copy(uri = localUri),
            )
        }
        val inCatalog = catalog.contains(song.id)
        if (!inCatalog) {
            return ResolvedPlayback.Dropped(song.id, PlaybackDropReason.UNAVAILABLE)
        }
        val cloudHit = cloud.lookup(song, logicalPath)
        // Entitlement is read only after the object is known to exist or not.
        // It never decides purge.
        val canStream = entitlement.canStreamFromCloud()
        val decision = playbackDecision(
            localAvailable = false,
            inCatalog = true,
            cloud = cloudHit.presence,
            canStreamCloud = canStream,
        )
        return when (decision) {
            PlaybackDecision.PLAY_LOCAL -> ResolvedPlayback.Playable(song)
            PlaybackDecision.STREAM_CLOUD -> streamOrSkip(song, cloudHit)
            PlaybackDecision.PURGE -> {
                cache.delete(song.id)
                if (purgeNow) purgeQuietly(listOf(song.id))
                ResolvedPlayback.Dropped(song.id, PlaybackDropReason.PURGED)
            }
            PlaybackDecision.SKIP -> ResolvedPlayback.Dropped(
                song.id,
                if (cloudHit.presence == CloudAudioPresence.PRESENT && !canStream) {
                    PlaybackDropReason.NOT_ENTITLED
                } else {
                    PlaybackDropReason.UNAVAILABLE
                },
            )
        }
    }

    private fun localPlayableUri(song: Song, logicalPath: String?): String? {
        if (!localAudio.isReadable(song.uri, logicalPath)) {
            return cache.existingUri(song.id)
        }
        // Library content URI when it opens. If only the indexed path still exists, play that file.
        if (localAudio.isReadable(song.uri, filePath = null) || logicalPath.isNullOrBlank()) {
            return song.uri
        }
        return File(logicalPath).toURI().toString()
    }

    private suspend fun streamOrSkip(song: Song, hit: CloudAudioHit?): ResolvedPlayback {
        val playUri = materialize(song.id, hit)
            ?: return ResolvedPlayback.Dropped(song.id, PlaybackDropReason.UNAVAILABLE)
        return ResolvedPlayback.Playable(song.copy(uri = playUri))
    }

    /**
     * Signed object URLs are downloaded once (no Range) and played from disk.
     * A non-R2 stream URL is the only URL ExoPlayer may open directly.
     */
    private suspend fun materialize(songId: String, hit: CloudAudioHit?): String? {
        if (hit == null) return null
        val download = hit.downloadUrl?.takeIf { it.isNotBlank() }
        val stream = hit.streamUrl?.takeIf { it.isNotBlank() }
        if (download != null) {
            return cache.storeFromUrl(songId, download)
        }
        if (stream != null && looksLikeSignedObjectUrl(stream)) {
            return cache.storeFromUrl(songId, stream)
        }
        return stream
    }

    private suspend fun purgeQuietly(songIds: List<String>) {
        try {
            catalog.purge(songIds)
        } catch (_: Exception) {
            // The song is still dropped from this queue. A later play retries the purge.
        }
    }
}

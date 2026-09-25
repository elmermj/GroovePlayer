package com.aethelsoft.grooveplayer.domain.usecase.player_category

import com.aethelsoft.grooveplayer.domain.backup.AppLibraryPaths
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.playback.CloudAudioLookup
import com.aethelsoft.grooveplayer.domain.playback.CloudAudioPresence
import com.aethelsoft.grooveplayer.domain.playback.CloudPlaybackCache
import com.aethelsoft.grooveplayer.domain.playback.LocalAudioAvailability
import com.aethelsoft.grooveplayer.domain.playback.PlaybackDecision
import com.aethelsoft.grooveplayer.domain.playback.PlaybackDropReason
import com.aethelsoft.grooveplayer.domain.playback.PlaybackStreamTicket
import com.aethelsoft.grooveplayer.domain.playback.PlaybackStreamTickets
import com.aethelsoft.grooveplayer.domain.playback.SongCatalog
import com.aethelsoft.grooveplayer.domain.playback.adjustedQueueStartIndex
import com.aethelsoft.grooveplayer.domain.playback.playbackDecision
import com.aethelsoft.grooveplayer.domain.playback.playbackStreamUri
import kotlinx.coroutines.CancellationException
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
     * At least one song had cloud audio but the user is not entitled to stream.
     * Those songs were skipped. Their catalog rows were not purged.
     */
    val premiumStreamBlocked: Boolean = false,
)

/**
 * Single resolve path for play and enqueue, before any player URI is opened.
 *
 * Local file, or a `groove-playback://` ticket when the object exists and the
 * server says `entitled`. The signed `stream_url` is requested when ExoPlayer
 * opens that item, not for the rest of the queue.
 *
 * Purge only on 404 `{exists:false}`. `entitled:false` (free, basic, open grace)
 * skips and keeps the row.
 */
@Singleton
class ResolvePlaybackSourceUseCase @Inject constructor(
    private val localAudio: LocalAudioAvailability,
    private val cache: CloudPlaybackCache,
    private val catalog: SongCatalog,
    private val cloud: CloudAudioLookup,
    private val tickets: PlaybackStreamTickets,
    private val appLibraryPaths: AppLibraryPaths,
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
        return try {
            resolveOneOrDrop(song, purgeNow)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A closed Room pool, a missing sourcePath column, or a lookup failure must
            // not crash cold start. A file that still opens locally keeps playing.
            localFallback(song) ?: ResolvedPlayback.Dropped(song.id, PlaybackDropReason.UNAVAILABLE)
        }
    }

    private suspend fun resolveOneOrDrop(
        song: Song,
        purgeNow: Boolean,
    ): ResolvedPlayback {
        val logicalPath = logicalPathFor(song)
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
        // Server `entitled` is the stream gate. It does not decide purge.
        val decision = playbackDecision(
            localAvailable = false,
            inCatalog = true,
            cloud = cloudHit.presence,
            canStreamCloud = cloudHit.entitled,
        )
        return when (decision) {
            PlaybackDecision.PLAY_LOCAL -> ResolvedPlayback.Playable(song)
            PlaybackDecision.STREAM_CLOUD -> {
                tickets.put(
                    PlaybackStreamTicket(
                        songId = song.id,
                        contentHash = cloudHit.contentHash,
                        logicalPath = cloudHit.logicalPath ?: logicalPath,
                        sizeBytes = cloudHit.sizeBytes ?: song.fileSizeBytes,
                    ),
                )
                ResolvedPlayback.Playable(song.copy(uri = playbackStreamUri(song.id)))
            }
            PlaybackDecision.PURGE -> {
                cache.delete(song.id)
                if (purgeNow) purgeQuietly(listOf(song.id))
                ResolvedPlayback.Dropped(song.id, PlaybackDropReason.PURGED)
            }
            PlaybackDecision.SKIP -> ResolvedPlayback.Dropped(
                song.id,
                if (cloudHit.presence == CloudAudioPresence.PRESENT && !cloudHit.entitled) {
                    PlaybackDropReason.NOT_ENTITLED
                } else {
                    PlaybackDropReason.UNAVAILABLE
                },
            )
        }
    }

    private suspend fun logicalPathFor(song: Song): String? {
        val stored = try {
            catalog.sourcePath(song.id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (!stored.isNullOrBlank() && appLibraryPaths.isInside(stored)) {
            val file = File(stored)
            if (file.isFile && file.length() > 0L) return stored
        }
        return song.filePath ?: stored
    }

    private fun localFallback(song: Song): ResolvedPlayback.Playable? {
        val localUri = runCatching { localPlayableUri(song, song.filePath) }.getOrNull() ?: return null
        return ResolvedPlayback.Playable(
            if (localUri == song.uri) song else song.copy(uri = localUri),
        )
    }

    private fun localPlayableUri(song: Song, logicalPath: String?): String? {
        if (!localAudio.isReadable(song.uri, logicalPath)) {
            return cache.existingUri(song.id)
        }
        if (!logicalPath.isNullOrBlank() && appLibraryPaths.isInside(logicalPath)) {
            val file = File(logicalPath)
            if (file.isFile && file.length() > 0L) return file.toURI().toString()
        }
        // Library content URI when it opens. If only the indexed path still exists, play that file.
        if (localAudio.isReadable(song.uri, filePath = null) || logicalPath.isNullOrBlank()) {
            return song.uri
        }
        return File(logicalPath).toURI().toString()
    }

    private suspend fun purgeQuietly(songIds: List<String>) {
        try {
            catalog.purge(songIds)
        } catch (_: Exception) {
            // The song is still dropped from this queue. A later play retries the purge.
        }
    }
}

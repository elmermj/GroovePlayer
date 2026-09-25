package com.aethelsoft.grooveplayer.data.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import com.aethelsoft.grooveplayer.domain.playback.CloudPlaybackCache
import com.aethelsoft.grooveplayer.domain.playback.CloudStreamOpen
import com.aethelsoft.grooveplayer.domain.playback.PLAYBACK_STREAM_SCHEME
import com.aethelsoft.grooveplayer.domain.playback.PlaybackStreamTickets
import com.aethelsoft.grooveplayer.domain.playback.SongCatalog
import com.aethelsoft.grooveplayer.domain.playback.CloudAudioLookup
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exchanges `groove-playback://{songId}` for POST `/v1/playback/stream-url`
 * when ExoPlayer opens the item. That is "about to play" (TTL 3600), not a
 * URL minted for the whole queue. A later open, including after a mid-play
 * 401/403 on the signed GET, fetches a fresh URL. Expiry is not absence.
 *
 * Range on the signed URL is intentional. This path must not use the backup
 * OkHttp client, which strips Range.
 */
@OptIn(UnstableApi::class)
@Singleton
class PlaybackStreamResolver @Inject constructor(
    private val tickets: PlaybackStreamTickets,
    private val cloud: CloudAudioLookup,
    private val catalog: SongCatalog,
    private val cache: CloudPlaybackCache,
    private val signals: PremiumStreamSignals,
) {
    fun resolve(spec: DataSpec): DataSpec {
        return try {
            resolveOrThrow(spec)
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            // ExoPlayer treats IOException as a failed open. A RuntimeException
            // from Retrofit/Room on the playback thread kills the process.
            throw IOException(e.message ?: "playback stream unavailable", e)
        }
    }

    private fun resolveOrThrow(spec: DataSpec): DataSpec {
        if (spec.uri.scheme != PLAYBACK_STREAM_SCHEME) return spec
        val songId = spec.uri.host?.takeIf { it.isNotBlank() }
            ?: throw IOException("playback stream song id missing")
        val ticket = tickets.get(songId)
            ?: throw IOException("playback stream ticket missing")
        when (val opened = runBlocking { cloud.openStream(ticket) }) {
            is CloudStreamOpen.Play -> return spec.withUri(Uri.parse(opened.url))
            CloudStreamOpen.NotEntitled -> {
                signals.notifyRequired()
                throw IOException("premium required")
            }
            CloudStreamOpen.Absent -> {
                runBlocking {
                    cache.delete(songId)
                    try {
                        catalog.purge(listOf(songId))
                    } catch (_: Exception) {
                        // Dropped from this open. A later play retries the purge.
                    }
                }
                throw IOException("object not found")
            }
            CloudStreamOpen.Unusable, CloudStreamOpen.Unknown ->
                throw IOException("playback stream unavailable")
        }
    }
}

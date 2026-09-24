package com.aethelsoft.grooveplayer.data.playback

import android.util.Log
import com.aethelsoft.grooveplayer.data.remote.api.PlaybackApi
import com.aethelsoft.grooveplayer.data.remote.dto.PlaybackObjectDto
import com.aethelsoft.grooveplayer.data.remote.dto.PlaybackStreamRequestDto
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.playback.CloudAudioHit
import com.aethelsoft.grooveplayer.domain.playback.CloudAudioLookup
import com.aethelsoft.grooveplayer.domain.playback.CloudAudioPresence
import com.aethelsoft.grooveplayer.domain.playback.CloudStreamOpen
import com.aethelsoft.grooveplayer.domain.playback.PlaybackStreamTicket
import com.aethelsoft.grooveplayer.domain.playback.interpretPlaybackObjects
import com.aethelsoft.grooveplayer.domain.playback.interpretStreamUrl
import com.squareup.moshi.Moshi
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GET `/v1/playback/objects`, then POST `/v1/playback/stream-url` only when a
 * track is about to play. Backup object listing and download-url are not used.
 *
 * 404 `{exists:false}` is absence. `entitled:false` and stream-url 403 keep the
 * catalog row. Network, 401, and 5xx are unknown.
 */
@Singleton
class CloudAudioLookupImpl @Inject constructor(
    private val playbackApi: PlaybackApi,
    private val moshi: Moshi,
    private val cloudSongCatalog: CloudSongCatalog,
) : CloudAudioLookup {

    private val bodyAdapter = moshi.adapter(PlaybackObjectDto::class.java)

    override suspend fun lookup(song: Song, logicalPath: String?): CloudAudioHit {
        val path = logicalPath?.takeIf { it.isNotBlank() }
        val size = song.fileSizeBytes?.takeIf { it > 0L }
        // No sha256 on the device catalog. Path + size is the supported fallback.
        if (path == null || size == null) {
            return CloudAudioHit(CloudAudioPresence.UNKNOWN)
        }
        return try {
            val response = playbackApi.getObject(logicalPath = path, sizeBytes = size)
            val dto = readBody(response)
            val (presence, entitled) = interpretPlaybackObjects(
                httpCode = response.code(),
                exists = dto?.exists,
                entitled = dto?.entitled,
            )
            if (presence == CloudAudioPresence.PRESENT) {
                cloudSongCatalog.confirm(song.id)
            }
            CloudAudioHit(
                presence = presence,
                entitled = entitled,
                contentHash = dto?.contentHash,
                logicalPath = dto?.logicalPath ?: path,
                sizeBytes = dto?.sizeBytes ?: size,
            )
        } catch (e: IOException) {
            Log.w(TAG, "playback objects unreachable for ${song.id}: ${e.message}")
            CloudAudioHit(CloudAudioPresence.UNKNOWN)
        }
    }

    override suspend fun openStream(ticket: PlaybackStreamTicket): CloudStreamOpen {
        val hash = ticket.contentHash?.takeIf { it.isNotBlank() }
        val path = ticket.logicalPath?.takeIf { it.isNotBlank() }
        val size = ticket.sizeBytes?.takeIf { it > 0L }
        if (hash == null && (path == null || size == null)) {
            return CloudStreamOpen.Unknown
        }
        return try {
            val response = playbackApi.streamUrl(
                PlaybackStreamRequestDto(
                    contentHash = hash,
                    logicalPath = path,
                    sizeBytes = size,
                    contentType = audioContentType(path),
                ),
            )
            val dto = readBody(response)
            interpretStreamUrl(
                httpCode = response.code(),
                exists = dto?.exists,
                entitled = dto?.entitled == true,
                dryRun = dto?.dryRun == true,
                streamUrl = dto?.streamUrl,
            )
        } catch (e: IOException) {
            Log.w(TAG, "stream-url unreachable for ${ticket.songId}: ${e.message}")
            CloudStreamOpen.Unknown
        }
    }

    private fun readBody(response: Response<PlaybackObjectDto>): PlaybackObjectDto? {
        response.body()?.let { return it }
        val raw = response.errorBody()?.use { it.string() } ?: return null
        return runCatching { bodyAdapter.fromJson(raw) }.getOrNull()
    }

    private fun audioContentType(path: String?): String? {
        val ext = path?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.isNotBlank() && it.length <= 5 }
            ?: return null
        return when (ext) {
            "mp3" -> "audio/mpeg"
            "m4a", "mp4" -> "audio/mp4"
            "flac" -> "audio/flac"
            "ogg", "opus" -> "audio/ogg"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            else -> null
        }
    }

    private companion object {
        const val TAG = "CloudAudioLookup"
    }
}

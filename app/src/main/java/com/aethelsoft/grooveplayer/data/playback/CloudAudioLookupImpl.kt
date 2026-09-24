package com.aethelsoft.grooveplayer.data.playback

import android.util.Log
import com.aethelsoft.grooveplayer.data.remote.api.BackupApi
import com.aethelsoft.grooveplayer.data.remote.api.PlaybackSourceApi
import com.aethelsoft.grooveplayer.data.remote.dto.BackupDownloadUrlRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.PlaybackSourceRequestDto
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.playback.CloudAudioHit
import com.aethelsoft.grooveplayer.domain.playback.CloudAudioLookup
import com.aethelsoft.grooveplayer.domain.playback.CloudAudioPresence
import com.aethelsoft.grooveplayer.domain.playback.cloudObjectMatchesSong
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prefers POST /v1/playback/source (Benny). Until that route exists, 404/501
 * falls back to the backup object list plus POST /v1/backup/download-url.
 *
 * 200 `{available:false}` means the audio is really gone. Network and auth
 * failures stay [CloudAudioPresence.UNKNOWN] so the catalog is not purged.
 * Dry-run URLs are not playable audio.
 */
@Singleton
class CloudAudioLookupImpl @Inject constructor(
    private val playbackSourceApi: PlaybackSourceApi,
    private val backupApi: BackupApi,
    private val cloudSongCatalog: CloudSongCatalog,
) : CloudAudioLookup {

    @Volatile
    private var playbackRouteMissing = false

    override suspend fun lookup(song: Song, logicalPath: String?): CloudAudioHit {
        if (!playbackRouteMissing) {
            try {
                val response = playbackSourceApi.resolve(
                    PlaybackSourceRequestDto(
                        songId = song.id,
                        logicalPath = logicalPath,
                        title = song.title,
                        artist = song.artist,
                        album = song.album?.name,
                        sizeBytes = song.fileSizeBytes,
                    )
                )
                return mapRoute(song.id, response.available, response.dryRun, response.downloadUrl, response.streamUrl)
            } catch (e: HttpException) {
                if (e.code() == 404 || e.code() == 501) {
                    playbackRouteMissing = true
                    Log.i(TAG, "POST /v1/playback/source unavailable (${e.code()}); using backup objects")
                } else {
                    Log.w(TAG, "playback source HTTP ${e.code()} for ${song.id}")
                    return CloudAudioHit(CloudAudioPresence.UNKNOWN)
                }
            } catch (e: IOException) {
                Log.w(TAG, "playback source unreachable for ${song.id}: ${e.message}")
                return CloudAudioHit(CloudAudioPresence.UNKNOWN)
            }
        }
        return fallback(song, logicalPath)
    }

    private fun mapRoute(
        songId: String,
        available: Boolean,
        dryRun: Boolean,
        downloadUrl: String?,
        streamUrl: String?,
    ): CloudAudioHit {
        if (!available || dryRun) return CloudAudioHit(CloudAudioPresence.ABSENT)
        val download = downloadUrl?.takeIf { it.isNotBlank() }
        val stream = streamUrl?.takeIf { it.isNotBlank() }
        if (download == null && stream == null) {
            return CloudAudioHit(CloudAudioPresence.UNKNOWN)
        }
        cloudSongCatalog.confirm(songId)
        return CloudAudioHit(
            presence = CloudAudioPresence.PRESENT,
            downloadUrl = download,
            streamUrl = stream,
        )
    }

    private suspend fun fallback(song: Song, logicalPath: String?): CloudAudioHit {
        val objects = try {
            cloudSongCatalog.load()
        } catch (e: IOException) {
            Log.w(TAG, "backup objects unreachable: ${e.message}")
            return CloudAudioHit(CloudAudioPresence.UNKNOWN)
        } ?: return CloudAudioHit(CloudAudioPresence.UNKNOWN)

        if (logicalPath.isNullOrBlank()) {
            // Cannot prove the object is missing without a path or the playback route.
            return CloudAudioHit(CloudAudioPresence.UNKNOWN)
        }
        val match = objects.firstOrNull { obj ->
            cloudObjectMatchesSong(logicalPath, song.fileSizeBytes, obj.logicalPath, obj.sizeBytes)
        } ?: return CloudAudioHit(CloudAudioPresence.ABSENT)

        return try {
            val url = backupApi.requestDownloadUrl(
                BackupDownloadUrlRequestDto(
                    contentHash = match.contentHash,
                    r2Key = match.r2Key,
                )
            )
            if (url.dryRun || url.downloadUrl.isNullOrBlank()) {
                CloudAudioHit(CloudAudioPresence.ABSENT)
            } else {
                cloudSongCatalog.confirm(song.id)
                CloudAudioHit(
                    presence = CloudAudioPresence.PRESENT,
                    downloadUrl = url.downloadUrl,
                )
            }
        } catch (e: HttpException) {
            Log.w(TAG, "download-url HTTP ${e.code()} for ${song.id}")
            CloudAudioHit(CloudAudioPresence.UNKNOWN)
        } catch (e: IOException) {
            Log.w(TAG, "download-url unreachable for ${song.id}: ${e.message}")
            CloudAudioHit(CloudAudioPresence.UNKNOWN)
        }
    }

    private companion object {
        const val TAG = "CloudAudioLookup"
    }
}

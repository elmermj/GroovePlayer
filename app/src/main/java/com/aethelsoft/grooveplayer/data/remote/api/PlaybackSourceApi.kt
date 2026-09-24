package com.aethelsoft.grooveplayer.data.remote.api

import com.aethelsoft.grooveplayer.data.remote.dto.PlaybackSourceRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.PlaybackSourceResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Benny: existence + whole-object URL for one catalog song.
 *
 * Contract (not on the server yet — 404/501 falls back to backup objects):
 * POST /v1/playback/source
 * 200 `{ "available": false }` when the object is gone (client may purge).
 * 200 `{ "available": true, "download_url": "..." }` for a single whole-object GET.
 * Optional `stream_url` only if it is NOT a signed R2 URL (no Range GET).
 * `dry_run: true` means the bytes are not in R2.
 */
interface PlaybackSourceApi {
    @POST("/v1/playback/source")
    suspend fun resolve(@Body body: PlaybackSourceRequestDto): PlaybackSourceResponseDto
}

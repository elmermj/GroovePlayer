package com.aethelsoft.grooveplayer.data.remote.api

import com.aethelsoft.grooveplayer.data.remote.dto.PlaybackObjectDto
import com.aethelsoft.grooveplayer.data.remote.dto.PlaybackStreamRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Playback stream API on `test` (SCRUM-62). There is no `/v1/playback/source`
 * and playback does not use the backup object list or download-url.
 *
 * GET `/v1/playback/objects` is the existence check (HEAD is the same lookup
 * without a body). The body is required here: `entitled` and `content_hash`
 * decide stream vs upgrade vs purge. 404 `{exists:false}` is the only purge signal.
 *
 * POST `/v1/playback/stream-url` is called when that track is about to play
 * (TTL 3600). An expired URL is fetched again; it is not "missing".
 */
interface PlaybackApi {
    @GET("/v1/playback/objects")
    suspend fun getObject(
        @Query("content_hash") contentHash: String? = null,
        @Query("logical_path") logicalPath: String? = null,
        @Query("size_bytes") sizeBytes: Long? = null,
    ): Response<PlaybackObjectDto>

    @POST("/v1/playback/stream-url")
    suspend fun streamUrl(@Body body: PlaybackStreamRequestDto): Response<PlaybackObjectDto>
}

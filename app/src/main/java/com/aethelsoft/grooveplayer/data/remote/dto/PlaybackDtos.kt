package com.aethelsoft.grooveplayer.data.remote.dto

import com.squareup.moshi.Json

/** GET `/v1/playback/objects` and POST `/v1/playback/stream-url`. */
data class PlaybackObjectDto(
    @param:Json(name = "exists") val exists: Boolean = false,
    @param:Json(name = "entitled") val entitled: Boolean = false,
    @param:Json(name = "match") val match: String? = null,
    @param:Json(name = "object_id") val objectId: String? = null,
    @param:Json(name = "content_hash") val contentHash: String? = null,
    @param:Json(name = "size_bytes") val sizeBytes: Long? = null,
    @param:Json(name = "r2_key") val r2Key: String? = null,
    @param:Json(name = "logical_path") val logicalPath: String? = null,
    @param:Json(name = "kind") val kind: String? = null,
    @param:Json(name = "error") val error: String? = null,
    @param:Json(name = "stream_url") val streamUrl: String? = null,
    @param:Json(name = "expires_in_sec") val expiresInSec: Int? = null,
    @param:Json(name = "content_type") val contentType: String? = null,
    @param:Json(name = "dry_run") val dryRun: Boolean = false,
    @param:Json(name = "method") val method: String? = null,
)

/** POST `/v1/playback/stream-url`. Hash wins when present; otherwise path + size. */
data class PlaybackStreamRequestDto(
    @param:Json(name = "content_hash") val contentHash: String? = null,
    @param:Json(name = "logical_path") val logicalPath: String? = null,
    @param:Json(name = "size_bytes") val sizeBytes: Long? = null,
    @param:Json(name = "content_type") val contentType: String? = null,
)

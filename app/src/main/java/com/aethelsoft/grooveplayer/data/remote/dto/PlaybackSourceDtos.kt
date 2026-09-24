package com.aethelsoft.grooveplayer.data.remote.dto

import com.squareup.moshi.Json

/** POST /v1/playback/source — see [com.aethelsoft.grooveplayer.data.remote.api.PlaybackSourceApi]. */
data class PlaybackSourceRequestDto(
    @param:Json(name = "song_id") val songId: String,
    @param:Json(name = "logical_path") val logicalPath: String? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "artist") val artist: String? = null,
    @param:Json(name = "album") val album: String? = null,
    @param:Json(name = "size_bytes") val sizeBytes: Long? = null,
)

data class PlaybackSourceResponseDto(
    @param:Json(name = "available") val available: Boolean = false,
    @param:Json(name = "download_url") val downloadUrl: String? = null,
    @param:Json(name = "stream_url") val streamUrl: String? = null,
    @param:Json(name = "content_hash") val contentHash: String? = null,
    @param:Json(name = "r2_key") val r2Key: String? = null,
    @param:Json(name = "object_id") val objectId: String? = null,
    @param:Json(name = "dry_run") val dryRun: Boolean = false,
    @param:Json(name = "expires_in_sec") val expiresInSec: Int? = null,
)

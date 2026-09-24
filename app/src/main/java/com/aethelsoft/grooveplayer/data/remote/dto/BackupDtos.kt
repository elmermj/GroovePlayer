package com.aethelsoft.grooveplayer.data.remote.dto

import com.squareup.moshi.Json

/** POST /v1/backup/upload-url — kind: song (default) | room_db */
data class BackupUploadUrlRequestDto(
    @param:Json(name = "content_hash") val contentHash: String,
    @param:Json(name = "size_bytes") val sizeBytes: Long,
    @param:Json(name = "content_type") val contentType: String? = null,
    @param:Json(name = "logical_path") val logicalPath: String? = null,
    /** song | room_db — omit/blank treated as song by Benny. */
    @param:Json(name = "kind") val kind: String? = null,
    /** Required for kind=room_db (Room @Database version). */
    @param:Json(name = "schema_version") val schemaVersion: Int? = null,
    @param:Json(name = "app_version") val appVersion: String? = null,
)

/**
 * Response for upload-url.
 * Dedupe hit: deduped=true + object_id + r2_key (no upload_url).
 * Else: upload_url + method=PUT + r2_key + expires_in_sec + dry_run.
 */
data class BackupUploadUrlResponseDto(
    @param:Json(name = "deduped") val deduped: Boolean = false,
    @param:Json(name = "upload_url") val uploadUrl: String? = null,
    @param:Json(name = "method") val method: String? = null,
    @param:Json(name = "r2_key") val r2Key: String? = null,
    @param:Json(name = "expires_in_sec") val expiresInSec: Int? = null,
    @param:Json(name = "dry_run") val dryRun: Boolean = false,
    @param:Json(name = "object_id") val objectId: String? = null,
    @param:Json(name = "kind") val kind: String? = null,
    @param:Json(name = "message") val message: String? = null,
)

/** POST /v1/backup/complete */
data class BackupCompleteRequestDto(
    @param:Json(name = "content_hash") val contentHash: String,
    @param:Json(name = "size_bytes") val sizeBytes: Long,
    @param:Json(name = "r2_key") val r2Key: String,
    @param:Json(name = "logical_path") val logicalPath: String? = null,
    @param:Json(name = "kind") val kind: String? = null,
    @param:Json(name = "schema_version") val schemaVersion: Int? = null,
    @param:Json(name = "app_version") val appVersion: String? = null,
)

/**
 * complete → catalogs object, bumps used_bytes, returns user (/v1/me shape).
 * Benny: dedupe path also always returns [user] (never null on success).
 */
data class BackupCompleteResponseDto(
    @param:Json(name = "deduped") val deduped: Boolean = false,
    @param:Json(name = "replaced") val replaced: Boolean = false,
    @param:Json(name = "object_id") val objectId: String? = null,
    @param:Json(name = "kind") val kind: String? = null,
    /** Always present on success after backend dedupe fix; keep nullable for older servers. */
    @param:Json(name = "user") val user: PublicUserDto? = null,
)

/** POST /v1/backup/download-url — allowed in grace (reads OK). */
data class BackupDownloadUrlRequestDto(
    @param:Json(name = "content_hash") val contentHash: String? = null,
    @param:Json(name = "r2_key") val r2Key: String? = null,
)

data class BackupDownloadUrlResponseDto(
    @param:Json(name = "download_url") val downloadUrl: String? = null,
    @param:Json(name = "method") val method: String? = null,
    @param:Json(name = "r2_key") val r2Key: String? = null,
    @param:Json(name = "expires_in_sec") val expiresInSec: Int? = null,
    @param:Json(name = "dry_run") val dryRun: Boolean = false,
)


/** POST /v1/backup/match — client pre-check (basename(logical_path)+size_bytes; optional hash). */
data class BackupMatchRequestDto(
    @param:Json(name = "logical_path") val logicalPath: String? = null,
    @param:Json(name = "size_bytes") val sizeBytes: Long,
    @param:Json(name = "content_hash") val contentHash: String? = null,
)

data class BackupMatchResponseDto(
    @param:Json(name = "matched") val matched: Boolean = false,
    @param:Json(name = "skip_reason") val skipReason: String? = null,
    @param:Json(name = "object_id") val objectId: String? = null,
    @param:Json(name = "r2_key") val r2Key: String? = null,
    @param:Json(name = "size_bytes") val sizeBytes: Long = 0L,
    @param:Json(name = "logical_path") val logicalPath: String? = null,
    @param:Json(name = "content_hash") val contentHash: String? = null,
)

/** GET /v1/backup/objects */
data class BackupObjectsResponseDto(
    @param:Json(name = "objects") val objects: List<BackupObjectDto> = emptyList(),
)

data class BackupObjectDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "content_hash") val contentHash: String,
    @param:Json(name = "size_bytes") val sizeBytes: Long = 0L,
    @param:Json(name = "r2_key") val r2Key: String,
    @param:Json(name = "logical_path") val logicalPath: String? = null,
    @param:Json(name = "kind") val kind: String? = null,
    @param:Json(name = "schema_version") val schemaVersion: Int? = null,
    @param:Json(name = "app_version") val appVersion: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
)

/** GET /v1/backup/library — Room DB snapshot metadata + download_url. */
data class BackupLibraryResponseDto(
    @param:Json(name = "library") val library: BackupLibraryDto? = null,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "dry_run") val dryRun: Boolean = false,
)

data class BackupLibraryDto(
    @param:Json(name = "object_id") val objectId: String? = null,
    @param:Json(name = "kind") val kind: String? = null,
    @param:Json(name = "content_hash") val contentHash: String? = null,
    @param:Json(name = "size_bytes") val sizeBytes: Long = 0L,
    @param:Json(name = "r2_key") val r2Key: String? = null,
    @param:Json(name = "logical_path") val logicalPath: String? = null,
    @param:Json(name = "schema_version") val schemaVersion: Int? = null,
    @param:Json(name = "app_version") val appVersion: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "download_url") val downloadUrl: String? = null,
    @param:Json(name = "method") val method: String? = null,
    @param:Json(name = "expires_in_sec") val expiresInSec: Int? = null,
    @param:Json(name = "content_type") val contentType: String? = null,
    @param:Json(name = "restore_notes") val restoreNotes: String? = null,
)

/** POST /v1/backup/trim (alias /v1/backups/trim) — cloud catalog + R2 only. */
data class BackupTrimRequestDto(
    @param:Json(name = "strategy") val strategy: String,
    @param:Json(name = "artist") val artist: String? = null,
    @param:Json(name = "album") val album: String? = null,
    @param:Json(name = "year") val year: Int? = null,
    /** Optional cap on bytes deleted this run; 0/null = until under quota. */
    @param:Json(name = "limit_bytes") val limitBytes: Long? = null,
)

data class BackupTrimResponseDto(
    @param:Json(name = "strategy") val strategy: String? = null,
    @param:Json(name = "deleted") val deleted: Int = 0,
    @param:Json(name = "bytes_freed") val bytesFreed: Long = 0L,
    @param:Json(name = "user") val user: PublicUserDto? = null,
)

/** DELETE /v1/backup/objects/{id} — cloud catalog + R2 only; may return refreshed user. */
data class BackupDeleteResponseDto(
    @param:Json(name = "deleted") val deleted: Boolean = true,
    @param:Json(name = "deleted_id") val deletedId: String? = null,
    @param:Json(name = "object_id") val objectId: String? = null,
    @param:Json(name = "bytes_freed") val bytesFreed: Long = 0L,
    @param:Json(name = "kind") val kind: String? = null,
    @param:Json(name = "note") val note: String? = null,
    @param:Json(name = "user") val user: PublicUserDto? = null,
)

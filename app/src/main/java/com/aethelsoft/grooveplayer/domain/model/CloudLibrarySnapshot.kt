package com.aethelsoft.grooveplayer.domain.model

/** Metadata from GET /v1/backup/library when a room_db snapshot exists. */
data class CloudLibrarySnapshot(
    val objectId: String?,
    val contentHash: String?,
    val sizeBytes: Long,
    val r2Key: String?,
    val logicalPath: String?,
    val schemaVersion: Int?,
    val appVersion: String?,
    val createdAtIso: String?,
    val dryRun: Boolean = false,
)

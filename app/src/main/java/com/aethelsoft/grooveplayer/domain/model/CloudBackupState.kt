package com.aethelsoft.grooveplayer.domain.model

/**
 * Client-side cloud backup job state (manual "Back up now").
 */
enum class CloudBackupPhase {
    IDLE,
    PREPARING,
    UPLOADING,
    SUCCESS,
    ERROR,
    BLOCKED_QUOTA,
    BLOCKED_GRACE,
    BLOCKED_NOT_PREMIUM,
}

data class CloudBackupState(
    val phase: CloudBackupPhase = CloudBackupPhase.IDLE,
    val progressPercent: Int = 0,
    val bytesPrepared: Long = 0L,
    val bytesUploaded: Long = 0L,
    val filesTotal: Int = 0,
    val filesCompleted: Int = 0,
    val filesDeduped: Int = 0,
    /** Songs skipped client-side: catalog already has same basename(logical_path) + size_bytes. */
    val filesSkipped: Int = 0,
    val lastBackupAtEpochMs: Long? = null,
    val lastError: String? = null,
    val includedFolders: List<String> = emptyList(),
    val message: String? = null,
    /** True when last upload-url responses were dry_run (R2 keys not live yet). */
    val lastRunDryRun: Boolean = false,
    /**
     * True when complete returned HTTP 422 (R2 HeadObject missing / size mismatch).
     * Local files are untouched — UI should offer Retry (re-PUT + complete / Back up now).
     */
    val canRetry: Boolean = false,
)

/** Backup object kinds (Benny docs/backup-library.md). */
object BackupKinds {
    const val SONG = "song"
    const val ROOM_DB = "room_db"
}

/** One cataloged R2 backup object from GET /v1/backup/objects. */
data class BackupObject(
    val id: String,
    val contentHash: String,
    val sizeBytes: Long,
    val r2Key: String,
    val logicalPath: String? = null,
    val kind: String = BackupKinds.SONG,
    val schemaVersion: Int? = null,
    val appVersion: String? = null,
    val createdAtIso: String? = null,
)

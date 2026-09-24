package com.aethelsoft.grooveplayer.domain.model

/**
 * Client-side cloud backup job state (manual "Back up now").
 */
enum class CloudBackupPhase {
    IDLE,
    PREPARING,
    /** Copying included-folder audio into the app library and updating Room paths. */
    CONSOLIDATING,
    UPLOADING,
    SUCCESS,
    ERROR,
    BLOCKED_QUOTA,
    BLOCKED_GRACE,
    BLOCKED_NOT_PREMIUM,
}

/** Ordered backup stages. Song upload and the catalog start only after consolidate finishes. */
enum class BackupJobStep {
    PREPARING,
    CONSOLIDATING,
    UPLOADING_FILES,
    UPLOADING_CATALOG,
}

/** True while a manual backup is consolidating files or uploading them. */
fun CloudBackupPhase.isUploadInProgress(): Boolean =
    this == CloudBackupPhase.PREPARING ||
        this == CloudBackupPhase.CONSOLIDATING ||
        this == CloudBackupPhase.UPLOADING

data class CloudBackupState(
    val phase: CloudBackupPhase = CloudBackupPhase.IDLE,
    val progressPercent: Int = 0,
    val bytesPrepared: Long = 0L,
    val bytesUploaded: Long = 0L,
    val filesTotal: Int = 0,
    val filesCompleted: Int = 0,
    /** Songs copied into the app library with Room paths updated. */
    val consolidateCompleted: Int = 0,
    val consolidateTotal: Int = 0,
    /** Song files finished (uploaded or hash-skipped). Catalog upload is separate. */
    val uploadCompleted: Int = 0,
    val uploadTotal: Int = 0,
    val jobStep: BackupJobStep = BackupJobStep.PREPARING,
    val filesDeduped: Int = 0,
    /** Songs skipped client-side: cloud already has the same SHA-256 and size. */
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

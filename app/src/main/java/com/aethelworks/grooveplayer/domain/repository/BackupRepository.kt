package com.aethelworks.grooveplayer.domain.repository

import com.aethelworks.grooveplayer.domain.backup.RestorePhase
import com.aethelworks.grooveplayer.domain.model.BackupObject
import com.aethelworks.grooveplayer.domain.model.CloudLibrarySnapshot
import com.aethelworks.grooveplayer.domain.model.CloudBackupState
import com.aethelworks.grooveplayer.domain.model.StorageEntitlement
import com.aethelworks.grooveplayer.domain.model.TrimCloudBackupRequest
import com.aethelworks.grooveplayer.domain.model.TrimCloudBackupResult
import com.aethelworks.grooveplayer.domain.backup.RestoreProgressSnapshot
import kotlinx.coroutines.flow.Flow
import java.io.File

interface BackupRepository {
    fun observeBackupState(): Flow<CloudBackupState>

    /** Latest restore-apply progress. Idle until [stageLibraryRestore] publishes work. */
    fun observeRestoreProgress(): Flow<RestoreProgressSnapshot>

    /** Persisted library-restore phase. [RestorePhase.IDLE] when nothing is in progress. */
    fun restorePhase(): RestorePhase

    /**
     * Manual backup. Gates on Premium + quota + grace + over_quota.
     * Uploads private-library songs, then the Room snapshot.
     */
    suspend fun startBackup(entitlement: StorageEntitlement?, isPremium: Boolean): Result<Unit>

    /**
     * Stop a running backup. Releases the in-flight upload call so the worker
     * can drop the lease, the job gate, and the local snapshot scratch file.
     * No-op when a backup is not in progress. Does not call the network itself.
     */
    fun cancelBackup()

    /**
     * Stop a running restore. Aborts the in-flight download so the worker can
     * delete scratch files and return the restore phase to idle.
     * A swap that has already started is left alone.
     */
    fun cancelRestore()

    /**
     * After cancel, delete half-written library files, drop the staging snapshot,
     * and release the restore job gate. Safe to call more than once.
     * Does not call the network.
     */
    suspend fun abandonCancelledRestore()

    /**
     * GET /v1/backup/lease.
     * Sets [com.aethelworks.grooveplayer.domain.model.CloudBackupState.otherDeviceHoldingLease]
     * when a different install holds a non-expired lease.
     */
    suspend fun refreshBackupLease()

    suspend fun refreshLocalState()

    /** POST /v1/backup/download-url then GET bytes (skipped when dry_run). Allowed in grace. */
    suspend fun downloadObject(
        contentHash: String? = null,
        r2Key: String? = null,
        destFile: File,
    ): Result<Unit>

    /** GET /v1/backup/objects — pass kind=song|room_db; default songs for UI list. */
    suspend fun listRemoteObjects(kind: String? = "song"): Result<List<BackupObject>>

    /**
     * DELETE /v1/backup/objects/{id} — cloud only.
     * room_db delete resets cloud library snapshot; local Room untouched.
     */
    suspend fun deleteRemoteObject(objectId: String): Result<Unit>

    /**
     * POST /v1/backup/trim — cloud songs only (server never selects room_db).
     */
    suspend fun trimCloudBackup(request: TrimCloudBackupRequest): Result<TrimCloudBackupResult>

    /** GET /v1/backup/library — null when no room_db snapshot yet. */
    suspend fun fetchCloudLibraryMetadata(): Result<CloudLibrarySnapshot?>

    /**
     * Download the cloud Room snapshot into a staging file.
     * Does not close Room and does not replace the live database.
     */
    suspend fun stageLibraryRestore(): Result<Unit>

    /**
     * Copy settings, history, and metadata from the staged snapshot into the open database,
     * then clear the staging file.
     */
    suspend fun applyStagedLibraryRestore(): Result<Unit>
}


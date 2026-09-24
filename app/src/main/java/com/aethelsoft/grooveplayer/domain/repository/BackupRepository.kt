package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.domain.backup.RestorePhase
import com.aethelsoft.grooveplayer.domain.model.BackupObject
import com.aethelsoft.grooveplayer.domain.model.CloudLibrarySnapshot
import com.aethelsoft.grooveplayer.domain.model.CloudBackupState
import com.aethelsoft.grooveplayer.domain.model.StorageEntitlement
import com.aethelsoft.grooveplayer.domain.model.TrimCloudBackupRequest
import com.aethelsoft.grooveplayer.domain.model.TrimCloudBackupResult
import kotlinx.coroutines.flow.Flow
import java.io.File

interface BackupRepository {
    fun observeBackupState(): Flow<CloudBackupState>

    /** Persisted library-restore phase. [RestorePhase.IDLE] when nothing is in progress. */
    fun restorePhase(): RestorePhase

    /** Folders that would be included in a backup (music folders minus exclusions). */
    suspend fun resolveIncludedFolders(): List<String>

    /**
     * Manual backup. Gates on Premium + quota + grace + over_quota.
     * Copies approved audio into the app library, uploads those files, then the Room snapshot.
     */
    suspend fun startBackup(entitlement: StorageEntitlement?, isPremium: Boolean): Result<Unit>

    /**
     * GET /v1/backup/lease.
     * Sets [com.aethelsoft.grooveplayer.domain.model.CloudBackupState.otherDeviceHoldingLease]
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


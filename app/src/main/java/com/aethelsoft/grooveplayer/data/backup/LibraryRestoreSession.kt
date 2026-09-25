package com.aethelsoft.grooveplayer.data.backup

import android.content.Context
import com.aethelsoft.grooveplayer.data.local.db.GroovePlayerDatabase
import com.aethelsoft.grooveplayer.domain.backup.RestoreApplyRecovery
import com.aethelsoft.grooveplayer.domain.backup.RestorePhase
import com.aethelsoft.grooveplayer.domain.backup.StagingVerdict
import com.aethelsoft.grooveplayer.domain.model.CloudLibrarySnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Restore progress lives in SharedPreferences and a staging directory, never inside the
 * Room file that a restore used to overwrite.
 */
@Singleton
class LibraryRestoreSession @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun phase(): RestorePhase = RestoreApplyRecovery.parsePhase(prefs.getString(KEY_PHASE, null))

    fun beginDownload() {
        deleteStagingFiles()
        clearLibraryIdentity()
        setPhase(RestorePhase.DOWNLOADING)
    }

    fun markStaged() = setPhase(RestorePhase.STAGED)

    fun markFilesReady() = setPhase(RestorePhase.FILES_READY)

    fun markSwapping() = setPhase(RestorePhase.SWAPPING)

    fun markCommitted() = setPhase(RestorePhase.COMMITTED)

    fun markApplying() = setPhase(RestorePhase.APPLYING)

    fun discard() {
        deleteStagingFiles()
        clearLibraryIdentity()
        setPhase(RestorePhase.IDLE)
        clearLegacyRestartFlag()
    }

    /** Cloud library identity for the snapshot this restore is applying. */
    fun rememberLibraryIdentity(snapshot: CloudLibrarySnapshot) {
        prefs.edit()
            .putBoolean(KEY_LIB_SAVED, true)
            .putString(KEY_LIB_ID, snapshot.objectId)
            .putString(KEY_LIB_HASH, snapshot.contentHash)
            .putLong(KEY_LIB_SIZE, snapshot.sizeBytes)
            .putString(KEY_LIB_CREATED, snapshot.createdAtIso)
            .putString(KEY_LIB_R2, snapshot.r2Key)
            .putString(KEY_LIB_PATH, snapshot.logicalPath)
            .putInt(KEY_LIB_SCHEMA, snapshot.schemaVersion ?: -1)
            .putString(KEY_LIB_APP, snapshot.appVersion)
            .commit()
    }

    fun libraryIdentity(): CloudLibrarySnapshot? {
        if (!prefs.getBoolean(KEY_LIB_SAVED, false)) return null
        val schema = prefs.getInt(KEY_LIB_SCHEMA, -1)
        return CloudLibrarySnapshot(
            objectId = prefs.getString(KEY_LIB_ID, null),
            contentHash = prefs.getString(KEY_LIB_HASH, null),
            sizeBytes = prefs.getLong(KEY_LIB_SIZE, 0L),
            r2Key = prefs.getString(KEY_LIB_R2, null),
            logicalPath = prefs.getString(KEY_LIB_PATH, null),
            schemaVersion = schema.takeIf { it >= 0 },
            appVersion = prefs.getString(KEY_LIB_APP, null),
            createdAtIso = prefs.getString(KEY_LIB_CREATED, null),
            dryRun = false,
        )
    }

    fun clearLibraryIdentity() {
        prefs.edit()
            .remove(KEY_LIB_SAVED)
            .remove(KEY_LIB_ID)
            .remove(KEY_LIB_HASH)
            .remove(KEY_LIB_SIZE)
            .remove(KEY_LIB_CREATED)
            .remove(KEY_LIB_R2)
            .remove(KEY_LIB_PATH)
            .remove(KEY_LIB_SCHEMA)
            .remove(KEY_LIB_APP)
            .commit()
    }

    fun clearLegacyRestartFlag() {
        prefs.edit().remove(KEY_NEEDS_RESTART_AFTER_RESTORE).commit()
    }

    fun partialGzip(): File = File(stagingDir(), "room.db.gz.partial")

    fun partialDatabase(): File = File(stagingDir(), "room.db.partial")

    fun stagingDatabase(): File = File(stagingDir(), "room.db")

    fun peekStagingVerdict(): StagingVerdict? {
        val file = stagingDatabase()
        if (!file.isFile) return null
        val header = ByteArray(RestoreApplyRecovery.HEADER_PROBE_BYTES)
        val read = file.inputStream().use { input -> input.read(header) }
        if (read < RestoreApplyRecovery.HEADER_PROBE_BYTES) return StagingVerdict.TRUNCATED
        return RestoreApplyRecovery.verdict(
            fileSize = file.length(),
            header = header,
            localSchema = GroovePlayerDatabase.SCHEMA_VERSION,
        )
    }

    private fun setPhase(phase: RestorePhase) {
        prefs.edit().putString(KEY_PHASE, phase.name).commit()
    }

    private fun stagingDir(): File =
        File(context.filesDir, "restore-staging").also { dir -> dir.mkdirs() }

    private fun deleteStagingFiles() {
        partialGzip().delete()
        partialDatabase().delete()
        val staged = stagingDatabase()
        staged.delete()
        File(staged.path + "-wal").delete()
        File(staged.path + "-shm").delete()
        File(staged.path + "-journal").delete()
    }

    companion object {
        const val PREFS = "groove_cloud_backup"
        const val KEY_PHASE = "library_restore_phase"
        const val KEY_LAST_ERROR = "last_error"
        const val KEY_NEEDS_RESTART_AFTER_RESTORE = "needs_restart_after_library_restore"
        private const val KEY_LIB_SAVED = "restore_library_saved"
        private const val KEY_LIB_ID = "restore_library_object_id"
        private const val KEY_LIB_HASH = "restore_library_content_hash"
        private const val KEY_LIB_SIZE = "restore_library_size_bytes"
        private const val KEY_LIB_CREATED = "restore_library_created_at"
        private const val KEY_LIB_R2 = "restore_library_r2_key"
        private const val KEY_LIB_PATH = "restore_library_logical_path"
        private const val KEY_LIB_SCHEMA = "restore_library_schema"
        private const val KEY_LIB_APP = "restore_library_app_version"
    }
}

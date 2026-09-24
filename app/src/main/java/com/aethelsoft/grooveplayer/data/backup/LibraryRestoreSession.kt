package com.aethelsoft.grooveplayer.data.backup

import android.content.Context
import com.aethelsoft.grooveplayer.data.local.db.GroovePlayerDatabase
import com.aethelsoft.grooveplayer.domain.backup.RestoreApplyRecovery
import com.aethelsoft.grooveplayer.domain.backup.RestorePhase
import com.aethelsoft.grooveplayer.domain.backup.StagingVerdict
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
        setPhase(RestorePhase.DOWNLOADING)
    }

    fun markStaged() = setPhase(RestorePhase.STAGED)

    fun markFilesReady() = setPhase(RestorePhase.FILES_READY)

    fun markSwapping() = setPhase(RestorePhase.SWAPPING)

    fun markCommitted() = setPhase(RestorePhase.COMMITTED)

    fun markApplying() = setPhase(RestorePhase.APPLYING)

    fun discard() {
        deleteStagingFiles()
        setPhase(RestorePhase.IDLE)
        clearLegacyRestartFlag()
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
    }
}

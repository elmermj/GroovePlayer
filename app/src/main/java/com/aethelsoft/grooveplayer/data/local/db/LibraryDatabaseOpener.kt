package com.aethelsoft.grooveplayer.data.local.db

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.aethelsoft.grooveplayer.data.backup.LibraryRestoreSession
import com.aethelsoft.grooveplayer.domain.backup.RestoreApplyRecovery
import com.aethelsoft.grooveplayer.domain.backup.RestorePhase
import java.io.File

/**
 * Opens Room once per process. A database newer than this build is left on disk and the
 * UI asks the user to update the app. A corrupt file from an interrupted restore can still
 * be replaced. Likes, history, and playlists are not deleted on upgrade or downgrade.
 */
object LibraryDatabaseOpener {
    private const val TAG = "LibraryDatabaseOpener"

    @Volatile
    var needsAppUpdate: Boolean = false

    fun open(context: Context): GroovePlayerDatabase {
        reconcileInterruptedSwap(context)
        val onDisk = SqliteUserVersion.read(context.getDatabasePath(GroovePlayerDatabase.DATABASE_NAME))
        if (DatabaseOpenPolicy.isDowngrade(onDisk, GroovePlayerDatabase.SCHEMA_VERSION)) {
            needsAppUpdate = true
            return inMemory(context)
        }
        val first = build(context)
        try {
            first.openHelper.writableDatabase
            RoomDbSwapFiles.forContext(context).acknowledgeOpen()
            return first
        } catch (error: Exception) {
            Log.e(TAG, "Room failed to open; recovering without a crash loop", error)
            runCatching { first.close() }
            if (DatabaseOpenPolicy.keepFileOnOpenFailure(error)) {
                needsAppUpdate = true
                return inMemory(context)
            }
            if (RestoreApplyRecovery.isUnrecoverableDatabaseFailure(error)) {
                val restored = runCatching {
                    RoomDbSwapFiles.forContext(context).restorePreviousIfPresent()
                }.getOrDefault(false)
                if (restored) {
                    val retry = build(context)
                    try {
                        retry.openHelper.writableDatabase
                        writePhase(
                            context,
                            RestorePhase.IDLE,
                            error = "Restore could not open the new library, so the previous library was kept. " +
                                "Songs already in the app library were left in place.",
                        )
                        RoomDbSwapFiles.forContext(context).acknowledgeOpen()
                        return retry
                    } catch (retryError: Exception) {
                        Log.e(TAG, "Rollback database still failed to open", retryError)
                        runCatching { retry.close() }
                        if (DatabaseOpenPolicy.keepFileOnOpenFailure(retryError)) {
                            needsAppUpdate = true
                            return inMemory(context)
                        }
                    }
                }
                deleteRoomFiles(context)
            }
        }
        val second = build(context)
        return try {
            second.openHelper.writableDatabase
            second
        } catch (error: Exception) {
            Log.e(TAG, "Room still failed to open", error)
            runCatching { second.close() }
            if (DatabaseOpenPolicy.keepFileOnOpenFailure(error)) {
                needsAppUpdate = true
                return inMemory(context)
            }
            deleteRoomFiles(context)
            build(context)
        }
    }

    private fun build(context: Context): GroovePlayerDatabase {
        return Room.databaseBuilder(
            context,
            GroovePlayerDatabase::class.java,
            GroovePlayerDatabase.DATABASE_NAME,
        )
            .addMigrations(*GroovePlayerMigrations.ALL.toTypedArray())
            .build()
    }

    private fun inMemory(context: Context): GroovePlayerDatabase {
        return Room.inMemoryDatabaseBuilder(context, GroovePlayerDatabase::class.java).build()
    }

    /**
     * Finish or undo a database rename before Room opens the live file.
     * App library audio is not touched.
     */
    private fun reconcileInterruptedSwap(context: Context) {
        val swap = RoomDbSwapFiles.forContext(context)
        when (swap.recover()) {
            RecoveryResult.PROMOTED -> writePhase(context, RestorePhase.IDLE)
            RecoveryResult.ROLLED_BACK -> {
                val staged = File(context.filesDir, "restore-staging/room.db")
                val phase = if (RoomDbSwapFiles.isSqliteFile(staged)) {
                    RestorePhase.FILES_READY
                } else {
                    RestorePhase.IDLE
                }
                writePhase(
                    context,
                    phase,
                    error = "Restore rolled back to the previous library. " +
                        "Songs already in the app library were left in place.",
                )
            }
            RecoveryResult.UNCHANGED -> Unit
        }
    }

    private fun writePhase(context: Context, phase: RestorePhase, error: String? = null) {
        val editor = context.getSharedPreferences(LibraryRestoreSession.PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(LibraryRestoreSession.KEY_PHASE, phase.name)
        if (error != null) {
            editor.putString(LibraryRestoreSession.KEY_LAST_ERROR, error)
        }
        editor.commit()
    }

    private fun deleteRoomFiles(context: Context) {
        context.deleteDatabase(GroovePlayerDatabase.DATABASE_NAME)
        val db = context.getDatabasePath(GroovePlayerDatabase.DATABASE_NAME)
        File(db.path + "-wal").delete()
        File(db.path + "-shm").delete()
        File(db.path + "-journal").delete()
    }
}

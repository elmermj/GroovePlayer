package com.aethelsoft.grooveplayer.data.local.db

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.aethelsoft.grooveplayer.domain.backup.RestoreApplyRecovery
import java.io.File

/**
 * Opens Room once per process. If a previous restore left a file Room cannot open,
 * the file is replaced with a fresh database so startup can reach Home. Downloads
 * live outside this file and are indexed again from MediaStore.
 */
object LibraryDatabaseOpener {
    private const val TAG = "LibraryDatabaseOpener"

    fun open(context: Context): GroovePlayerDatabase {
        val first = build(context)
        try {
            first.openHelper.writableDatabase
            return first
        } catch (error: Exception) {
            Log.e(TAG, "Room failed to open; recovering without a crash loop", error)
            runCatching { first.close() }
            if (RestoreApplyRecovery.isUnrecoverableDatabaseFailure(error)) {
                deleteRoomFiles(context)
            }
        }
        val second = build(context)
        return try {
            second.openHelper.writableDatabase
            second
        } catch (error: Exception) {
            Log.e(TAG, "Room still failed to open; recreating an empty library database", error)
            runCatching { second.close() }
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
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    private fun deleteRoomFiles(context: Context) {
        context.deleteDatabase(GroovePlayerDatabase.DATABASE_NAME)
        val db = context.getDatabasePath(GroovePlayerDatabase.DATABASE_NAME)
        File(db.path + "-wal").delete()
        File(db.path + "-shm").delete()
        File(db.path + "-journal").delete()
    }
}

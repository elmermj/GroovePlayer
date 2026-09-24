package com.aethelsoft.grooveplayer.data.backup

import android.database.sqlite.SQLiteDatabase
import com.aethelsoft.grooveplayer.domain.backup.BackupCatalogPaths
import com.aethelsoft.grooveplayer.domain.backup.PlacedCloudSong

/** Rewrites snapshot `songs.sourcePath` values to app-private library paths. */
object StagedCatalogRewriter {
    fun rewrite(databaseFile: java.io.File, placements: List<PlacedCloudSong>) {
        val db = SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        )
        db.use { sqlite ->
            if (!tableExists(sqlite, "songs")) return@use
            val updates = mutableListOf<Pair<String, String>>()
            sqlite.rawQuery("SELECT songId, sourcePath FROM songs", null).use { cursor ->
                val idColumn = cursor.getColumnIndex("songId")
                val pathColumn = cursor.getColumnIndex("sourcePath")
                if (idColumn < 0 || pathColumn < 0) return@use
                while (cursor.moveToNext()) {
                    if (cursor.isNull(pathColumn)) continue
                    val current = cursor.getString(pathColumn) ?: continue
                    val rewritten = BackupCatalogPaths.rewrite(current, placements)
                    if (rewritten != current) {
                        updates += cursor.getString(idColumn) to rewritten
                    }
                }
            }
            if (updates.isEmpty()) return@use
            sqlite.beginTransaction()
            try {
                for ((songId, path) in updates) {
                    sqlite.execSQL(
                        "UPDATE songs SET sourcePath = ? WHERE songId = ?",
                        arrayOf(path, songId),
                    )
                }
                sqlite.setTransactionSuccessful()
            } finally {
                sqlite.endTransaction()
            }
            sqlite.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor ->
                cursor.moveToFirst()
            }
        }
        java.io.File(databaseFile.path + "-wal").delete()
        java.io.File(databaseFile.path + "-shm").delete()
    }

    private fun tableExists(db: SQLiteDatabase, table: String): Boolean {
        db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(table),
        ).use { cursor -> return cursor.moveToFirst() }
    }
}

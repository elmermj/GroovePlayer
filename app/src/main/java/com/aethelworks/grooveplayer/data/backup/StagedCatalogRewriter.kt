package com.aethelworks.grooveplayer.data.backup

import android.database.sqlite.SQLiteDatabase
import com.aethelworks.grooveplayer.domain.backup.BackupCatalogPaths
import com.aethelworks.grooveplayer.domain.backup.PlacedCloudSong
import java.io.File

/** Rewrites snapshot `songs.sourcePath` values to app-private library paths. */
object StagedCatalogRewriter {
    fun rewrite(databaseFile: File, placements: List<PlacedCloudSong>) {
        val db = SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        )
        db.use { sqlite ->
            if (rewrite(AndroidCatalogDb(sqlite), placements)) {
                sqlite.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor ->
                    cursor.moveToFirst()
                }
            }
        }
        File(databaseFile.path + "-wal").delete()
        File(databaseFile.path + "-shm").delete()
    }

    /**
     * [CatalogDb] is the SQL session. Unit tests supply a JVM SQLite connection;
     * restore supplies the staged snapshot file.
     */
    internal fun rewrite(db: CatalogDb, placements: List<PlacedCloudSong>): Boolean {
        val songs = db.query(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf("songs"),
        )
        if (songs.isEmpty()) return false
        // SQLite fails at prepare time when SELECT names a missing column.
        // Snapshots from before sourcePath existed (about 2026-09-24) need the
        // column added so restore can finish.
        val hasSourcePath = db.query("PRAGMA table_info(songs)").any { row ->
            row.text("name")?.equals("sourcePath", ignoreCase = true) == true
        }
        var changed = false
        if (!hasSourcePath) {
            db.exec("ALTER TABLE songs ADD COLUMN sourcePath TEXT")
            changed = true
        }
        val updates = mutableListOf<Pair<String, String>>()
        for (row in db.query("SELECT songId, sourcePath FROM songs")) {
            val current = row.text("sourcePath") ?: continue
            val rewritten = BackupCatalogPaths.rewrite(current, placements)
            if (rewritten != current) {
                val songId = row.text("songId") ?: continue
                updates += songId to rewritten
            }
        }
        if (updates.isEmpty()) return changed
        db.transaction {
            for ((songId, path) in updates) {
                db.exec(
                    "UPDATE songs SET sourcePath = ? WHERE songId = ?",
                    arrayOf<Any?>(path, songId),
                )
            }
        }
        return true
    }

    internal interface CatalogDb {
        fun query(sql: String, args: Array<String>? = null): List<Map<String, String?>>
        fun exec(sql: String, args: Array<Any?> = emptyArray())
        fun transaction(block: () -> Unit)
    }

    private class AndroidCatalogDb(private val db: SQLiteDatabase) : CatalogDb {
        override fun query(sql: String, args: Array<String>?): List<Map<String, String?>> {
            val rows = mutableListOf<Map<String, String?>>()
            db.rawQuery(sql, args).use { cursor ->
                while (cursor.moveToNext()) {
                    val row = LinkedHashMap<String, String?>(cursor.columnCount)
                    for (index in 0 until cursor.columnCount) {
                        row[cursor.getColumnName(index)] =
                            if (cursor.isNull(index)) null else cursor.getString(index)
                    }
                    rows += row
                }
            }
            return rows
        }

        override fun exec(sql: String, args: Array<Any?>) {
            if (args.isEmpty()) db.execSQL(sql) else db.execSQL(sql, args)
        }

        override fun transaction(block: () -> Unit) {
            db.beginTransaction()
            try {
                block()
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun Map<String, String?>.text(column: String): String? =
        entries.firstOrNull { it.key.equals(column, ignoreCase = true) }?.value
}

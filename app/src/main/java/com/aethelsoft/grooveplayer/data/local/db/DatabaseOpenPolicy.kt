package com.aethelsoft.grooveplayer.data.local.db

import java.io.File

/**
 * Upgrade and downgrade must leave the on-disk library in place.
 * A newer file than this app understands is a downgrade of the app, not a reason to delete rows.
 */
object DatabaseOpenPolicy {
    fun isDowngrade(onDiskVersion: Int?, schemaVersion: Int): Boolean =
        onDiskVersion != null && onDiskVersion > schemaVersion

    fun keepFileOnOpenFailure(error: Throwable): Boolean {
        val text = generateSequence(error) { it.cause }
            .joinToString(" ") { it.message.orEmpty() }
            .lowercase()
        return "downgrade" in text || "migration" in text || "data integrity" in text
    }
}

/** user_version from the SQLite header. Null when the file is missing or not a database. */
object SqliteUserVersion {
    fun read(file: File): Int? {
        if (!file.isFile || file.length() < 64) return null
        val header = ByteArray(64)
        file.inputStream().use { input ->
            if (input.read(header) != 64) return null
        }
        val magic = "SQLite format 3\u0000".encodeToByteArray()
        if (!header.copyOfRange(0, magic.size).contentEquals(magic)) return null
        return ((header[60].toInt() and 0xff) shl 24) or
            ((header[61].toInt() and 0xff) shl 16) or
            ((header[62].toInt() and 0xff) shl 8) or
            (header[63].toInt() and 0xff)
    }
}

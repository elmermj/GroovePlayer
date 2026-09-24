package com.aethelsoft.grooveplayer.domain.backup

/**
 * Decisions for cloud library restore that must not depend on a live Room connection.
 *
 * The live database file is never the download destination. A process death mid-download
 * discards the partial file and opens Home. A process death after the snapshot is validated
 * resumes apply.
 */
enum class RestorePhase {
    IDLE,
    DOWNLOADING,
    STAGED,
    APPLYING,
}

enum class StagingVerdict {
    VALID,
    TRUNCATED,
    NOT_SQLITE,
    NEWER_THAN_APP,
}

enum class ColdStartAction {
    OPEN_HOME,
    RESUME_APPLY,
    DISCARD_AND_HOME,
}

object RestoreApplyRecovery {
    const val HEADER_PROBE_BYTES = 100
    const val USER_VERSION_OFFSET = 60

    private val SQLITE_MAGIC = "SQLite format 3\u0000".encodeToByteArray()

    fun parsePhase(raw: String?): RestorePhase {
        if (raw.isNullOrBlank()) return RestorePhase.IDLE
        return runCatching { RestorePhase.valueOf(raw) }.getOrDefault(RestorePhase.IDLE)
    }

    fun isSqliteHeader(header: ByteArray): Boolean {
        if (header.size < SQLITE_MAGIC.size) return false
        return header.copyOfRange(0, SQLITE_MAGIC.size).contentEquals(SQLITE_MAGIC)
    }

    /** Room stores `@Database(version)` as SQLite `PRAGMA user_version` (big-endian at offset 60). */
    fun readUserVersion(header: ByteArray): Int? {
        if (header.size < USER_VERSION_OFFSET + 4 || !isSqliteHeader(header)) return null
        return ((header[USER_VERSION_OFFSET].toInt() and 0xff) shl 24) or
            ((header[USER_VERSION_OFFSET + 1].toInt() and 0xff) shl 16) or
            ((header[USER_VERSION_OFFSET + 2].toInt() and 0xff) shl 8) or
            (header[USER_VERSION_OFFSET + 3].toInt() and 0xff)
    }

    fun verdict(fileSize: Long, header: ByteArray, localSchema: Int): StagingVerdict {
        if (fileSize < HEADER_PROBE_BYTES || header.size < HEADER_PROBE_BYTES) {
            return StagingVerdict.TRUNCATED
        }
        if (!isSqliteHeader(header)) return StagingVerdict.NOT_SQLITE
        val version = readUserVersion(header) ?: return StagingVerdict.NOT_SQLITE
        if (version <= 0) return StagingVerdict.NOT_SQLITE
        if (version > localSchema) return StagingVerdict.NEWER_THAN_APP
        return StagingVerdict.VALID
    }

    /**
     * [RestorePhase.DOWNLOADING] always discards, even if a partial file already has a SQLite
     * header. Only a fully staged snapshot is safe to apply.
     */
    fun coldStartAction(phase: RestorePhase, verdict: StagingVerdict?): ColdStartAction {
        return when (phase) {
            RestorePhase.IDLE -> ColdStartAction.OPEN_HOME
            RestorePhase.DOWNLOADING -> ColdStartAction.DISCARD_AND_HOME
            RestorePhase.STAGED, RestorePhase.APPLYING -> when (verdict) {
                StagingVerdict.VALID -> ColdStartAction.RESUME_APPLY
                else -> ColdStartAction.DISCARD_AND_HOME
            }
        }
    }

    /**
     * Failures that mean the on-disk Room file cannot be opened again.
     * A closed connection pool is an in-process problem; the file itself is left alone.
     */
    fun isUnrecoverableDatabaseFailure(error: Throwable): Boolean {
        val text = generateSequence(error) { it.cause }
            .joinToString(" ") { "${it.javaClass.name} ${it.message.orEmpty()}" }
            .lowercase()
        if ("connection pool has been closed" in text) return false
        return "not a database" in text ||
            "corrupt" in text ||
            "malformed" in text ||
            "data integrity" in text ||
            "migration" in text ||
            "downgrade" in text ||
            "sqliteexception" in text ||
            "disk image" in text
    }
}

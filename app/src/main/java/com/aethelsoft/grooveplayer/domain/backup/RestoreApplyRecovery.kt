package com.aethelsoft.grooveplayer.domain.backup

/**
 * Decisions for cloud library restore that must not depend on a live Room connection.
 *
 * The live database file is never the download destination. A process death mid-download
 * resumes the download and does not apply a partial snapshot. A process death after the
 * required audio is on disk resumes the swap. A process death during the rename finishes
 * it or rolls back to the previous database before Room opens.
 */
enum class RestorePhase {
    IDLE,
    /** Room snapshot or audio is still downloading. A partial SQLite file is not applied. */
    DOWNLOADING,
    /** Snapshot verified; cloud audio may still be missing. */
    STAGED,
    /** Legacy in-place apply. Resumed as a download so files are confirmed before swap. */
    APPLYING,
    /** Every required cloud song has bytes on disk and the snapshot is ready to swap. */
    FILES_READY,
    /** Rename of the live database is in progress. Cold start finishes or rolls back. */
    SWAPPING,
    /** Incoming database is at the live path. Next process opens it; do not swap again. */
    COMMITTED,
}

enum class StagingVerdict {
    VALID,
    TRUNCATED,
    NOT_SQLITE,
    NEWER_THAN_APP,
}

enum class ColdStartAction {
    OPEN_HOME,
    /** Continue downloads, then swap. Does not apply a partial snapshot. */
    RESUME_DOWNLOAD,
    /** Audio is already confirmed. Swap the staged database, or finish a parked swap. */
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
     * [RestorePhase.DOWNLOADING] resumes the download even if a partial file already has a
     * SQLite header. Only [RestorePhase.FILES_READY] (or a swap already in progress) may
     * replace the live database. [RestorePhase.COMMITTED] opens Home on the swapped file.
     */
    fun coldStartAction(phase: RestorePhase, verdict: StagingVerdict?): ColdStartAction {
        return when (phase) {
            RestorePhase.IDLE, RestorePhase.COMMITTED -> ColdStartAction.OPEN_HOME
            RestorePhase.DOWNLOADING -> ColdStartAction.RESUME_DOWNLOAD
            RestorePhase.STAGED, RestorePhase.APPLYING -> when (verdict) {
                StagingVerdict.VALID -> ColdStartAction.RESUME_DOWNLOAD
                else -> ColdStartAction.DISCARD_AND_HOME
            }
            RestorePhase.FILES_READY, RestorePhase.SWAPPING -> when (verdict) {
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

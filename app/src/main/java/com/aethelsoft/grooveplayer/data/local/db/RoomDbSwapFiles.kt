package com.aethelsoft.grooveplayer.data.local.db

import android.content.Context
import com.aethelsoft.grooveplayer.domain.backup.DbSwapAction
import com.aethelsoft.grooveplayer.domain.backup.DbSwapSnapshot
import com.aethelsoft.grooveplayer.domain.backup.DbSwapStep
import com.aethelsoft.grooveplayer.domain.backup.RoomDbSwap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Copies the live Room file aside, writes the snapshot to a sibling, then renames.
 * A kill mid-rename is finished or rolled back by [recover] before Room opens.
 */
class RoomDbSwapFiles(
    private val live: File,
    private val aside: File,
    private val incoming: File,
    private val previous: File,
    private val journal: File,
) {
    fun step(): DbSwapStep = RoomDbSwap.parseStep(
        if (journal.isFile) journal.readText() else null,
    )

    fun arm(staging: File) {
        require(isSqliteFile(staging)) { "Staged library is not a SQLite database" }
        require(live.isFile) { "Live library database is missing" }
        copyDurable(live, previous)
        require(isSqliteFile(previous)) { "Previous library copy failed verification" }
        copyDurable(staging, incoming)
        require(isSqliteFile(incoming)) { "Incoming library copy failed verification" }
        writeStep(DbSwapStep.ARMED)
    }

    /**
     * Park the live file, promote incoming, drop WAL/SHM so the next process cannot
     * replay the old log onto the new database. Does not close Room.
     */
    fun commit() {
        val current = step()
        if (current == DbSwapStep.COMMITTED && isSqliteFile(live)) return
        require(current == DbSwapStep.ARMED || current == DbSwapStep.LIVE_PARKED) {
            "Database swap is not armed (step=$current)"
        }
        if (current != DbSwapStep.LIVE_PARKED) {
            writeStep(DbSwapStep.LIVE_PARKED)
        }
        if (live.exists()) {
            if (aside.exists() && !aside.delete()) {
                error("Could not clear parked database ${aside.name}")
            }
            if (!live.renameTo(aside)) {
                copyDurable(live, aside)
                if (!live.delete()) error("Could not park the live database")
            }
        }
        deleteSidecars(live)
        if (!live.exists()) {
            if (!incoming.renameTo(live)) {
                copyDurable(incoming, live)
                incoming.delete()
            }
        }
        require(isSqliteFile(live)) { "Promoted library failed verification" }
        deleteSidecars(live)
        writeStep(DbSwapStep.COMMITTED)
    }

    /**
     * After Room has opened the live file: drop the rollback copy once a committed
     * swap is known-good. An armed swap keeps [previous] until commit or rollback.
     */
    fun acknowledgeOpen() {
        if (step() == DbSwapStep.COMMITTED) {
            writeStep(DbSwapStep.IDLE)
            previous.delete()
            aside.delete()
            incoming.delete()
        }
    }

    /**
     * Copy the pre-swap generation back when the swapped file cannot be opened.
     * An idle journal never rolls back, even if an old previous file is still on disk.
     */
    fun restorePreviousIfPresent(): Boolean {
        if (step() != DbSwapStep.COMMITTED && step() != DbSwapStep.LIVE_PARKED) return false
        if (!isSqliteFile(previous)) return false
        incoming.delete()
        aside.delete()
        copyDurable(previous, live)
        deleteSidecars(live)
        writeStep(DbSwapStep.IDLE)
        return isSqliteFile(live)
    }

    /**
     * Undo an in-progress swap. A committed swap is left alone so the next process
     * can open it. Groove Downloads audio is not touched.
     */
    fun rollback() {
        when (step()) {
            DbSwapStep.COMMITTED -> Unit
            DbSwapStep.LIVE_PARKED -> {
                if (aside.isFile) restoreAside() else restorePrevious()
            }
            DbSwapStep.ARMED, DbSwapStep.IDLE -> {
                // Live file was never parked. Drop the spare copy so a later open
                // failure cannot roll the library back to this attempt.
                incoming.delete()
                aside.delete()
                previous.delete()
                writeStep(DbSwapStep.IDLE)
            }
        }
    }

    /**
     * @return PROMOTED when the incoming snapshot is now live,
     * ROLLED_BACK when the previous generation was restored,
     * UNCHANGED when the live file was already safe to open.
     */
    fun recover(): RecoveryResult {
        return when (RoomDbSwap.nextAction(snapshot())) {
            DbSwapAction.NONE -> {
                if (step() == DbSwapStep.COMMITTED && isSqliteFile(live)) {
                    RecoveryResult.PROMOTED
                } else {
                    RecoveryResult.UNCHANGED
                }
            }
            DbSwapAction.PROMOTE_INCOMING -> {
                promoteIncoming()
                RecoveryResult.PROMOTED
            }
            DbSwapAction.RESTORE_ASIDE -> {
                restoreAside()
                RecoveryResult.ROLLED_BACK
            }
            DbSwapAction.RESTORE_PREVIOUS -> {
                restorePrevious()
                RecoveryResult.ROLLED_BACK
            }
        }
    }

    fun snapshot(): DbSwapSnapshot = DbSwapSnapshot(
        step = step(),
        liveExists = live.isFile,
        liveHeaderOk = isSqliteFile(live),
        asideExists = aside.isFile,
        asideHeaderOk = isSqliteFile(aside),
        incomingExists = incoming.isFile,
        incomingHeaderOk = isSqliteFile(incoming),
        previousExists = previous.isFile,
        previousHeaderOk = isSqliteFile(previous),
    )

    private fun promoteIncoming() {
        if (live.exists() && !live.delete()) error("Could not replace live database")
        if (!incoming.renameTo(live)) {
            copyDurable(incoming, live)
            incoming.delete()
        }
        require(isSqliteFile(live)) { "Promoted library failed verification" }
        deleteSidecars(live)
        writeStep(DbSwapStep.COMMITTED)
    }

    private fun restoreAside() {
        incoming.delete()
        if (live.exists() && !live.delete()) error("Could not clear live database during rollback")
        if (!aside.renameTo(live)) {
            copyDurable(aside, live)
            aside.delete()
        }
        deleteSidecars(live)
        writeStep(DbSwapStep.IDLE)
    }

    private fun restorePrevious() {
        require(isSqliteFile(previous)) { "No previous library to roll back to" }
        incoming.delete()
        aside.delete()
        copyDurable(previous, live)
        require(isSqliteFile(live)) { "Rollback copy failed verification" }
        deleteSidecars(live)
        writeStep(DbSwapStep.IDLE)
    }

    private fun writeStep(step: DbSwapStep) {
        journal.parentFile?.mkdirs()
        FileOutputStream(journal).use { out ->
            out.write(step.name.toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
    }

    companion object {
        fun forContext(context: Context): RoomDbSwapFiles {
            val live = context.getDatabasePath(GroovePlayerDatabase.DATABASE_NAME)
            return RoomDbSwapFiles(
                live = live,
                aside = File(live.path + ".aside"),
                incoming = File(live.path + ".incoming"),
                previous = File(
                    context.filesDir,
                    "restore-previous/${GroovePlayerDatabase.DATABASE_NAME}",
                ),
                journal = File(context.filesDir, "restore-swap/journal"),
            )
        }

        fun isSqliteFile(file: File): Boolean {
            if (!file.isFile || file.length() < 16L) return false
            val header = ByteArray(16)
            FileInputStream(file).use { input ->
                if (input.read(header) < header.size) return false
            }
            return header.contentEquals(SQLITE_MAGIC)
        }

        fun copyDurable(
            src: File,
            dest: File,
            onBytes: ((copied: Long, total: Long) -> Unit)? = null,
        ) {
            dest.parentFile?.mkdirs()
            val tmp = File(dest.parentFile, dest.name + ".tmp")
            if (tmp.exists()) tmp.delete()
            FileInputStream(src).channel.use { input ->
                FileOutputStream(tmp).channel.use { output ->
                    var position = 0L
                    val size = input.size()
                    onBytes?.invoke(0L, size)
                    while (position < size) {
                        val requested = if (onBytes == null) {
                            size - position
                        } else {
                            minOf(COPY_PROGRESS_BYTES, size - position)
                        }
                        val transferred = input.transferTo(position, requested, output)
                        if (transferred <= 0L) break
                        position += transferred
                        onBytes?.invoke(position, size)
                    }
                    output.force(true)
                }
            }
            if (dest.exists() && !dest.delete()) error("Could not replace ${dest.name}")
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
        }

        private const val COPY_PROGRESS_BYTES = 256L * 1024L

        fun deleteSidecars(database: File) {
            listOf("-wal", "-shm", "-journal").forEach { suffix ->
                File(database.path + suffix).delete()
            }
        }

        private val SQLITE_MAGIC = "SQLite format 3\u0000".encodeToByteArray()
    }
}

enum class RecoveryResult {
    UNCHANGED,
    PROMOTED,
    ROLLED_BACK,
}

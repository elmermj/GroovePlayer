package com.aethelsoft.grooveplayer.domain.backup

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/** Which exclusive cloud transfer currently owns [BackupJobGate]. */
enum class BackupTransfer {
    BACKUP,
    RESTORE,
}

/**
 * One backup or library restore at a time. A second start fails immediately
 * instead of waiting and then running after the first job.
 */
@Singleton
class BackupJobGate @Inject constructor() {
    private val owner = AtomicReference<BackupTransfer?>(null)

    fun current(): BackupTransfer? = owner.get()

    fun tryAcquire(transfer: BackupTransfer): Boolean = owner.compareAndSet(null, transfer)

    fun release(transfer: BackupTransfer) {
        owner.compareAndSet(transfer, null)
    }

    companion object {
        const val BUSY_MESSAGE = "A backup or restore is already running."
    }
}

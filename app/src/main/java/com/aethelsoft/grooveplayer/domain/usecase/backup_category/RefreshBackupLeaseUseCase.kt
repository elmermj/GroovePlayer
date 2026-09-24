package com.aethelsoft.grooveplayer.domain.usecase.backup_category

import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import javax.inject.Inject

/** GET /v1/backup/lease and publish whether another device holds the backup lock. */
class RefreshBackupLeaseUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke() {
        backupRepository.refreshBackupLease()
    }
}

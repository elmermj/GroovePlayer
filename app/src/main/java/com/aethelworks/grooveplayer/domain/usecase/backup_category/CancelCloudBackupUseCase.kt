package com.aethelworks.grooveplayer.domain.usecase.backup_category

import com.aethelworks.grooveplayer.domain.repository.BackupRepository
import javax.inject.Inject

class CancelCloudBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    operator fun invoke() = backupRepository.cancelBackup()
}

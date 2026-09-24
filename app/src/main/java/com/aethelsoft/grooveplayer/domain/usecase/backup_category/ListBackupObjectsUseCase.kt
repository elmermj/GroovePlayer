package com.aethelsoft.grooveplayer.domain.usecase.backup_category

import com.aethelsoft.grooveplayer.domain.model.BackupObject
import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import javax.inject.Inject

class ListBackupObjectsUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(): Result<List<BackupObject>> =
        backupRepository.listRemoteObjects()
}

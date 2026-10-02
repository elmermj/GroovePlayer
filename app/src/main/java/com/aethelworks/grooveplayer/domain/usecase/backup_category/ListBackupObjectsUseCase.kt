package com.aethelworks.grooveplayer.domain.usecase.backup_category

import com.aethelworks.grooveplayer.domain.model.BackupObject
import com.aethelworks.grooveplayer.domain.repository.BackupRepository
import javax.inject.Inject

class ListBackupObjectsUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(): Result<List<BackupObject>> =
        backupRepository.listRemoteObjects()
}

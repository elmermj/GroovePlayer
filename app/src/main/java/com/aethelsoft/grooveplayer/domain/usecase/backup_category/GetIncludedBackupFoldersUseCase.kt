package com.aethelsoft.grooveplayer.domain.usecase.backup_category

import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import javax.inject.Inject

class GetIncludedBackupFoldersUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(): List<String> = backupRepository.resolveIncludedFolders()
}

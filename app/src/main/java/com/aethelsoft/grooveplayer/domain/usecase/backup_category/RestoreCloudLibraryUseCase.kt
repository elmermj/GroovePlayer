package com.aethelsoft.grooveplayer.domain.usecase.backup_category

import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import javax.inject.Inject

class RestoreCloudLibraryUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend fun stage(): Result<Unit> = backupRepository.stageLibraryRestore()

    suspend fun apply(): Result<Unit> = backupRepository.applyStagedLibraryRestore()
}

package com.aethelsoft.grooveplayer.domain.usecase.backup_category

import com.aethelsoft.grooveplayer.domain.backup.RestoreProgressSnapshot
import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class RestoreCloudLibraryUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    fun observeProgress(): Flow<RestoreProgressSnapshot> = backupRepository.observeRestoreProgress()

    suspend fun stage(): Result<Unit> = backupRepository.stageLibraryRestore()

    suspend fun apply(): Result<Unit> = backupRepository.applyStagedLibraryRestore()
}

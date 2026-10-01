package com.aethelworks.grooveplayer.domain.usecase.backup_category

import com.aethelworks.grooveplayer.domain.model.CloudBackupState
import com.aethelworks.grooveplayer.domain.repository.BackupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCloudBackupStateUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    operator fun invoke(): Flow<CloudBackupState> = backupRepository.observeBackupState()
}

package com.aethelworks.grooveplayer.domain.usecase.backup_category

import com.aethelworks.grooveplayer.domain.model.TrimCloudBackupRequest
import com.aethelworks.grooveplayer.domain.model.TrimCloudBackupResult
import com.aethelworks.grooveplayer.domain.repository.BackupRepository
import javax.inject.Inject

class TrimCloudBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(request: TrimCloudBackupRequest): Result<TrimCloudBackupResult> =
        backupRepository.trimCloudBackup(request)
}

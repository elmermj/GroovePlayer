package com.aethelsoft.grooveplayer.domain.usecase.backup_category

import com.aethelsoft.grooveplayer.domain.model.TrimCloudBackupRequest
import com.aethelsoft.grooveplayer.domain.model.TrimCloudBackupResult
import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import javax.inject.Inject

class TrimCloudBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(request: TrimCloudBackupRequest): Result<TrimCloudBackupResult> =
        backupRepository.trimCloudBackup(request)
}

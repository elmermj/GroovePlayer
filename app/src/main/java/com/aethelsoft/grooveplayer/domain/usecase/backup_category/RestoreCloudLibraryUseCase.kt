package com.aethelsoft.grooveplayer.domain.usecase.backup_category

import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import javax.inject.Inject

class RestoreCloudLibraryUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(): Result<Unit> =
        backupRepository.restoreLibraryFromCloud()
}

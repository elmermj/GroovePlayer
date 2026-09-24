package com.aethelsoft.grooveplayer.domain.usecase.backup_category

import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import javax.inject.Inject

/** Cloud-only delete — never implies local library removal. */
class DeleteBackupObjectUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(objectId: String): Result<Unit> =
        backupRepository.deleteRemoteObject(objectId)
}

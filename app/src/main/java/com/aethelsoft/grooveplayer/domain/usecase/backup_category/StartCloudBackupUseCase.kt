package com.aethelsoft.grooveplayer.domain.usecase.backup_category

import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import javax.inject.Inject

class StartCloudBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        // Always re-fetch /v1/me so quota/hard_stop gates use live storage (QA under-quota).
        runCatching { authRepository.refreshSession() }
        val user = authRepository.getAuthUser()
        val isPremium = user?.privilegeTier == PrivilegeTier.PREMIUM
        val entitlement = user?.storage
        return backupRepository.startBackup(entitlement, isPremium)
    }
}

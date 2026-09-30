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
        val current = authRepository.getAuthUser()
        val isPremium = current?.privilegeTier == PrivilegeTier.PREMIUM
        // Live storage is only needed for a Premium upload. Free must not refresh.
        if (isPremium) {
            runCatching { authRepository.refreshSession() }
        }
        val user = authRepository.getAuthUser() ?: current
        return backupRepository.startBackup(user?.storage, user?.privilegeTier == PrivilegeTier.PREMIUM)
    }
}

package com.aethelsoft.grooveplayer.domain.usecase.user_category

import com.aethelsoft.grooveplayer.domain.model.StorageUsageData
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import javax.inject.Inject

/**
 * UseCase for getting storage usage breakdown (included vs excluded music folders).
 */
class GetStorageUsageUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(): StorageUsageData = musicRepository.getStorageUsage()
}

package com.aethelworks.grooveplayer.domain.usecase.user_category

import com.aethelworks.grooveplayer.domain.model.StorageUsageData
import com.aethelworks.grooveplayer.domain.repository.MusicRepository
import javax.inject.Inject

/**
 * UseCase for getting storage usage breakdown (included vs excluded music folders).
 */
class GetStorageUsageUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(): StorageUsageData = musicRepository.getStorageUsage()
}

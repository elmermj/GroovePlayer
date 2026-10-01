package com.aethelworks.grooveplayer.domain.usecase.backup_category

import com.aethelworks.grooveplayer.domain.model.CloudLibrarySnapshot
import com.aethelworks.grooveplayer.domain.repository.BackupRepository
import javax.inject.Inject

class FetchCloudLibraryUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(): Result<CloudLibrarySnapshot?> =
        backupRepository.fetchCloudLibraryMetadata()
}

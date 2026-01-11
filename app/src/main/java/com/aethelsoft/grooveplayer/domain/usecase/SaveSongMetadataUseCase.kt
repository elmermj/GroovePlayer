package com.aethelsoft.grooveplayer.domain.usecase

import com.aethelsoft.grooveplayer.domain.repository.SongMetadata
import com.aethelsoft.grooveplayer.domain.repository.SongMetadataRepository
import javax.inject.Inject

class SaveSongMetadataUseCase @Inject constructor(
    private val repository: SongMetadataRepository
) {
    suspend operator fun invoke(metadata: SongMetadata) {
        repository.saveMetadata(metadata)
    }
}


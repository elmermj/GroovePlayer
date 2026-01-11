package com.aethelsoft.grooveplayer.domain.usecase.player_category

import com.aethelsoft.grooveplayer.domain.repository.SongMetadata
import com.aethelsoft.grooveplayer.domain.repository.SongMetadataRepository
import javax.inject.Inject

class GetSongMetadataUseCase @Inject constructor(
    private val repository: SongMetadataRepository
) {
    suspend operator fun invoke(songId: String): SongMetadata? {
        return repository.getMetadata(songId)
    }
}


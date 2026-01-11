package com.aethelsoft.grooveplayer.domain.usecase.search_category

import com.aethelsoft.grooveplayer.domain.repository.SongMetadataRepository
import javax.inject.Inject

class SearchAlbumsUseCase @Inject constructor(
    private val repository: SongMetadataRepository
) {
    suspend operator fun invoke(query: String): List<String> {
        return if (query.isBlank()) {
            repository.getAllAlbums()
        } else {
            repository.searchAlbums(query)
        }
    }
}


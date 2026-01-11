package com.aethelsoft.grooveplayer.domain.usecase.search_category

import com.aethelsoft.grooveplayer.domain.repository.SongMetadataRepository
import javax.inject.Inject

class SearchArtistsUseCase @Inject constructor(
    private val repository: SongMetadataRepository
) {
    suspend operator fun invoke(query: String): List<String> {
        return if (query.isBlank()) {
            repository.getAllArtists()
        } else {
            repository.searchArtists(query)
        }
    }
}


package com.aethelsoft.grooveplayer.domain.usecase.search_category

import com.aethelsoft.grooveplayer.domain.repository.SongMetadataRepository
import javax.inject.Inject

class SearchGenresUseCase @Inject constructor(
    private val repository: SongMetadataRepository
) {
    suspend operator fun invoke(query: String): List<String> {
        return if (query.isBlank()) {
            repository.getAllGenres()
        } else {
            repository.searchGenres(query)
        }
    }
}


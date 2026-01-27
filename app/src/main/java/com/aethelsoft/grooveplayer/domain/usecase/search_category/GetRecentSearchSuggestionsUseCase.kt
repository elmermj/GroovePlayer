package com.aethelsoft.grooveplayer.domain.usecase.search_category

import com.aethelsoft.grooveplayer.domain.model.SearchSuggestion
import com.aethelsoft.grooveplayer.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentSearchSuggestionsUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<SearchSuggestion>> {
        return searchRepository.getRecentSuggestions(limit)
    }
}

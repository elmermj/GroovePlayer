package com.aethelsoft.grooveplayer.domain.usecase.search_category

import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import javax.inject.Inject

class SearchSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(query: String): List<Song> {
        return if (query.isBlank()) {
            emptyList()
        } else {
            musicRepository.searchSongs(query)
        }
    }
}

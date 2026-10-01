package com.aethelworks.grooveplayer.domain.usecase.library_category

import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.repository.SongLikeRepository
import javax.inject.Inject

class ToggleSongLikeUseCase @Inject constructor(
    private val songLikeRepository: SongLikeRepository,
) {
    suspend operator fun invoke(song: Song) {
        songLikeRepository.toggle(song)
    }
}

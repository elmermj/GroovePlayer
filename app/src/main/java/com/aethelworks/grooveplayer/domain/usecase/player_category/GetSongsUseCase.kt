package com.aethelworks.grooveplayer.domain.usecase.player_category

import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.repository.MusicRepository
import javax.inject.Inject

class GetSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(): List<Song> {
        return musicRepository.getAllSongs()
    }
}


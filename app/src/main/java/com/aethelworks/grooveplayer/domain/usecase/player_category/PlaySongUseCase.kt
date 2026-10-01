package com.aethelworks.grooveplayer.domain.usecase.player_category

import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.repository.PlayerRepository
import javax.inject.Inject

class PlaySongUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend operator fun invoke(song: Song) = repo.playSong(song)
}
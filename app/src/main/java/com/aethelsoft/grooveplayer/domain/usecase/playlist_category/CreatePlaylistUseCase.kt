package com.aethelsoft.grooveplayer.domain.usecase.playlist_category

import com.aethelsoft.grooveplayer.domain.repository.PlaylistRepository
import javax.inject.Inject

class CreatePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(name: String): Long = playlistRepository.create(name)
}

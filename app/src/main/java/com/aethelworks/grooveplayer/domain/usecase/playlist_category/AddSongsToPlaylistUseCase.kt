package com.aethelworks.grooveplayer.domain.usecase.playlist_category

import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.repository.PlaylistRepository
import javax.inject.Inject

class AddSongsToPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(playlistId: Long, songs: List<Song>) {
        playlistRepository.addSongs(playlistId, songs)
    }
}

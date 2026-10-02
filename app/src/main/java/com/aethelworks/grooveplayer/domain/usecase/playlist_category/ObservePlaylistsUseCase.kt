package com.aethelworks.grooveplayer.domain.usecase.playlist_category

import com.aethelworks.grooveplayer.domain.model.Playlist
import com.aethelworks.grooveplayer.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlaylistsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    operator fun invoke(): Flow<List<Playlist>> = playlistRepository.observePlaylists()
}

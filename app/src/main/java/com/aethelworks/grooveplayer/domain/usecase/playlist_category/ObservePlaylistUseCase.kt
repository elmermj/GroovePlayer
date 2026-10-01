package com.aethelworks.grooveplayer.domain.usecase.playlist_category

import com.aethelworks.grooveplayer.domain.model.PlaylistWithTracks
import com.aethelworks.grooveplayer.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    operator fun invoke(id: Long): Flow<PlaylistWithTracks?> = playlistRepository.observePlaylist(id)
}

package com.aethelsoft.grooveplayer.domain.usecase.playlist_category

import com.aethelsoft.grooveplayer.domain.playlist.PlaylistOrder
import com.aethelsoft.grooveplayer.domain.repository.PlaylistRepository
import javax.inject.Inject

class ReorderPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(playlistId: Long, fromIndex: Int, toIndex: Int) {
        val playlist = playlistRepository.getPlaylist(playlistId) ?: return
        val moved = PlaylistOrder.move(playlist.tracks, fromIndex, toIndex)
        if (moved.map { it.entryId } == playlist.tracks.map { it.entryId }) return
        playlistRepository.reorder(playlistId, moved.map { it.entryId })
    }
}

package com.aethelsoft.grooveplayer.domain.usecase.playlist_category

import com.aethelsoft.grooveplayer.domain.playlist.M3uCodec
import com.aethelsoft.grooveplayer.domain.playlist.PlaylistLibraryPaths
import com.aethelsoft.grooveplayer.domain.repository.PlaylistRepository
import javax.inject.Inject

class ExportM3uPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(playlistId: Long): String? {
        val playlist = playlistRepository.getPlaylist(playlistId) ?: return null
        val tracks = PlaylistLibraryPaths.toM3uTracks(playlist.tracks.map { it.song })
        return M3uCodec.serialize(playlist.playlist.name, tracks)
    }
}

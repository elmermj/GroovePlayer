package com.aethelsoft.grooveplayer.domain.usecase.playlist_category

import com.aethelsoft.grooveplayer.domain.model.M3uTrack
import com.aethelsoft.grooveplayer.domain.playlist.M3uCodec
import com.aethelsoft.grooveplayer.domain.playlist.PlaylistM3uMatch
import com.aethelsoft.grooveplayer.domain.repository.PlaylistRepository
import javax.inject.Inject

class ExportM3uPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(playlistId: Long): String? {
        val playlist = playlistRepository.getPlaylist(playlistId) ?: return null
        val tracks = playlist.tracks.mapNotNull { track ->
            if (!track.available) return@mapNotNull null
            val name = PlaylistM3uMatch.exportFileName(track.song)
            if (name.isEmpty()) return@mapNotNull null
            val artist = track.song.artist.trim()
            val title = track.song.title.trim()
            val label = when {
                artist.isEmpty() -> title
                title.isEmpty() -> artist
                else -> "$artist - $title"
            }
            M3uTrack(
                location = name,
                title = label,
                durationSeconds = if (track.song.durationMs > 0) track.song.durationMs / 1000 else -1L,
            )
        }
        return M3uCodec.serialize(playlist.playlist.name, tracks)
    }
}

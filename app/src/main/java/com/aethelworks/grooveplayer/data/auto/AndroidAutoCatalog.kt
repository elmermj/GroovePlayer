package com.aethelworks.grooveplayer.data.auto

import com.aethelworks.grooveplayer.domain.auto.AutoLibrarySnapshot
import com.aethelworks.grooveplayer.domain.library.SongLikeIndex
import com.aethelworks.grooveplayer.domain.repository.MusicRepository
import com.aethelworks.grooveplayer.domain.repository.PlaybackHistoryRepository
import com.aethelworks.grooveplayer.domain.repository.PlaylistRepository
import com.aethelworks.grooveplayer.domain.repository.SongLikeRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the Android Auto tree from the private library.
 * [MusicRepository] lists groove-library audio only. MediaStore is not queried.
 */
@Singleton
class AndroidAutoCatalog @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val playbackHistoryRepository: PlaybackHistoryRepository,
    private val songLikeRepository: SongLikeRepository,
) {
    suspend fun snapshot(): AutoLibrarySnapshot {
        val playlists = playlistRepository.observePlaylists().first()
        return AutoLibrarySnapshot(
            songs = musicRepository.getAllSongs(),
            playlists = playlists.mapNotNull { playlistRepository.getPlaylist(it.id) },
            recentlyPlayed = playbackHistoryRepository.getRecentlyPlayed(LIST_LIMIT).first(),
            mostPlayed = playbackHistoryRepository.getMostPlayed(LIST_LIMIT).first(),
            favoriteTracks = songLikeRepository.observeFavoriteTracks(SongLikeIndex.ALL).first(),
            favoriteArtists = songLikeRepository.observeFavoriteArtists(SongLikeIndex.ALL).first(),
            favoriteAlbums = songLikeRepository.observeFavoriteAlbums(SongLikeIndex.ALL).first(),
        )
    }

    private companion object {
        const val LIST_LIMIT = 50
    }
}

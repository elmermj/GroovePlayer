package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.domain.model.FavoriteAlbum
import com.aethelsoft.grooveplayer.domain.model.FavoriteArtist
import com.aethelsoft.grooveplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for playback history operations.
 * All methods return Flows for reactive updates.
 */
interface PlaybackHistoryRepository {
    suspend fun recordPlayback(song: Song)
    
    // Reactive Flows - update in real-time
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<Song>>
    fun getFavoriteTracks(sinceTimestamp: Long, limit: Int = 50): Flow<List<Song>>
    fun getFavoriteArtists(sinceTimestamp: Long, limit: Int = 50): Flow<List<FavoriteArtist>>
    fun getFavoriteAlbums(sinceTimestamp: Long, limit: Int = 50): Flow<List<FavoriteAlbum>>
    fun getLastPlayedSongs(sinceTimestamp: Long, limit: Int = 8): Flow<List<Song>>
}


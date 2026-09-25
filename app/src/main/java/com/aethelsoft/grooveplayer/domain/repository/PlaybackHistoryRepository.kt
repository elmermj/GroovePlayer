package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.domain.model.MostPlayedTrack
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
    fun getLastPlayedSongs(sinceTimestamp: Long, limit: Int = 8): Flow<List<Song>>

    /** All-time play counts from local playback history, highest first. */
    fun getMostPlayed(limit: Int = 50): Flow<List<MostPlayedTrack>>
}


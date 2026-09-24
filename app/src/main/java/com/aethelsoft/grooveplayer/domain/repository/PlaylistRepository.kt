package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.domain.model.Playlist
import com.aethelsoft.grooveplayer.domain.model.PlaylistWithTracks
import com.aethelsoft.grooveplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    fun observePlaylist(id: Long): Flow<PlaylistWithTracks?>
    suspend fun getPlaylist(id: Long): PlaylistWithTracks?
    suspend fun create(name: String): Long
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)
    suspend fun addSongs(playlistId: Long, songs: List<Song>)
    suspend fun removeTrack(playlistId: Long, entryId: Long)
    suspend fun reorder(playlistId: Long, orderedEntryIds: List<Long>)
}

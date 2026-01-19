package com.aethelsoft.grooveplayer.data.repository

import com.aethelsoft.grooveplayer.data.local.db.dao.PlaybackHistoryDao
import com.aethelsoft.grooveplayer.data.local.db.entity.PlaybackHistoryEntity
import com.aethelsoft.grooveplayer.domain.model.FavoriteAlbum
import com.aethelsoft.grooveplayer.domain.model.FavoriteArtist
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackHistoryRepositoryImpl @Inject constructor(
    private val dao: PlaybackHistoryDao
) : PlaybackHistoryRepository {
    
    override suspend fun recordPlayback(song: Song) {
        val entity = PlaybackHistoryEntity(
            songId = song.id,
            songTitle = song.title,
            artist = song.artist,
            album = song.album ?: "Unknown Album",
            genre = song.genre.ifEmpty { "Unknown Genre" },
            uri = song.uri,
            artworkUrl = song.artworkUrl,
            playedAt = System.currentTimeMillis()
        )
        try {
            val rowId = dao.insertPlayback(entity)
            android.util.Log.d("PlaybackHistoryRepo", "Recorded playback: ${song.title}, rowId=$rowId, timestamp=${entity.playedAt}")
        } catch (e: Exception) {
            android.util.Log.e("PlaybackHistoryRepo", "Failed to insert playback", e)
            throw e
        }
    }
    
    override fun getRecentlyPlayed(limit: Int): Flow<List<Song>> {
        return dao.getRecentlyPlayed(limit).map { entities ->
            android.util.Log.d("PlaybackHistoryRepo", "getRecentlyPlayed: Found ${entities.size} entries")
            entities.map { it.toDomain() }
        }
    }
    
    override fun getFavoriteTracks(sinceTimestamp: Long, limit: Int): Flow<List<Song>> {
        return dao.getFavoriteTracks(sinceTimestamp, limit).map { results ->
            android.util.Log.d("PlaybackHistoryRepo", "getFavoriteTracks: Found ${results.size} entries (timestamp=$sinceTimestamp)")
            results.map { result ->
                Song(
                    id = result.songId,
                    title = result.songTitle,
                    artist = result.artist,
                    uri = result.uri,
                    genre = result.genre,
                    durationMs = 0L,
                    artworkUrl = result.artworkUrl,
                    album = result.album
                )
            }
        }
    }
    
    override fun getFavoriteArtists(sinceTimestamp: Long, limit: Int): Flow<List<FavoriteArtist>> {
        return dao.getFavoriteArtists(sinceTimestamp, limit).map { results ->
            results.map { result ->
                FavoriteArtist(
                    artist = result.artist,
                    playCount = result.playCount
                )
            }
        }
    }
    
    override fun getFavoriteAlbums(sinceTimestamp: Long, limit: Int): Flow<List<FavoriteAlbum>> {
        return dao.getFavoriteAlbums(sinceTimestamp, limit).map { results ->
            results.map { result ->
                FavoriteAlbum(
                    album = result.album,
                    artist = result.artist,
                    playCount = result.playCount
                )
            }
        }
    }

    override fun getLastPlayedSongs(sinceTimestamp: Long, limit: Int): Flow<List<Song>> {
        return dao.getLastPlayedSongs(sinceTimestamp, limit).map { entities ->
            android.util.Log.d("PlaybackHistoryRepo", "getLastPlayedSongs: Found ${entities.size} entries (timestamp=$sinceTimestamp)")
            entities.forEach { 
                android.util.Log.d("PlaybackHistoryRepo", "  - ${it.songTitle} at ${it.playedAt}")
            }
            entities.map { entity ->
                entity.toDomain()
            }
        }
    }
    
    private fun PlaybackHistoryEntity.toDomain(): Song {
        return Song(
            id = songId,
            title = songTitle,
            artist = artist,
            uri = uri,
            genre = genre,
            durationMs = 0L,
            artworkUrl = artworkUrl,
            album = album
        )
    }
}


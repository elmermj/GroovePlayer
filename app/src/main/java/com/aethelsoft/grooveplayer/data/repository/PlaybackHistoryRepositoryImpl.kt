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
import kotlin.collections.map

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
        dao.insertPlayback(entity)
    }
    
    override fun getRecentlyPlayed(limit: Int): Flow<List<Song>> {
        return dao.getRecentlyPlayed(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getFavoriteTracks(sinceTimestamp: Long, limit: Int): List<Song> {
        return dao.getFavoriteTracks(sinceTimestamp, limit).map { result ->
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
    
    override suspend fun getFavoriteArtists(sinceTimestamp: Long, limit: Int): List<FavoriteArtist> {
        return dao.getFavoriteArtists(sinceTimestamp, limit).map { result ->
            FavoriteArtist(
                artist = result.artist,
                playCount = result.playCount
            )
        }
    }
    
    override suspend fun getFavoriteAlbums(sinceTimestamp: Long, limit: Int): List<FavoriteAlbum> {
        return dao.getFavoriteAlbums(sinceTimestamp, limit).map { result ->
            FavoriteAlbum(
                album = result.album,
                artist = result.artist,
                playCount = result.playCount
            )
        }
    }

    override suspend fun getLastPlayedSongs(sinceTimestamp: Long, limit: Int): List<Song> {
        return dao.getLastPlayedSongs(sinceTimestamp, limit).map { entity ->
            entity.toDomain()
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


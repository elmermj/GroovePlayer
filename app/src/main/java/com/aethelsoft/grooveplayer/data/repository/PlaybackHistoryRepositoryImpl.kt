package com.aethelsoft.grooveplayer.data.repository

import com.aethelsoft.grooveplayer.data.local.db.dao.AlbumDao
import com.aethelsoft.grooveplayer.data.local.db.dao.ArtistDao
import com.aethelsoft.grooveplayer.data.local.db.dao.PlaybackHistoryDao
import com.aethelsoft.grooveplayer.data.local.db.entity.AlbumEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.ArtistEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.PlaybackHistoryEntity
import com.aethelsoft.grooveplayer.domain.model.Album
import com.aethelsoft.grooveplayer.domain.model.FavoriteAlbum
import com.aethelsoft.grooveplayer.domain.model.FavoriteArtist
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.makeAlbumId
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import com.aethelsoft.grooveplayer.utils.ArtistParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackHistoryRepositoryImpl @Inject constructor(
    private val dao: PlaybackHistoryDao,
    private val artistDao: ArtistDao,
    private val albumDao: AlbumDao
) : PlaybackHistoryRepository {
    
    override suspend fun recordPlayback(song: Song) {
        // Parse multiple artists from the artist string
        val artists = ArtistParser.parseArtists(song.artist)
        
        // Determine album name - if missing, create "Single - {song title}"
        val albumName = song.album?.name?.takeIf {
            it.isNotBlank() && it != "Unknown Album"
        } ?: "Single - ${song.title}"
        
        // Create/update artist entities for all artists
        artists.forEach { artistName ->
            val existing = artistDao.getArtist(artistName)
            if (existing == null) {
                artistDao.insertOrUpdate(ArtistEntity(name = artistName))
            }
        }
        
        // Create/update album entity - link to primary artist
        val primaryArtist = artists.firstOrNull() ?: "Unknown Artist"
        val existingAlbum = albumDao.getAlbum(albumName, primaryArtist)
        if (existingAlbum == null) {
            albumDao.insertOrUpdate(
                AlbumEntity(
                    name = albumName,
                    artist = primaryArtist,
                    artworkUrl = song.artworkUrl
                )
            )
        } else if (existingAlbum.artworkUrl == null && song.artworkUrl != null) {
            // Update artwork if missing
            albumDao.insertOrUpdate(
                existingAlbum.copy(artworkUrl = song.artworkUrl)
            )
        }
        
        // Record playback with processed album name
        val entity = PlaybackHistoryEntity(
            songId = song.id,
            songTitle = song.title,
            artist = song.artist, // Keep original artist string for display
            album = albumName,
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
                    album = result.album?.let { albumName ->
                        Album(
                            id = albumName,
                            name = albumName,
                            artist = result.artist,
                            artworkUrl = result.artworkUrl,
                            songs = emptyList(),
                            year = null
                        )
                    }
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
                val primaryArtist = ArtistParser.parseArtists(result.artist).firstOrNull() ?: result.artist
                FavoriteAlbum(
                    album = result.album,
                    artist = primaryArtist,
                    playCount = result.playCount,
                    artworkUrl = result.artworkUrl
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
            album = album?.let { albumName ->
                Album(
                    id = makeAlbumId(artist, albumName),
                    name = albumName,
                    artist = artist,
                    artworkUrl = artworkUrl,
                    songs = emptyList(),
                    year = null
                )
            }
        )
    }
}


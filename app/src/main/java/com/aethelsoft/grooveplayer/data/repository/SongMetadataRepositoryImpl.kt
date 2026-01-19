package com.aethelsoft.grooveplayer.data.repository

import com.aethelsoft.grooveplayer.data.local.db.dao.AlbumDao
import com.aethelsoft.grooveplayer.data.local.db.dao.ArtistDao
import com.aethelsoft.grooveplayer.data.local.db.dao.PlaybackHistoryDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SongMetadataDao
import com.aethelsoft.grooveplayer.data.local.db.entity.AlbumEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.ArtistEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.SongMetadataEntity
import com.aethelsoft.grooveplayer.domain.repository.AlbumMetadata
import com.aethelsoft.grooveplayer.domain.repository.ArtistMetadata
import com.aethelsoft.grooveplayer.domain.repository.SongMetadata
import com.aethelsoft.grooveplayer.domain.repository.SongMetadataRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongMetadataRepositoryImpl @Inject constructor(
    private val songMetadataDao: SongMetadataDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val artistDao: ArtistDao,
    private val albumDao: AlbumDao
) : SongMetadataRepository {
    
    override suspend fun getMetadata(songId: String): SongMetadata? {
        val entity = songMetadataDao.getMetadata(songId) ?: return null
        return entity.toDomain()
    }
    
    override suspend fun saveMetadata(metadata: SongMetadata) {
        val entity = SongMetadataEntity(
            songId = metadata.songId,
            title = metadata.title,
            genres = metadata.genres.joinToString(","),
            artists = metadata.artists.joinToString(","),
            album = metadata.album,
            year = metadata.year,
            useAlbumYear = metadata.useAlbumYear
        )
        songMetadataDao.insertOrUpdate(entity)
        
        // Also update artist and album entities
        metadata.artists.forEach { artistName ->
            val existing = artistDao.getArtist(artistName)
            if (existing == null) {
                artistDao.insertOrUpdate(ArtistEntity(name = artistName))
            }
        }
        
        metadata.album?.let { albumName ->
            val existing = albumDao.getAlbum(albumName, metadata.artists.firstOrNull() ?: "")
            if (existing == null) {
                albumDao.insertOrUpdate(AlbumEntity(
                    name = albumName,
                    artist = metadata.artists.firstOrNull() ?: ""
                ))
            }
        }
    }
    
    override suspend fun searchGenres(query: String): List<String> {
        return playbackHistoryDao.searchGenres(query)
    }
    
    override suspend fun getAllGenres(): List<String> {
        return playbackHistoryDao.getAllGenres()
    }
    
    override suspend fun searchArtists(query: String): List<String> {
        val fromHistory = playbackHistoryDao.searchArtistsFromHistory(query)
        val fromArtists = artistDao.searchArtists(query).map { it.name }
        return (fromHistory + fromArtists).distinct().sorted()
    }
    
    override suspend fun getAllArtists(): List<String> {
        val fromHistory = playbackHistoryDao.searchArtistsFromHistory("")
        val fromArtists = artistDao.getAllArtistNames()
        return (fromHistory + fromArtists).distinct().sorted()
    }
    
    override suspend fun searchAlbums(query: String): List<String> {
        val fromHistory = albumDao.searchAlbumsFromHistory(query)
        val fromAlbums = albumDao.searchAlbums(query).map { it.name }
        return (fromHistory + fromAlbums).distinct().sorted()
    }
    
    override suspend fun getAllAlbums(): List<String> {
        val fromHistory = albumDao.searchAlbumsFromHistory("")
        val fromAlbums = albumDao.getAllAlbumNames()
        return (fromHistory + fromAlbums).distinct().sorted()
    }
    
    override suspend fun getAlbum(albumName: String, artistName: String): AlbumMetadata? {
        val entity = albumDao.getAlbum(albumName, artistName) ?: return null
        return AlbumMetadata(
            name = entity.name,
            artist = entity.artist,
            artworkUrl = entity.artworkUrl,
            year = entity.year
        )
    }
    
    override suspend fun saveAlbum(album: AlbumMetadata) {
        val entity = AlbumEntity(
            name = album.name,
            artist = album.artist,
            artworkUrl = album.artworkUrl,
            year = album.year
        )
        albumDao.insertOrUpdate(entity)
    }
    
    override suspend fun getArtist(artistName: String): ArtistMetadata? {
        val entity = artistDao.getArtist(artistName) ?: return null
        return ArtistMetadata(
            name = entity.name,
            imageUrl = entity.imageUrl
        )
    }
    
    override suspend fun saveArtist(artist: ArtistMetadata) {
        val entity = ArtistEntity(
            name = artist.name,
            imageUrl = artist.imageUrl
        )
        artistDao.insertOrUpdate(entity)
    }
    
    private fun SongMetadataEntity.toDomain(): SongMetadata {
        return SongMetadata(
            songId = songId,
            title = title,
            genres = genres.split(",").filter { it.isNotBlank() },
            artists = artists.split(",").filter { it.isNotBlank() },
            album = album,
            year = year,
            useAlbumYear = useAlbumYear
        )
    }
}


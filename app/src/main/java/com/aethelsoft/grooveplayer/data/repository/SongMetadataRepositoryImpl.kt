package com.aethelsoft.grooveplayer.data.repository

import com.aethelsoft.grooveplayer.data.local.db.dao.AlbumDao
import com.aethelsoft.grooveplayer.data.local.db.dao.ArtistDao
import com.aethelsoft.grooveplayer.data.local.db.dao.GenreDao
import com.aethelsoft.grooveplayer.data.local.db.dao.PlaybackHistoryDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SongMetadataDao
import com.aethelsoft.grooveplayer.data.local.db.entity.AlbumArtistCrossRef
import com.aethelsoft.grooveplayer.data.local.db.entity.AlbumEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.ArtistEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.GenreEntity
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
    private val albumDao: AlbumDao,
    private val genreDao: GenreDao
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
        
        // Also update artist, album, and genre entities

        // Ensure all artists are created
        metadata.artists.forEach { artistName ->
            val existing = artistDao.getArtist(artistName)
            if (existing == null) {
                artistDao.insertOrUpdate(ArtistEntity(name = artistName))
            }
        }

        // Determine album name - if missing, create "Single - {song title}"
        val albumName = metadata.album?.takeIf {
            it.isNotBlank() && it != "Unknown Album"
        } ?: "Single - ${metadata.title}"

        // Create/update album entity - link to all artists for this album
        val primaryArtist = metadata.artists.firstOrNull() ?: "Unknown Artist"
        val existingAlbum = albumDao.getAlbum(albumName, primaryArtist) ?: albumDao.getAlbumByName(albumName)
        val albumEntity = if (existingAlbum == null) {
            val newAlbum = AlbumEntity(
                name = albumName
            )
            albumDao.insertOrUpdate(newAlbum)
            // Re-query to obtain generated ID (name is unique enough for our purposes here)
            albumDao.getAlbumByName(albumName) ?: newAlbum
        } else {
            existingAlbum
        }

        // Link album to all involved artists
        metadata.artists.forEach { artistName ->
            val artistEntity = artistDao.getArtist(artistName) ?: return@forEach
            albumDao.insertAlbumArtistCrossRef(
                AlbumArtistCrossRef(
                    albumId = albumEntity.albumId,
                    artistId = artistEntity.artistId
                )
            )
        }

        // Ensure all genres are created
        metadata.genres.forEach { genreName ->
            val trimmed = genreName.trim()
            if (trimmed.isEmpty()) return@forEach
            val existing = genreDao.getByName(trimmed)
            if (existing == null) {
                genreDao.insertOrUpdate(GenreEntity(name = trimmed))
            }
        }
    }
    
    override suspend fun searchGenres(query: String): List<String> {
        val fromHistory = playbackHistoryDao.searchGenres(query)
        val fromGenres = genreDao.searchGenres(query)
        return (fromHistory + fromGenres).distinct().sorted()
    }
    
    override suspend fun getAllGenres(): List<String> {
        val fromHistory = playbackHistoryDao.getAllGenres()
        val fromGenres = genreDao.getAllGenres()
        return (fromHistory + fromGenres).distinct().sorted()
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
            artist = artistName,
            artworkUrl = entity.artworkUrl,
            year = entity.year
        )
    }
    
    override suspend fun saveAlbum(album: AlbumMetadata) {
        // Ensure artist exists
        val existingArtist = artistDao.getArtist(album.artist)
        if (existingArtist == null) {
            artistDao.insertOrUpdate(ArtistEntity(name = album.artist))
        }
        val artistEntity = artistDao.getArtist(album.artist)
            ?: ArtistEntity(name = album.artist)

        // Create or update album
        val existingAlbum = albumDao.getAlbum(album.name, album.artist) ?: albumDao.getAlbumByName(album.name)
        val albumEntity = existingAlbum?.copy(
            artworkUrl = album.artworkUrl ?: existingAlbum.artworkUrl,
            year = album.year ?: existingAlbum.year
        ) ?: AlbumEntity(
            name = album.name,
            artworkUrl = album.artworkUrl,
            year = album.year
        )
        albumDao.insertOrUpdate(albumEntity)

        // Re-fetch album with proper ID and link to artist
        val finalAlbum = albumDao.getAlbum(album.name, album.artist) ?: albumDao.getAlbumByName(album.name) ?: albumEntity
        albumDao.insertAlbumArtistCrossRef(
            AlbumArtistCrossRef(
                albumId = finalAlbum.albumId,
                artistId = artistEntity.artistId
            )
        )
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
    
    override suspend fun getLatestAlbumByArtist(artistName: String): AlbumMetadata? {
        val entity = albumDao.getLatestAlbumByArtist(artistName) ?: return null
        return AlbumMetadata(
            name = entity.name,
            artist = artistName,
            artworkUrl = entity.artworkUrl,
            year = entity.year
        )
    }
    
    private fun SongMetadataEntity.toDomain(): SongMetadata =
        SongMetadata(
            songId = songId,
            title = title,
            genres = genres.split(",").filter { it.isNotBlank() },
            artists = artists.split(",").filter { it.isNotBlank() },
            album = album,
            year = year,
            useAlbumYear = useAlbumYear
        )

}


package com.aethelsoft.grooveplayer.domain.repository

interface SongMetadataRepository {
    suspend fun getMetadata(songId: String): SongMetadata?
    suspend fun saveMetadata(metadata: SongMetadata)
    suspend fun searchGenres(query: String): List<String>
    suspend fun getAllGenres(): List<String>
    suspend fun searchArtists(query: String): List<String>
    suspend fun getAllArtists(): List<String>
    suspend fun searchAlbums(query: String): List<String>
    suspend fun getAllAlbums(): List<String>
    suspend fun getAlbum(albumName: String, artistName: String): AlbumMetadata?
    suspend fun saveAlbum(album: AlbumMetadata)
    suspend fun getArtist(artistName: String): ArtistMetadata?
    suspend fun saveArtist(artist: ArtistMetadata)
}

data class SongMetadata(
    val songId: String,
    val title: String,
    val genres: List<String>,
    val artists: List<String>,
    val album: String?,
    val year: Int?,
    val useAlbumYear: Boolean
)

data class AlbumMetadata(
    val name: String,
    val artist: String,
    val artworkUrl: String?,
    val year: Int?
)

data class ArtistMetadata(
    val name: String,
    val imageUrl: String?
)


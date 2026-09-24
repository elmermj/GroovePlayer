package com.aethelsoft.grooveplayer.domain.usecase.home_category

import com.aethelsoft.grooveplayer.data.local.db.dao.AlbumDao
import com.aethelsoft.grooveplayer.data.local.db.dao.ArtistDao
import com.aethelsoft.grooveplayer.data.local.db.dao.GenreDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SongDao
import com.aethelsoft.grooveplayer.data.local.db.entity.AlbumArtistCrossRef
import com.aethelsoft.grooveplayer.data.local.db.entity.AlbumEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.ArtistEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.GenreEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.SongArtistCrossRef
import com.aethelsoft.grooveplayer.data.local.db.entity.SongEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.SongGenreCrossRef
import com.aethelsoft.grooveplayer.domain.library.LibraryGenreIndex
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import com.aethelsoft.grooveplayer.utils.ArtistParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Initializes the normalized artist/album/song/genre index from MediaStore on app start.
 * Idempotent and optimized with in-memory caches to avoid duplicate work.
 */
class InitializeLibraryIndexUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val artistDao: ArtistDao,
    private val albumDao: AlbumDao,
    private val genreDao: GenreDao,
    private val songDao: SongDao
) {

    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        // In-memory caches to minimize DB lookups
        val artistCache = mutableMapOf<String, Long>()              // name -> artistId
        val albumCache = mutableMapOf<Pair<String, String>, Long>() // (albumName, primaryArtist) -> albumId
        val genreIdCache = mutableMapOf<String, Long>()             // lowercase name -> genreId

        val songs = musicRepository.getAllSongs()

        for (song in songs) {
            val artistNames = ArtistParser.parseArtists(song.artist)
            val primaryArtist = ArtistParser.getPrimaryArtist(song.artist)
            val albumName = song.album?.name?.takeIf { it.isNotBlank() && it != "Unknown Album" }
                ?: "Single - ${song.title}"

            // Ensure artists exist
            val artistIds = mutableListOf<Long>()
            for (name in artistNames) {
                val cachedId = artistCache[name]
                if (cachedId != null) {
                    artistIds.add(cachedId)
                    continue
                }
                val existing = artistDao.getArtist(name)
                val ensured = if (existing == null) {
                    artistDao.insertOrUpdate(ArtistEntity(name = name))
                    artistDao.getArtist(name) ?: ArtistEntity(name = name)
                } else {
                    existing
                }
                artistCache[name] = ensured.artistId
                artistIds.add(ensured.artistId)
            }

            // Ensure album exists
            val albumKey = albumName to primaryArtist
            val albumId = albumCache[albumKey] ?: run {
                val existingAlbum = albumDao.getAlbum(albumName, primaryArtist)
                    ?: albumDao.getAlbumByName(albumName)
                val ensured = if (existingAlbum == null) {
                    val newAlbum = AlbumEntity(
                        name = albumName,
                        artworkUrl = song.artworkUrl,
                        year = song.year
                    )
                    albumDao.insertOrUpdate(newAlbum)
                    albumDao.getAlbumByName(albumName) ?: newAlbum
                } else {
                    existingAlbum
                }
                albumCache[albumKey] = ensured.albumId
                ensured.albumId
            }

            // Link album to all artists
            artistIds.forEach { artistId ->
                albumDao.insertAlbumArtistCrossRef(
                    AlbumArtistCrossRef(
                        albumId = albumId,
                        artistId = artistId
                    )
                )
            }

            // Ensure SongEntity + song-artist links
            val songEntity = SongEntity(
                songId = song.id,
                albumId = albumId,
                uri = song.uri,
                title = song.title,
                trackNumber = null,
                durationMs = song.durationMs
            )
            songDao.insertOrUpdate(songEntity)

            artistIds.forEach { artistId ->
                songDao.insertSongArtistCrossRef(
                    SongArtistCrossRef(
                        songId = song.id,
                        artistId = artistId
                    )
                )
            }

            // Link every genre tag on the song. Cross-refs require the song row.
            for (genreName in LibraryGenreIndex.namesFor(song)) {
                val genreId = ensureGenreId(genreName, genreIdCache) ?: continue
                songDao.insertSongGenreCrossRef(
                    SongGenreCrossRef(
                        songId = song.id,
                        genreId = genreId
                    )
                )
            }
        }
    }

    private suspend fun ensureGenreId(
        genreName: String,
        genreIdCache: MutableMap<String, Long>,
    ): Long? {
        val cacheKey = genreName.lowercase()
        genreIdCache[cacheKey]?.let { return it }
        val existing = genreDao.getByName(genreName)
        val ensured = if (existing == null) {
            genreDao.insertOrUpdate(GenreEntity(name = genreName))
            genreDao.getByName(genreName)
        } else {
            existing
        }
        val id = ensured?.genreId ?: return null
        genreIdCache[cacheKey] = id
        return id
    }
}


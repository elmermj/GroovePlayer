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
import com.aethelsoft.grooveplayer.domain.artwork.EmbeddedArtworkKeys
import com.aethelsoft.grooveplayer.domain.backup.AppLibraryPaths
import com.aethelsoft.grooveplayer.domain.library.LibraryGenreIndex
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import com.aethelsoft.grooveplayer.domain.repository.SongMetadataRepository
import com.aethelsoft.grooveplayer.utils.ArtistParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
    private val songDao: SongDao,
    private val songMetadataRepository: SongMetadataRepository,
    private val appLibraryPaths: AppLibraryPaths,
) {

    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        // In-memory caches to minimize DB lookups
        val artistCache = mutableMapOf<String, Long>()              // name -> artistId
        val albumCache = mutableMapOf<Pair<String, String>, Long>() // (albumName, primaryArtist) -> albumId
        val genreIdCache = mutableMapOf<String, Long>()             // lowercase name -> genreId

        val songs = musicRepository.getAllSongs()
        val editedGenresBySongId = songMetadataRepository.getAllMetadata()
            .associate { it.songId to it.genres }

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
                val artwork = EmbeddedArtworkKeys.prefer(ensured.artworkUrl, song.artworkUrl)
                val row = if (artwork == ensured.artworkUrl) {
                    ensured
                } else {
                    ensured.copy(artworkUrl = artwork).also { albumDao.insertOrUpdate(it) }
                }
                albumCache[albumKey] = row.albumId
                row.albumId
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
            val existing = songDao.getSong(song.id)
            val existingPath = existing?.sourcePath
            val songEntity = SongEntity(
                songId = song.id,
                albumId = albumId,
                uri = song.uri,
                title = song.title,
                trackNumber = null,
                durationMs = song.durationMs,
                sourcePath = if (keepsAppLibraryFile(existingPath)) existingPath else song.filePath,
                contentHash = existing?.contentHash,
                inPrivateLibrary = true,
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

            // Replace links with the same tags genre browse uses, including edits.
            songDao.deleteSongGenreCrossRefs(song.id)
            for (genreName in LibraryGenreIndex.namesFor(song, editedGenresBySongId)) {
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

    private fun keepsAppLibraryFile(path: String?): Boolean {
        if (path.isNullOrBlank() || !appLibraryPaths.isInside(path)) return false
        val file = File(path)
        return file.isFile && file.length() > 0L
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


package com.aethelsoft.grooveplayer.data.local.mediastore

import com.aethelsoft.grooveplayer.data.library.PrivateLibraryCatalog
import com.aethelsoft.grooveplayer.domain.model.FolderSizeEntry
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.StorageUsageData
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Library catalog. Only audio files in the app-private groove-library are listed.
 * MediaStore is not queried and device folders are not scanned.
 */
@Singleton
class MediaStoreRepository @Inject constructor(
    private val privateLibrary: PrivateLibraryCatalog,
) : MusicRepository {

    private val _catalogGeneration = MutableStateFlow(0L)
    override val catalogGeneration: StateFlow<Long> = _catalogGeneration.asStateFlow()

    override fun bumpCatalogGeneration() {
        _catalogGeneration.update { it + 1 }
    }

    override suspend fun getMusicFolderPaths(): List<String> = emptyList()

    override suspend fun getStorageUsage(): StorageUsageData = withContext(Dispatchers.IO) {
        privateLibrary.storageUsage()
    }

    override suspend fun getAllSongs(): List<Song> = privateLibrary.songs()

    override suspend fun getSongsPage(offset: Int, limit: Int): List<Song> =
        page(privateLibrary.songs(), offset, limit)

    override suspend fun getSongsByArtist(artist: String): List<Song> =
        privateLibrary.songs().filter { it.artist.equals(artist, ignoreCase = true) }

    override suspend fun getSongsByArtistPage(artist: String, offset: Int, limit: Int): List<Song> =
        page(getSongsByArtist(artist), offset, limit)

    override suspend fun getSongsByAlbum(album: String): List<Song> =
        privateLibrary.songs().filter { it.album?.name.equals(album, ignoreCase = true) }

    override suspend fun getSongsByAlbumPage(album: String, offset: Int, limit: Int): List<Song> =
        page(getSongsByAlbum(album), offset, limit)

    override suspend fun searchSongs(query: String): List<Song> {
        val lower = query.lowercase()
        return privateLibrary.songs().filter { song ->
            song.title.lowercase().contains(lower) ||
                song.artist.lowercase().contains(lower) ||
                song.album?.name?.lowercase()?.contains(lower) == true
        }
    }

    private fun page(songs: List<Song>, offset: Int, limit: Int): List<Song> {
        if (limit <= 0 || offset < 0 || offset >= songs.size) return emptyList()
        return songs.drop(offset).take(limit)
    }
}

fun storageUsageFor(path: String, bytes: Long): StorageUsageData {
    return StorageUsageData(
        totalBytes = bytes,
        includedBytes = bytes,
        excludedBytes = 0L,
        includedFolderDetails = listOf(FolderSizeEntry(path, bytes)),
        excludedFolderDetails = emptyList(),
    )
}

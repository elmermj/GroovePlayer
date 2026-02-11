package com.aethelsoft.grooveplayer.data.local.mediastore

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.aethelsoft.grooveplayer.data.local.mediastore.model.MediaStoreSongData
import com.aethelsoft.grooveplayer.data.mapper.SongMapper
import com.aethelsoft.grooveplayer.domain.model.FolderSizeEntry
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.StorageUsageData
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaStore implementation of MusicRepository.
 * This is the data layer that retrieves songs from Android's MediaStore
 * and maps them to domain models.
 */
@Singleton
class MediaStoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository
) : MusicRepository {

    override suspend fun getMusicFolderPaths(): List<String> = withContext(Dispatchers.IO) {
        fetchMusicFolderPaths()
    }

    override suspend fun getStorageUsage(): StorageUsageData = withContext(Dispatchers.IO) {
        fetchStorageUsage()
    }
    
    override suspend fun getAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songsData = fetchSongsFromMediaStore()
        SongMapper.mediaStoreToDomainList(songsData)
    }

    override suspend fun getSongsPage(offset: Int, limit: Int): List<Song> = withContext(Dispatchers.IO) {
        val songsData = fetchSongsFromMediaStore(offset = offset, limit = limit)
        SongMapper.mediaStoreToDomainList(songsData)
    }

    override suspend fun getSongsByArtist(artist: String): List<Song> = withContext(Dispatchers.IO) {
        val songsData = fetchSongsFromMediaStore(selection = "${MediaStore.Audio.Media.ARTIST}=?", selectionArgs = arrayOf(artist))
        SongMapper.mediaStoreToDomainList(songsData)
    }

    override suspend fun getSongsByArtistPage(artist: String, offset: Int, limit: Int): List<Song> = withContext(Dispatchers.IO) {
        val songsData = fetchSongsFromMediaStore(selection = "${MediaStore.Audio.Media.ARTIST}=?", selectionArgs = arrayOf(artist), offset = offset, limit = limit)
        SongMapper.mediaStoreToDomainList(songsData)
    }

    override suspend fun getSongsByAlbum(album: String): List<Song> = withContext(Dispatchers.IO) {
        val songsData = fetchSongsFromMediaStore(selection = "${MediaStore.Audio.Media.ALBUM}=?", selectionArgs = arrayOf(album))
        SongMapper.mediaStoreToDomainList(songsData)
    }

    override suspend fun getSongsByAlbumPage(album: String, offset: Int, limit: Int): List<Song> = withContext(Dispatchers.IO) {
        val songsData = fetchSongsFromMediaStore(selection = "${MediaStore.Audio.Media.ALBUM}=?", selectionArgs = arrayOf(album), offset = offset, limit = limit)
        SongMapper.mediaStoreToDomainList(songsData)
    }
    
    override suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val songsData = fetchSongsFromMediaStore()
        val lowerQuery = query.lowercase()
        SongMapper.mediaStoreToDomainList(
            songsData.filter { 
                it.title.lowercase().contains(lowerQuery) ||
                it.artist.lowercase().contains(lowerQuery) ||
                it.album?.lowercase()?.contains(lowerQuery) == true
            }
        )
    }
    
    /**
     * Internal method to fetch songs from MediaStore.
     * @param selection optional WHERE clause (without "AND"); base selection is IS_MUSIC != 0
     * @param selectionArgs optional args for selection
     * @param offset number of rows to skip (0 for none)
     * @param limit max rows to return (Int.MAX_VALUE for all)
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun fetchSongsFromMediaStore(
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<MediaStoreSongData> {
        val excludedSet = userRepository.getUserSettings().excludedFolders
            .map { normalizePath(it) }.toSet()
        val songs = mutableListOf<MediaStoreSongData>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.GENRE
        )

        val baseSelection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val fullSelection = if (selection != null) "$baseSelection AND $selection" else baseSelection
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            fullSelection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val genreColumn = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)

            var index = 0
            while (cursor.moveToNext()) {
                if (index < offset) {
                    index++
                    continue
                }
                if (songs.size >= limit) break
                val dataPath = cursor.getString(dataColumn) ?: ""
                val parentPath = if (dataPath.isNotEmpty()) {
                    File(dataPath).parent?.let { normalizePath(it) } ?: ""
                } else ""
                if (parentPath.isNotEmpty() && excludedSet.contains(parentPath)) {
                    index++
                    continue
                }
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val duration = cursor.getLong(durationColumn)
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val albumId = cursor.getLong(albumIdColumn)
                val genre = if (genreColumn >= 0) {
                    cursor.getString(genreColumn) ?: ""
                } else {
                    ""
                }

                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()

                // Get album art URI
                val artworkUri = try {
                    val albumArtUri = android.net.Uri.parse("content://media/external/audio/albumart")
                    ContentUris.withAppendedId(albumArtUri, albumId).toString()
                } catch (e: Exception) {
                    null
                }

                songs.add(
                    MediaStoreSongData(
                        id = id.toString(),
                        title = title,
                        artist = artist,
                        uri = uri,
                        genre = genre,
                        durationMs = duration,
                        artworkUrl = artworkUri,
                        album = album
                    )
                )
                index++
            }
        }

        return songs
    }

    /** Returns distinct folder paths that contain music (parent of MediaStore DATA paths). */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun fetchMusicFolderPaths(): List<String> {
        val folders = mutableSetOf<String>()
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val baseSelection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            baseSelection,
            null,
            null
        )?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn) ?: continue
                if (path.isEmpty()) continue
                File(path).parent?.let { parent ->
                    folders.add(normalizePath(parent))
                }
            }
        }
        return folders.toList().sorted()
    }

    private fun normalizePath(path: String): String =
        path.trim().trimEnd('/', '\\')

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun fetchStorageUsage(): StorageUsageData {
        val excludedSet = userRepository.getUserSettings().excludedFolders
            .map { normalizePath(it) }.toSet()
        val folderSizes = mutableMapOf<String, Long>()
        val projection = arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.SIZE)
        val baseSelection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            baseSelection,
            null,
            null
        )?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn) ?: continue
                val size = cursor.getLong(sizeColumn).coerceAtLeast(0L)
                if (path.isEmpty()) continue
                val parentPath = File(path).parent?.let { normalizePath(it) } ?: continue
                folderSizes[parentPath] = (folderSizes[parentPath] ?: 0L) + size
            }
        }
        var includedBytes = 0L
        var excludedBytes = 0L
        val includedDetails = mutableListOf<FolderSizeEntry>()
        val excludedDetails = mutableListOf<FolderSizeEntry>()
        folderSizes.forEach { (path, bytes) ->
            if (path in excludedSet) {
                excludedBytes += bytes
                excludedDetails.add(FolderSizeEntry(path, bytes))
            } else {
                includedBytes += bytes
                includedDetails.add(FolderSizeEntry(path, bytes))
            }
        }
        val totalBytes = includedBytes + excludedBytes
        return StorageUsageData(
            totalBytes = totalBytes,
            includedBytes = includedBytes,
            excludedBytes = excludedBytes,
            includedFolderDetails = includedDetails.sortedByDescending { it.bytes },
            excludedFolderDetails = excludedDetails.sortedByDescending { it.bytes }
        )
    }
}


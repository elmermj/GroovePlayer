package com.aethelsoft.grooveplayer.data.local.mediastore

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.aethelsoft.grooveplayer.data.local.mediastore.model.MediaStoreSongData
import com.aethelsoft.grooveplayer.data.mapper.SongMapper
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaStore implementation of MusicRepository.
 * This is the data layer that retrieves songs from Android's MediaStore
 * and maps them to domain models.
 */
@Singleton
class MediaStoreRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : MusicRepository {
    
    override suspend fun getAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songsData = fetchSongsFromMediaStore()
        SongMapper.mediaStoreToDomainList(songsData)
    }

    override suspend fun getSongsByArtist(artist: String): List<Song> = withContext(Dispatchers.IO) {
        val songsData = fetchSongsFromMediaStore()
        SongMapper.mediaStoreToDomainList(
            songsData.filter { it.artist.equals(artist, ignoreCase = true) }
        )
    }

    override suspend fun getSongsByAlbum(album: String): List<Song> = withContext(Dispatchers.IO) {
        val songsData = fetchSongsFromMediaStore()
        SongMapper.mediaStoreToDomainList(
            songsData.filter { it.album?.equals(album, ignoreCase = true) == true }
        )
    }
    
    /**
     * Internal method to fetch songs from MediaStore.
     * Returns data layer models (MediaStoreSongData).
     */
    private fun fetchSongsFromMediaStore(): List<MediaStoreSongData> {
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

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val duration = cursor.getLong(durationColumn)
                val data = cursor.getString(dataColumn) ?: ""
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
            }
        }

        return songs
    }
}


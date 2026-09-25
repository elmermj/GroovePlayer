package com.aethelsoft.grooveplayer.data.library

import android.media.MediaMetadataRetriever
import com.aethelsoft.grooveplayer.data.backup.GrooveDownloadsLocator
import com.aethelsoft.grooveplayer.data.local.db.dao.SongDao
import com.aethelsoft.grooveplayer.data.local.mediastore.storageUsageFor
import com.aethelsoft.grooveplayer.domain.artwork.EmbeddedArtworkKeys
import com.aethelsoft.grooveplayer.domain.library.PrivateLibrarySongs
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.StorageUsageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Audio files in the app-private library, listed without MediaStore. */
@Singleton
class PrivateLibraryCatalog @Inject constructor(
    private val grooveDownloads: GrooveDownloadsLocator,
    private val songDao: SongDao,
) {
    suspend fun songs(): List<Song> = withContext(Dispatchers.IO) {
        val root = grooveDownloads.directory()
        val files = root.listFiles() ?: return@withContext emptyList()
        files
            .filter { it.isFile && it.length() > 0L && PrivateLibrarySongs.isListedAudio(it.name) }
            .sortedBy { it.name.lowercase() }
            .map { file ->
                val tags = readTags(file)
                val row = songDao.findBySourcePath(file.absolutePath)
                val base = PrivateLibrarySongs.toSong(
                    absolutePath = file.absolutePath,
                    displayName = file.name,
                    sizeBytes = file.length(),
                    durationMs = tags.durationMs,
                    title = tags.title ?: row?.title ?: file.name.substringBeforeLast('.', file.name),
                    artist = tags.artist ?: PrivateLibrarySongs.ARTIST,
                    albumName = tags.album ?: PrivateLibrarySongs.ALBUM,
                    genre = tags.genre.orEmpty(),
                    artworkUrl = EmbeddedArtworkKeys.uri(row?.contentHash)
                        ?: artworkFor(root, row?.contentHash),
                )
                if (row == null) base else base.copy(id = row.songId)
            }
    }

    fun storageUsage(): StorageUsageData {
        val root = grooveDownloads.directory()
        val bytes = root.listFiles()
            ?.filter { it.isFile && PrivateLibrarySongs.isListedAudio(it.name) }
            ?.sumOf { it.length() }
            ?: 0L
        return storageUsageFor(root.absolutePath, bytes)
    }

    private fun artworkFor(root: File, hash: String?): String? {
        if (hash.isNullOrBlank()) return null
        val file = File(root, ".artwork/${hash.lowercase()}.jpg")
        return if (file.isFile && file.length() > 0L) file.toURI().toString() else null
    }

    private fun readTags(file: File): Tags {
        return try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                Tags(
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?.takeIf { it.isNotBlank() },
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?.takeIf { it.isNotBlank() },
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        ?.takeIf { it.isNotBlank() },
                    genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                        ?.takeIf { it.isNotBlank() },
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L,
                )
            } finally {
                retriever.release()
            }
        } catch (_: Exception) {
            Tags(null, null, null, null, 0L)
        }
    }

    private data class Tags(
        val title: String?,
        val artist: String?,
        val album: String?,
        val genre: String?,
        val durationMs: Long,
    )
}

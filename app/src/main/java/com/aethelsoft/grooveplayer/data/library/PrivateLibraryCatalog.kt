package com.aethelsoft.grooveplayer.data.library

import android.media.MediaMetadataRetriever
import com.aethelsoft.grooveplayer.data.backup.GrooveDownloadsLocator
import com.aethelsoft.grooveplayer.domain.library.PrivateLibrarySongs
import com.aethelsoft.grooveplayer.domain.model.Song
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Audio files in the app-private library, listed without MediaStore. */
@Singleton
class PrivateLibraryCatalog @Inject constructor(
    private val grooveDownloads: GrooveDownloadsLocator,
) {
    fun songs(): List<Song> {
        val root = grooveDownloads.directory()
        val files = root.listFiles() ?: return emptyList()
        return files
            .filter { it.isFile && it.length() > 0L && PrivateLibrarySongs.isListedAudio(it.name) }
            .sortedBy { it.name.lowercase() }
            .map { file ->
                val tags = readTags(file)
                PrivateLibrarySongs.toSong(
                    absolutePath = file.absolutePath,
                    displayName = file.name,
                    sizeBytes = file.length(),
                    durationMs = tags.durationMs,
                    title = tags.title ?: file.name.substringBeforeLast('.', file.name),
                    artist = tags.artist ?: PrivateLibrarySongs.ARTIST,
                )
            }
    }

    private fun readTags(file: File): Tags {
        return try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(file.absolutePath)
                Tags(
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?.takeIf { it.isNotBlank() },
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?.takeIf { it.isNotBlank() },
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L,
                )
            }
        } catch (_: Exception) {
            Tags(null, null, 0L)
        }
    }

    private data class Tags(val title: String?, val artist: String?, val durationMs: Long)
}

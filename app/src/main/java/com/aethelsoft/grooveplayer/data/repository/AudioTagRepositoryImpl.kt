package com.aethelsoft.grooveplayer.data.repository

import android.content.Context
import android.net.Uri
import com.aethelsoft.grooveplayer.domain.model.AudioTags
import com.aethelsoft.grooveplayer.domain.repository.AudioTagRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioTagRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioTagRepository {

    override suspend fun readTags(contentUri: String): AudioTags? = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(contentUri)
            val ext = context.contentResolver.getType(uri)?.let(::extensionForMime) ?: ".mp3"
            val tempFile = File.createTempFile("tag_read_", ext, context.cacheDir).apply {
                deleteOnExit()
            }
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null

                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tagOrCreateDefault ?: return@withContext null

                val yearStr = tag.getFirst(FieldKey.YEAR)
                val year = yearStr.toIntOrNull()

                val trackStr = tag.getFirst(FieldKey.TRACK)
                val trackNumber = trackStr.toIntOrNull()

                val artistStr = tag.getFirst(FieldKey.ARTIST)
                val artists = if (artistStr.isNotBlank()) {
                    artistStr.split("/", ";&")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                } else emptyList()

                val genreStr = tag.getFirst(FieldKey.GENRE)
                val genres = if (genreStr.isNotBlank()) listOf(genreStr) else emptyList()

                val artwork = tag.getFirstArtwork()
                val artworkBytes = artwork?.binaryData
                val artworkMime = artwork?.mimeType

                val grooveId = tag.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID)
                    .takeIf { it.isNotBlank() }

                AudioTags(
                    title = tag.getFirst(FieldKey.TITLE).ifBlank { "" },
                    artists = artists.ifEmpty { listOf(tag.getFirst(FieldKey.ARTIST)).filter { it.isNotBlank() } },
                    album = tag.getFirst(FieldKey.ALBUM).takeIf { it.isNotBlank() },
                    genres = genres,
                    year = year,
                    trackNumber = trackNumber,
                    artworkBytes = artworkBytes,
                    artworkMimeType = artworkMime,
                    grooveId = grooveId
                )
            } finally {
                tempFile.delete()
            }
        }.getOrElse {
            android.util.Log.e("AudioTagRepository", "Failed to read tags from $contentUri", it)
            null
        }
    }

    private fun extensionForMime(mimeType: String): String = when {
        mimeType.contains("mpeg", ignoreCase = true) -> ".mp3"
        mimeType.contains("mp4", ignoreCase = true) || mimeType.contains("x-m4a", ignoreCase = true) -> ".m4a"
        mimeType.contains("flac", ignoreCase = true) -> ".flac"
        mimeType.contains("ogg", ignoreCase = true) -> ".ogg"
        mimeType.contains("wav", ignoreCase = true) -> ".wav"
        mimeType.contains("aac", ignoreCase = true) -> ".aac"
        else -> ".mp3"
    }

    override suspend fun writeTags(contentUri: String, tags: AudioTags): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(contentUri)
                val ext = context.contentResolver.getType(uri)?.let(::extensionForMime) ?: ".mp3"
                val tempFile = File.createTempFile("tag_write_", ext, context.cacheDir).apply {
                    deleteOnExit()
                }
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("Cannot open input stream for $contentUri")

                    val audioFile = AudioFileIO.read(tempFile)
                    val tag = audioFile.tagOrCreateDefault
                        ?: throw IllegalStateException("Cannot get or create tag")

                    tag.setField(FieldKey.TITLE, tags.title)

                    val artistStr = tags.artists.joinToString("; ")
                    tag.setField(FieldKey.ARTIST, artistStr)

                    tag.setField(FieldKey.ALBUM, tags.album ?: "")
                    tag.setField(FieldKey.GENRE, tags.genres.firstOrNull() ?: "")

                    if (tags.year != null) {
                        tag.setField(FieldKey.YEAR, tags.year.toString())
                    }
                    if (tags.trackNumber != null) {
                        tag.setField(FieldKey.TRACK, tags.trackNumber.toString())
                    }

                    // Persist app-specific stable ID in a standard tag field
                    if (!tags.grooveId.isNullOrBlank()) {
                        tag.setField(FieldKey.MUSICBRAINZ_TRACK_ID, tags.grooveId)
                    }

                    if (tags.artworkBytes != null) {
                        tag.deleteArtworkField()
                        val ext = when (tags.artworkMimeType) {
                            "image/png" -> ".png"
                            else -> ".jpg"
                        }
                        val tempArt = File.createTempFile("art_", ext, context.cacheDir).apply {
                            deleteOnExit()
                            writeBytes(tags.artworkBytes)
                        }
                        try {
                            val artwork = ArtworkFactory.createArtworkFromFile(tempArt)
                            artwork.mimeType = tags.artworkMimeType ?: "image/jpeg"
                            tag.addField(artwork)
                        } finally {
                            tempArt.delete()
                        }
                    }

                    audioFile.commit()

                    context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                        tempFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("Cannot open output stream for $contentUri")
                } finally {
                    tempFile.delete()
                }
            }.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { Result.failure(it) }
            )
        }
}

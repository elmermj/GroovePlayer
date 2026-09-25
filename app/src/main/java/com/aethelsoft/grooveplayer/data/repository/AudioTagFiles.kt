package com.aethelsoft.grooveplayer.data.repository

import com.aethelsoft.grooveplayer.domain.model.AudioTags
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.Artwork
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File

/**
 * Reads and writes tags on a real file. The destination is replaced only after
 * the edited copy commits, via a same-directory rename.
 */
object AudioTagFiles {
    fun read(file: File, mimeType: String? = null): AudioTags {
        val format = AudioContainerFormat.detect(
            file.name,
            mimeType,
            AudioContainerFormat.header(file),
        )
        val audioFile = if (format.writerSuffix != null) {
            AudioFileIO.readAs(file, format.writerSuffix)
        } else {
            AudioFileIO.readMagic(file)
        }
        val tag = audioFile.tag ?: throw IllegalStateException("No tags in ${file.name}")
        return tag.toAudioTags()
    }

    fun replace(
        destination: File,
        tags: AudioTags,
        replaceFrontCover: Boolean,
        mimeType: String? = null,
    ) {
        if (!destination.isFile) {
            throw IllegalStateException("Can't find this song file. The song was left unchanged.")
        }
        val format = AudioContainerFormat.detect(
            destination.name,
            mimeType,
            AudioContainerFormat.header(destination),
        )
        if (!format.writable) {
            throw IllegalStateException(format.refusalMessage())
        }
        val parent = destination.parentFile
            ?: throw IllegalStateException("Can't find this song file. The song was left unchanged.")
        val temp = File(parent, ".${destination.name}.${System.nanoTime()}.${format.writerSuffix}")
        var swapped = false
        try {
            destination.copyTo(temp, overwrite = true)
            apply(temp, format.writerSuffix!!, tags, replaceFrontCover)
            if (!temp.renameTo(destination)) {
                throw IllegalStateException(
                    "Couldn't replace the song file safely. The song was left unchanged.",
                )
            }
            swapped = true
        } catch (error: Exception) {
            if (error is IllegalStateException && error.message.orEmpty().contains("left unchanged")) {
                throw error
            }
            throw IllegalStateException(
                "Couldn't save tags to this ${format.label} file. The song was left unchanged.",
                error,
            )
        } finally {
            if (!swapped && temp.exists()) temp.delete()
        }
    }

    /** Edits [file] in place. Callers pass a disposable copy, never the song itself. */
    fun editCopy(
        file: File,
        tags: AudioTags,
        replaceFrontCover: Boolean,
        mimeType: String? = null,
    ) {
        val format = AudioContainerFormat.detect(
            file.name,
            mimeType,
            AudioContainerFormat.header(file),
        )
        if (!format.writable) {
            throw IllegalStateException(format.refusalMessage())
        }
        try {
            apply(file, format.writerSuffix!!, tags, replaceFrontCover)
        } catch (error: Exception) {
            if (error is IllegalStateException && error.message.orEmpty().contains("left unchanged")) {
                throw error
            }
            throw IllegalStateException(
                "Couldn't save tags to this ${format.label} file. The song was left unchanged.",
                error,
            )
        }
    }

    private fun apply(
        file: File,
        writerSuffix: String,
        tags: AudioTags,
        replaceFrontCover: Boolean,
    ) {
        val audioFile = AudioFileIO.readAs(file, writerSuffix)
        val tag = audioFile.tagOrCreateAndSetDefault
            ?: throw IllegalStateException("Couldn't save tags to this file. The song was left unchanged.")
        tag.setField(FieldKey.TITLE, tags.title)
        tag.setField(FieldKey.ARTIST, tags.artists.joinToString("; "))
        tag.setField(FieldKey.ALBUM, tags.album ?: "")
        tag.setField(FieldKey.GENRE, tags.genres.firstOrNull() ?: "")
        if (tags.year != null) {
            tag.setField(FieldKey.YEAR, tags.year.toString())
        }
        if (tags.trackNumber != null) {
            tag.setField(FieldKey.TRACK, tags.trackNumber.toString())
        }
        if (!tags.grooveId.isNullOrBlank()) {
            tag.setField(FieldKey.MUSICBRAINZ_TRACK_ID, tags.grooveId)
        }
        applyPictures(tag, tags, replaceFrontCover)
        audioFile.commit()
    }

    private fun applyPictures(tag: Tag, tags: AudioTags, replaceFrontCover: Boolean) {
        val planned = ArtworkPreservation.picturesToWrite(
            existing = tag.artworkList.map { it.toPicture() },
            replaceFrontCover = replaceFrontCover,
            newFrontCover = frontCoverOrNull(tags, replaceFrontCover),
        ) ?: return
        if (tag.artworkList.isNotEmpty()) {
            tag.deleteArtworkField()
        }
        for (picture in planned) {
            tag.addField(picture.toArtwork())
        }
    }

    private fun frontCoverOrNull(tags: AudioTags, replaceFrontCover: Boolean): TagPicture? {
        if (!replaceFrontCover) return null
        val bytes = tags.artworkBytes
        if (bytes == null || bytes.isEmpty()) return null
        return TagPicture(
            pictureType = ArtworkPreservation.FRONT_COVER,
            mimeType = tags.artworkMimeType ?: "image/jpeg",
            description = "",
            data = bytes,
        )
    }
}

private fun Tag.toAudioTags(): AudioTags {
    val year = getFirst(FieldKey.YEAR).toIntOrNull()
    val trackNumber = getFirst(FieldKey.TRACK).toIntOrNull()
    val artistStr = getFirst(FieldKey.ARTIST)
    val artists = if (artistStr.isNotBlank()) {
        artistStr.split("/", ";&")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    } else {
        emptyList()
    }
    val genreStr = getFirst(FieldKey.GENRE)
    val artwork = getFirstArtwork()
    return AudioTags(
        title = getFirst(FieldKey.TITLE).ifBlank { "" },
        artists = artists.ifEmpty {
            listOf(getFirst(FieldKey.ARTIST)).filter { it.isNotBlank() }
        },
        album = getFirst(FieldKey.ALBUM).takeIf { it.isNotBlank() },
        genres = if (genreStr.isNotBlank()) listOf(genreStr) else emptyList(),
        year = year,
        trackNumber = trackNumber,
        artworkBytes = artwork?.binaryData,
        artworkMimeType = artwork?.mimeType,
        grooveId = getFirst(FieldKey.MUSICBRAINZ_TRACK_ID).takeIf { it.isNotBlank() },
    )
}

private fun Artwork.toPicture(): TagPicture = TagPicture(
    pictureType = pictureType,
    mimeType = mimeType,
    description = description,
    data = binaryData,
    linkedUrl = if (isLinked) imageUrl else null,
)

private fun TagPicture.toArtwork(): Artwork {
    val artwork = ArtworkFactory.getNew()
    artwork.pictureType = pictureType
    artwork.mimeType = mimeType
    artwork.description = description ?: ""
    if (!linkedUrl.isNullOrBlank() && (data == null || data.isEmpty())) {
        artwork.isLinked = true
        artwork.imageUrl = linkedUrl
    } else {
        artwork.binaryData = data
    }
    return artwork
}

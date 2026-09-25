package com.aethelsoft.grooveplayer.data.repository

import com.aethelsoft.grooveplayer.domain.model.AudioTags
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AudioTagEditTest {

    @Test
    fun formatFollowsMagicThenMimeThenExtension() {
        assertEquals("FLAC", AudioContainerFormat.detect("song.mp3", "audio/mpeg", "fLaC".toByteArray()).label)
        assertEquals("flac", AudioContainerFormat.detect("song.mp3", null, "fLaC....".toByteArray()).writerSuffix)
        assertEquals("ogg", AudioContainerFormat.detect("a.opus", null, "OggS".toByteArray()).writerSuffix)
        assertEquals(
            "Opus",
            AudioContainerFormat.detect("a.ogg", null, "OggSxxxxOpusHead".toByteArray()).label,
        )
        assertEquals("ogg", AudioContainerFormat.detect("a.opus", null, ByteArray(0)).writerSuffix)
        assertEquals("mp3", AudioContainerFormat.detect("no-ext", "audio/mpeg", ByteArray(0)).writerSuffix)
        assertEquals("m4a", AudioContainerFormat.detect("track.m4a", null, ByteArray(0)).writerSuffix)
        assertEquals("m4a", AudioContainerFormat.detect("track.mp4", null, ByteArray(0)).writerSuffix)
        assertEquals("wav", AudioContainerFormat.detect("track.wav", null, ByteArray(0)).writerSuffix)
        val wave = ByteArray(12)
        "RIFF".toByteArray().copyInto(wave, 0)
        "WAVE".toByteArray().copyInto(wave, 8)
        assertEquals("WAV", AudioContainerFormat.detect("song.bin", null, wave).label)
        val ftyp = ByteArray(16)
        "ftyp".toByteArray().copyInto(ftyp, 4)
        "M4A ".toByteArray().copyInto(ftyp, 8)
        assertEquals("M4A", AudioContainerFormat.detect("song.bin", null, ftyp).label)
        assertEquals("MP3", AudioContainerFormat.detect("song.bin", null, "ID3".toByteArray()).label)
        assertEquals("MP3", AudioContainerFormat.detect("song.bin", null, byteArrayOf(0xFF.toByte(), 0xFB.toByte())).label)
        assertFalse(AudioContainerFormat.detect("voice.aac", "audio/aac", ByteArray(0)).writable)
        assertEquals("AAC", AudioContainerFormat.detect("voice.aac", null, byteArrayOf(0xFF.toByte(), 0xF1.toByte())).label)
        assertFalse(AudioContainerFormat.detect("song.bin", null, byteArrayOf(1, 2, 3)).writable)
        assertTrue(
            AudioContainerFormat.detect("voice.aac", null, ByteArray(0))
                .refusalMessage()
                .contains("left unchanged"),
        )
    }

    @Test
    fun titleOnlyEditDoesNotRewritePictures() {
        val existing = listOf(icon(), front())
        assertNull(ArtworkPreservation.picturesToWrite(existing, replaceFrontCover = false, newFrontCover = null))
    }

    @Test
    fun replacingFrontCoverKeepsOtherPictures() {
        val replacement = TagPicture(ArtworkPreservation.FRONT_COVER, "image/jpeg", "", byteArrayOf(7, 7))
        val planned = ArtworkPreservation.picturesToWrite(
            existing = listOf(icon(), front(), back()),
            replaceFrontCover = true,
            newFrontCover = replacement,
        )
        requireNotNull(planned)
        assertEquals(listOf(3, 1, 4), planned.map { it.pictureType })
        assertArrayEquals(byteArrayOf(7, 7), planned[0].data)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), planned[1].data)
        assertArrayEquals(byteArrayOf(5, 5), planned[2].data)
    }

    @Test
    fun clearingFrontCoverDropsOnlyThatPicture() {
        val planned = ArtworkPreservation.picturesToWrite(
            existing = listOf(icon(), front()),
            replaceFrontCover = true,
            newFrontCover = null,
        )
        requireNotNull(planned)
        assertEquals(listOf(1), planned.map { it.pictureType })
    }

    @Test
    fun titleOnlySaveKeepsEveryPictureAndUnrelatedFrame() {
        val dir = tempDir()
        val file = File(dir, "song.mp3")
        file.writeBytes(mp3Frames())
        val audio = AudioFileIO.read(file)
        val tag = audio.tagOrCreateAndSetDefault
        tag.setField(FieldKey.TITLE, "Old")
        tag.setField(FieldKey.COMMENT, "keep-me")
        tag.setField(FieldKey.MUSICBRAINZ_TRACK_ID, "groove-1")
        tag.addField(picture(1, byteArrayOf(1, 2, 3, 4), "image/png"))
        tag.addField(picture(3, byteArrayOf(9, 9, 9, 9, 9), "image/jpeg"))
        audio.commit()

        AudioTagFiles.replace(
            destination = file,
            tags = AudioTags(title = "New", artists = listOf("A")),
            replaceFrontCover = false,
        )

        val saved = AudioFileIO.read(file).tag
        assertEquals("New", saved.getFirst(FieldKey.TITLE))
        assertEquals("keep-me", saved.getFirst(FieldKey.COMMENT))
        assertEquals("groove-1", saved.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID))
        val pictures = saved.artworkList
        assertEquals(2, pictures.size)
        assertEquals(1, pictures[0].pictureType)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), pictures[0].binaryData)
        assertEquals(3, pictures[1].pictureType)
        assertArrayEquals(byteArrayOf(9, 9, 9, 9, 9), pictures[1].binaryData)
        assertTrue(dir.listFiles().orEmpty().none { it.name.startsWith(".") })
    }

    @Test
    fun changedFrontCoverLeavesTheOtherPicture() {
        val dir = tempDir()
        val file = File(dir, "song.mp3")
        file.writeBytes(mp3Frames())
        val audio = AudioFileIO.read(file)
        val tag = audio.tagOrCreateAndSetDefault
        tag.setField(FieldKey.TITLE, "Old")
        tag.setField(FieldKey.LYRICS, "verse")
        tag.addField(picture(1, byteArrayOf(1, 2, 3, 4), "image/png"))
        tag.addField(picture(3, byteArrayOf(9, 9, 9), "image/jpeg"))
        audio.commit()

        AudioTagFiles.replace(
            destination = file,
            tags = AudioTags(
                title = "Old",
                artworkBytes = byteArrayOf(8, 8, 8, 8),
                artworkMimeType = "image/png",
            ),
            replaceFrontCover = true,
        )

        val saved = AudioFileIO.read(file).tag
        assertEquals("verse", saved.getFirst(FieldKey.LYRICS))
        val pictures = saved.artworkList
        assertEquals(2, pictures.size)
        assertEquals(3, pictures[0].pictureType)
        assertArrayEquals(byteArrayOf(8, 8, 8, 8), pictures[0].binaryData)
        assertEquals(1, pictures[1].pictureType)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), pictures[1].binaryData)
    }

    @Test
    fun unwritableFormatLeavesTheFileUntouched() {
        val dir = tempDir()
        val file = File(dir, "voice.aac")
        val original = byteArrayOf(1, 2, 3, 4, 5, 6)
        file.writeBytes(original)
        val error = runCatching {
            AudioTagFiles.replace(
                destination = file,
                tags = AudioTags(title = "Nope"),
                replaceFrontCover = false,
            )
        }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertTrue(error!!.message.orEmpty().contains("left unchanged"))
        assertArrayEquals(original, file.readBytes())
        assertEquals(listOf("voice.aac"), dir.list().orEmpty().toList())
    }

    @Test
    fun failedMp3WriteDoesNotReplaceTheOriginal() {
        val dir = tempDir()
        val file = File(dir, "broken.mp3")
        val original = byteArrayOf(1, 2, 3, 4, 5)
        file.writeBytes(original)
        val error = runCatching {
            AudioTagFiles.replace(
                destination = file,
                tags = AudioTags(title = "Nope"),
                replaceFrontCover = false,
            )
        }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertTrue(error!!.message.orEmpty().contains("left unchanged"))
        assertArrayEquals(original, file.readBytes())
    }

    private fun icon() = TagPicture(1, "image/png", "icon", byteArrayOf(1, 2, 3, 4))

    private fun front() = TagPicture(3, "image/jpeg", "front", byteArrayOf(9, 9, 9))

    private fun back() = TagPicture(4, "image/jpeg", "back", byteArrayOf(5, 5))

    private fun picture(type: Int, data: ByteArray, mime: String) =
        ArtworkFactory.getNew().apply {
            pictureType = type
            binaryData = data
            mimeType = mime
        }

    private fun mp3Frames(): ByteArray {
        val frame = ByteArray(417)
        frame[0] = 0xFF.toByte()
        frame[1] = 0xFB.toByte()
        frame[2] = 0x90.toByte()
        frame[3] = 0x64.toByte()
        return frame + frame
    }

    private fun tempDir(): File = File.createTempFile("tag-edit", "").apply {
        delete()
        mkdirs()
    }
}

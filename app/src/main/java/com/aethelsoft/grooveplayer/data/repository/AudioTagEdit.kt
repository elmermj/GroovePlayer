package com.aethelsoft.grooveplayer.data.repository

/**
 * Container sniffing and front-cover edits for the metadata writer.
 * Magic bytes win over the name so a file:// URI with no MIME type is not treated as MP3.
 */
data class AudioContainer(
    val label: String,
    /** jaudiotagger suffix when this library can write the container. */
    val writerSuffix: String?,
) {
    val writable: Boolean get() = writerSuffix != null

    fun refusalMessage(): String {
        val kind = label.ifBlank { "file" }
        return "Can't write tags to this $kind file. The song was left unchanged."
    }
}

object AudioContainerFormat {
    val MP3 = AudioContainer("MP3", "mp3")
    val M4A = AudioContainer("M4A", "m4a")
    val FLAC = AudioContainer("FLAC", "flac")
    val OGG = AudioContainer("Ogg", "ogg")
    val OPUS = AudioContainer("Opus", "ogg")
    val WAV = AudioContainer("WAV", "wav")
    val AAC = AudioContainer("AAC", null)
    val UNKNOWN = AudioContainer("", null)

    fun detect(fileName: String?, mimeType: String?, header: ByteArray): AudioContainer {
        fromMagic(header)?.let { return it }
        fromMime(mimeType)?.let { return it }
        fromExtension(fileName)?.let { return it }
        return UNKNOWN
    }

    fun header(file: java.io.File, max: Int = HEADER_BYTES): ByteArray {
        if (!file.isFile) return ByteArray(0)
        java.io.FileInputStream(file).use { input ->
            val buf = ByteArray(max)
            var offset = 0
            while (offset < max) {
                val read = input.read(buf, offset, max - offset)
                if (read < 0) break
                offset += read
            }
            return if (offset == max) buf else buf.copyOf(offset)
        }
    }

    private fun fromMagic(header: ByteArray): AudioContainer? {
        if (header.size >= 4 && header.startsWithAscii("fLaC")) return FLAC
        if (header.size >= 4 && header.startsWithAscii("OggS")) {
            return if (header.hasAscii("OpusHead")) OPUS else OGG
        }
        if (header.size >= 12 && header.startsWithAscii("RIFF") && header.hasAsciiAt(8, "WAVE")) {
            return WAV
        }
        if (header.size >= 12 && header.hasAsciiAt(4, "ftyp")) return M4A
        if (header.size >= 3 && header.startsWithAscii("ID3")) return MP3
        if (isMp3Frame(header)) return MP3
        if (isAacAdts(header)) return AAC
        return null
    }

    private fun fromMime(mimeType: String?): AudioContainer? {
        val raw = mimeType?.trim()?.lowercase().orEmpty()
        if (raw.isEmpty() || raw == "application/octet-stream") return null
        val base = raw.substringBefore(';').trim()
        return when {
            raw.contains("opus") -> OPUS
            base.contains("mpeg") || base == "audio/mp3" -> MP3
            base.contains("mp4") || base.contains("m4a") -> M4A
            base.contains("flac") -> FLAC
            base.contains("ogg") || base.contains("vorbis") -> OGG
            base.contains("wav") || base.contains("wave") -> WAV
            base.contains("aac") -> AAC
            else -> null
        }
    }

    private fun fromExtension(fileName: String?): AudioContainer? {
        val name = fileName?.trim().orEmpty()
        val ext = name.substringAfterLast('.', "")
        if (ext.isEmpty() || ext == name) return null
        return when (ext.lowercase()) {
            "mp3" -> MP3
            "m4a", "mp4", "m4b", "m4p" -> M4A
            "flac" -> FLAC
            "ogg", "oga" -> OGG
            "opus" -> OPUS
            "wav", "wave" -> WAV
            "aac" -> AAC
            else -> null
        }
    }

    private fun isMp3Frame(header: ByteArray): Boolean {
        if (header.size < 2 || header[0] != 0xFF.toByte()) return false
        val second = header[1].toInt() and 0xFF
        if ((second and 0xE0) != 0xE0) return false
        val version = (second shr 3) and 0x3
        val layer = (second shr 1) and 0x3
        return version != 1 && layer == 1
    }

    private fun isAacAdts(header: ByteArray): Boolean {
        if (header.size < 2 || header[0] != 0xFF.toByte()) return false
        val second = header[1].toInt() and 0xFF
        if ((second and 0xF0) != 0xF0) return false
        val layer = (second shr 1) and 0x3
        return layer == 0
    }

    private fun ByteArray.startsWithAscii(text: String): Boolean = hasAsciiAt(0, text)

    private fun ByteArray.hasAscii(text: String): Boolean {
        if (size < text.length) return false
        val bytes = text.toByteArray(Charsets.US_ASCII)
        for (start in 0..size - bytes.size) {
            if (hasAsciiAt(start, text)) return true
        }
        return false
    }

    private fun ByteArray.hasAsciiAt(offset: Int, text: String): Boolean {
        val bytes = text.toByteArray(Charsets.US_ASCII)
        if (offset < 0 || offset + bytes.size > size) return false
        for (index in bytes.indices) {
            if (this[offset + index] != bytes[index]) return false
        }
        return true
    }

    private const val HEADER_BYTES = 128
}

data class TagPicture(
    val pictureType: Int,
    val mimeType: String?,
    val description: String?,
    val data: ByteArray?,
    val linkedUrl: String? = null,
)

object ArtworkPreservation {
    /** ID3 / Vorbis front-cover picture type. Other pictures stay in the file. */
    const val FRONT_COVER = 3

    /**
     * Null means the caller must not touch picture frames.
     * A list replaces picture frames: only the front cover is removed or swapped.
     */
    fun picturesToWrite(
        existing: List<TagPicture>,
        replaceFrontCover: Boolean,
        newFrontCover: TagPicture?,
    ): List<TagPicture>? {
        if (!replaceFrontCover) return null
        val kept = existing.filter { it.pictureType != FRONT_COVER }
        if (newFrontCover == null || newFrontCover.data == null || newFrontCover.data.isEmpty()) {
            return kept
        }
        return listOf(newFrontCover.copy(pictureType = FRONT_COVER)) + kept
    }
}

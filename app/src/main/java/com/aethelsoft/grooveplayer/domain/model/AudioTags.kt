package com.aethelsoft.grooveplayer.domain.model

/**
 * Audio file tags that can be read/written via jaudiotagger.
 * Supports MP3, M4A, FLAC, OGG, WAV, etc.
 */
data class AudioTags(
    val title: String = "",
    val artists: List<String> = emptyList(),
    val album: String? = null,
    val genres: List<String> = emptyList(),
    val year: Int? = null,
    val trackNumber: Int? = null,
    val artworkBytes: ByteArray? = null,
    val artworkMimeType: String? = null,
    /** App-specific stable song identifier stored in tags (e.g. MUSICBRAINZ_TRACK_ID). */
    val grooveId: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioTags
        if (title != other.title) return false
        if (artists != other.artists) return false
        if (album != other.album) return false
        if (genres != other.genres) return false
        if (year != other.year) return false
        if (trackNumber != other.trackNumber) return false
        if (artworkBytes != null) {
            if (other.artworkBytes == null) return false
            if (!artworkBytes.contentEquals(other.artworkBytes)) return false
        } else if (other.artworkBytes != null) return false
        if (artworkMimeType != other.artworkMimeType) return false
        if (grooveId != other.grooveId) return false
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artists.hashCode()
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + genres.hashCode()
        result = 31 * result + (year ?: 0)
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        result = 31 * result + (artworkMimeType?.hashCode() ?: 0)
        result = 31 * result + (grooveId?.hashCode() ?: 0)
        return result
    }
}

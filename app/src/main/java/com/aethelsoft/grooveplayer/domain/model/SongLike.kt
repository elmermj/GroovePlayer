package com.aethelsoft.grooveplayer.domain.model

/**
 * One liked song. [songId] is unique, so a song cannot appear twice in Favorite tracks.
 * Artists and albums are not stored separately; they are grouped from these rows.
 */
data class SongLike(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val uri: String,
    val artworkUrl: String?,
    val durationMs: Long,
    val likedAt: Long,
)

fun likedTrackLabel(count: Int): String =
    if (count == 1) "1 liked track" else "$count liked tracks"

fun Song.toSongLike(likedAt: Long): SongLike {
    val titleText = title.trim().ifEmpty { "Unknown" }
    val artistText = artist.trim().ifEmpty { "Unknown Artist" }
    val albumText = album?.name?.trim().takeUnless { it.isNullOrEmpty() }
        ?: "Single - $titleText"
    return SongLike(
        songId = id,
        title = titleText,
        artist = artistText,
        album = albumText,
        genre = genre,
        uri = uri,
        artworkUrl = artworkUrl?.takeIf { it.isNotBlank() },
        durationMs = durationMs,
        likedAt = likedAt,
    )
}

fun SongLike.toSong(): Song {
    return Song(
        id = songId,
        title = title,
        artist = artist,
        uri = uri,
        genre = genre,
        durationMs = durationMs,
        artworkUrl = artworkUrl,
        album = Album(
            id = makeAlbumId(artist, album),
            name = album,
            artist = artist,
            artworkUrl = artworkUrl,
            songs = emptyList(),
        ),
    )
}

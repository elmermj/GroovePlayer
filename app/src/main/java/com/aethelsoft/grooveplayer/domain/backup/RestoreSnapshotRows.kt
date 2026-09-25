package com.aethelsoft.grooveplayer.domain.backup

import com.aethelsoft.grooveplayer.domain.model.UserSettings
import com.aethelsoft.grooveplayer.domain.model.VisualizationMode

/**
 * Column-oriented copy of a cloud Room snapshot.
 * Missing columns keep the current app defaults so an older schema still applies.
 */
data class RestoredPlayback(
    val songId: String,
    val songTitle: String,
    val artist: String,
    val album: String,
    val genre: String,
    val uri: String,
    val artworkUrl: String?,
    val playedAt: Long,
)

data class RestoredMetadata(
    val songId: String,
    val title: String,
    val genres: String,
    val artists: String,
    val album: String?,
    val year: Int?,
    val useAlbumYear: Boolean,
    val updatedAt: Long,
)

fun restoredSettings(row: Map<String, String?>): UserSettings {
    return UserSettings(
        id = 1,
        lastPlayedSongsTimer = row.int("lastPlayedSongsTimer", 3),
        fadeTimer = row.int("fadeTimer", 0),
        equalizerEnabled = row.bool("equalizerEnabled", false),
        equalizerPreset = row.int("equalizerPreset", -1),
        equalizerBandLevels = row.string("equalizerBandLevels")
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?: emptyList(),
        lastPlayedSongId = row.string("lastPlayedSongId")?.takeIf { it.isNotBlank() },
        lastPlayedPosition = row.long("lastPlayedPosition", 0L),
        shuffleEnabled = row.bool("shuffleEnabled", false),
        repeatMode = row.string("repeatMode")?.takeIf { it.isNotBlank() } ?: "OFF",
        queueSongIds = row.string("queueSongIds")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList(),
        queueStartIndex = row.int("queueStartIndex", 0),
        isEndlessQueue = row.bool("isEndlessQueue", false),
        visualizationMode = when (row.string("visualizationMode")) {
            "OFF" -> VisualizationMode.OFF
            "REAL_TIME" -> VisualizationMode.REAL_TIME
            else -> VisualizationMode.SIMULATED
        },
        showMiniPlayerOnStart = row.bool("showMiniPlayerOnStart", false),
        notificationsEnabled = row.bool("notificationsEnabled", true),
        excludedFolders = row.string("excludedFolders")
            ?.split("||")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList(),
        uiStyleId = row.string("uiStyleId")?.takeIf { it.isNotBlank() } ?: "default",
        uiStyleOverrides = row.string("uiStyleOverrides").orEmpty(),
    )
}

fun restoredPlayback(row: Map<String, String?>): RestoredPlayback? {
    val songId = row.string("songId")?.takeIf { it.isNotBlank() } ?: return null
    return RestoredPlayback(
        songId = songId,
        songTitle = row.string("songTitle").orEmpty(),
        artist = row.string("artist").orEmpty(),
        album = row.string("album").orEmpty(),
        genre = row.string("genre").orEmpty(),
        uri = row.string("uri").orEmpty(),
        artworkUrl = row.string("artworkUrl")?.takeIf { it.isNotBlank() },
        playedAt = row.long("playedAt", 0L),
    )
}

fun restoredMetadata(row: Map<String, String?>): RestoredMetadata? {
    val songId = row.string("songId")?.takeIf { it.isNotBlank() } ?: return null
    return RestoredMetadata(
        songId = songId,
        title = row.string("title").orEmpty(),
        genres = row.string("genres").orEmpty(),
        artists = row.string("artists").orEmpty(),
        album = row.string("album")?.takeIf { it.isNotBlank() },
        year = row.string("year")?.toIntOrNull(),
        useAlbumYear = row.bool("useAlbumYear", false),
        updatedAt = row.long("updatedAt", 0L),
    )
}

data class RestoredSongLike(
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

fun restoredSongLike(row: Map<String, String?>): RestoredSongLike? {
    val songId = row.string("songId")?.takeIf { it.isNotBlank() } ?: return null
    val title = row.string("title").orEmpty().ifBlank { "Unknown" }
    return RestoredSongLike(
        songId = songId,
        title = title,
        artist = row.string("artist").orEmpty().ifBlank { "Unknown Artist" },
        album = row.string("album").orEmpty().ifBlank { "Single - $title" },
        genre = row.string("genre").orEmpty(),
        uri = row.string("uri").orEmpty(),
        artworkUrl = row.string("artworkUrl")?.takeIf { it.isNotBlank() },
        durationMs = row.long("durationMs", 0L),
        likedAt = row.long("likedAt", 0L),
    )
}

/** Path update for a song that already exists locally. Does not insert a foreign catalog row. */
fun restoredSourcePath(row: Map<String, String?>): Pair<String, String>? {
    val songId = row.string("songId")?.takeIf { it.isNotBlank() } ?: return null
    val path = row.string("sourcePath")?.takeIf { it.isNotBlank() } ?: return null
    return songId to path
}

private fun Map<String, String?>.string(name: String): String? {
    this[name]?.let { return it }
    val key = keys.firstOrNull { it.equals(name, ignoreCase = true) } ?: return null
    return this[key]
}

private fun Map<String, String?>.int(name: String, default: Int): Int =
    string(name)?.toIntOrNull() ?: default

private fun Map<String, String?>.long(name: String, default: Long): Long =
    string(name)?.toLongOrNull() ?: default

private fun Map<String, String?>.bool(name: String, default: Boolean): Boolean {
    val raw = string(name) ?: return default
    return raw == "1" || raw.equals("true", ignoreCase = true)
}

package com.aethelsoft.grooveplayer.widget

import com.aethelsoft.grooveplayer.domain.artwork.EmbeddedArtworkKeys
import java.io.File

/**
 * Snapshot the home-screen widget can draw. Built from the live player only.
 * Remote artwork URLs are kept for identity but are not a loadable source.
 */
data class PlaybackWidgetState(
    val trackId: String?,
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val hasTrack: Boolean,
    val artworkUrl: String?,
) {
    companion object {
        val EMPTY = PlaybackWidgetState(
            trackId = null,
            title = "",
            artist = "",
            isPlaying = false,
            hasTrack = false,
            artworkUrl = null,
        )

        fun from(
            trackId: String?,
            title: String?,
            artist: String?,
            isPlaying: Boolean,
            artworkUrl: String?,
        ): PlaybackWidgetState {
            val trimmedTitle = title?.trim().orEmpty()
            if (trimmedTitle.isEmpty()) return EMPTY
            return PlaybackWidgetState(
                trackId = trackId?.trim()?.takeIf { it.isNotEmpty() },
                title = trimmedTitle,
                artist = artist?.trim().orEmpty(),
                isPlaying = isPlaying,
                hasTrack = true,
                artworkUrl = artworkUrl?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }
}

/** Transport buttons talk to the playback service only while a track is queued. */
fun transportControlsActive(state: PlaybackWidgetState): Boolean = state.hasTrack

/**
 * Artwork the widget may decode itself. `content` and `file` URIs are local.
 * Anything else, including http, is not opened here.
 */
fun localArtworkUri(artworkUrl: String?): String? {
    val value = artworkUrl?.trim().orEmpty()
    val separator = value.indexOf(':')
    if (separator <= 0) return null
    return when (value.substring(0, separator).lowercase()) {
        "content", "file" -> value
        else -> null
    }
}

/**
 * A JPEG already written for this hash. Does not open the audio file or the network.
 */
fun cachedArtworkFile(cacheDir: File, artworkUrl: String?): File? {
    val hash = EmbeddedArtworkKeys.hashOf(artworkUrl) ?: return null
    if (!cacheDir.isDirectory) return null
    val prefix = hash.lowercase()
    return cacheDir.listFiles()?.firstOrNull { file ->
        file.isFile &&
            file.length() > 0L &&
            file.parentFile == cacheDir &&
            file.name.lowercase().startsWith(prefix) &&
            file.name.endsWith(".jpg")
    }
}

sealed class WidgetArtworkSource {
    data object None : WidgetArtworkSource()
    data class LocalUri(val value: String) : WidgetArtworkSource()
    data class CacheFile(val file: File) : WidgetArtworkSource()
}

fun widgetArtworkSource(cacheDir: File, artworkUrl: String?): WidgetArtworkSource {
    localArtworkUri(artworkUrl)?.let { return WidgetArtworkSource.LocalUri(it) }
    cachedArtworkFile(cacheDir, artworkUrl)?.let { return WidgetArtworkSource.CacheFile(it) }
    return WidgetArtworkSource.None
}

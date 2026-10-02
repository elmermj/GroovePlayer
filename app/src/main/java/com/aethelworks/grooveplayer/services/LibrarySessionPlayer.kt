package com.aethelworks.grooveplayer.services

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

/**
 * Session player for Android Auto.
 *
 * Transport commands reach the existing ExoPlayer. Queue replacement is ignored
 * here because [GrooveMediaLibraryService] already applied the queue through
 * [com.aethelworks.grooveplayer.domain.repository.PlayerRepository], which is
 * the phone's local-first / Premium stream path. A second write from the
 * session would restart that queue or drop a song the resolver had skipped.
 */
class LibrarySessionPlayer(
    player: Player,
) : ForwardingPlayer(player) {
    override fun setMediaItems(mediaItems: MutableList<MediaItem>) = Unit

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) = Unit

    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ) = Unit

    override fun setMediaItem(mediaItem: MediaItem) = Unit

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) = Unit

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) = Unit

    override fun addMediaItem(mediaItem: MediaItem) = Unit

    override fun addMediaItem(index: Int, mediaItem: MediaItem) = Unit

    override fun addMediaItems(mediaItems: MutableList<MediaItem>) = Unit

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) = Unit

    override fun removeMediaItem(index: Int) = Unit

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) = Unit

    override fun clearMediaItems() = Unit

    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) = Unit

    override fun replaceMediaItems(
        fromIndex: Int,
        toIndex: Int,
        mediaItems: MutableList<MediaItem>,
    ) = Unit

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) = Unit

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) = Unit
}

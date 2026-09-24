package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.data.player.AudioVisualizationData
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    suspend fun setQueue(songs: List<Song>, startIndex: Int = 0, isEndlessQueue: Boolean = false, autoPlay: Boolean = true)
    /** Jump to [index] in the current queue without rebuilding it (keeps order and endless state). */
    suspend fun skipToQueueIndex(index: Int)
    /** Move the queue item at [from] to [to]. Playback is not interrupted. */
    suspend fun moveQueueItem(from: Int, to: Int)
    /** Remove the queue item at [index]. The currently playing item cannot be removed. */
    suspend fun removeQueueItem(index: Int): Boolean
    /** Re-insert [song] at [index] (used by Undo after a remove). */
    suspend fun insertQueueItem(index: Int, song: Song)
    /** Insert [song] immediately after the currently playing item (Play next). */
    suspend fun playNext(song: Song)
    suspend fun play()
    suspend fun pause()
    suspend fun playSong(song: Song)
    suspend fun next()
    suspend fun previous()
    suspend fun seekTo(positionMs: Long)
    /**
     * Shuffle reorders the real queue: the current song stays put and only the songs after it are shuffled,
     * so the queue always shows true playback order. Turning it off restores the original order (edits carry over).
     * Pass [reorderQueue] = false to only restore the flag (e.g. cold-start restore of an already-shuffled queue).
     */
    suspend fun setShuffle(enable: Boolean, reorderQueue: Boolean = true)
    suspend fun setRepeat(mode: RepeatMode)
    suspend fun setVolume(volume: Float)
    suspend fun setFullScreenPlayerOpen(isOpen: Boolean)
    suspend fun setMute(mute: Boolean)

    fun observeCurrentSong(): Flow<Song?>
    fun observeIsPlaying(): Flow<Boolean>
    fun observePosition(): Flow<Long>
    fun observeDuration(): Flow<Long>
    fun observeQueue(): Flow<List<Song>>
    fun observeShuffle(): Flow<Boolean>
    fun observeRepeat(): Flow<RepeatMode>
    fun observeVolume(): Flow<Float>
    fun observeIsFullScreenPlayerOpen(): Flow<Boolean>
    fun observeIsPlayerMuted(): Flow<Boolean>
    fun observeAudioAmplitude(): Flow<Float>
    fun observeAudioVisualization(): Flow<AudioVisualizationData>
}
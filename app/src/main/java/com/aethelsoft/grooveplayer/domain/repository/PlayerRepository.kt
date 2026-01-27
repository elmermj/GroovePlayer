package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.data.player.AudioVisualizationData
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    suspend fun setQueue(songs: List<Song>, startIndex: Int = 0, isEndlessQueue: Boolean = false, autoPlay: Boolean = true)
    suspend fun play()
    suspend fun pause()
    suspend fun playSong(song: Song)
    suspend fun next()
    suspend fun previous()
    suspend fun seekTo(positionMs: Long)
    suspend fun setShuffle(enable: Boolean)
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
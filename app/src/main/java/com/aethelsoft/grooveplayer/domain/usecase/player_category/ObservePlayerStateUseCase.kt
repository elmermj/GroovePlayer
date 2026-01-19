package com.aethelsoft.grooveplayer.domain.usecase.player_category

import com.aethelsoft.grooveplayer.data.player.AudioVisualizationData
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for observing player state.
 * Provides access to reactive player state flows.
 * 
 * Note: For reactive state observation, it's acceptable in Clean Architecture
 * to have a UseCase that returns flows, as this maintains the dependency rule
 * (Domain layer doesn't depend on Data layer).
 */
class ObservePlayerStateUseCase @Inject constructor(
    private val playerRepository: PlayerRepository
) {
    fun observeCurrentSong(): Flow<Song?> = playerRepository.observeCurrentSong()
    fun observeIsPlaying(): Flow<Boolean> = playerRepository.observeIsPlaying()
    fun observePosition(): Flow<Long> = playerRepository.observePosition()
    fun observeDuration(): Flow<Long> = playerRepository.observeDuration()
    fun observeQueue(): Flow<List<Song>> = playerRepository.observeQueue()
    fun observeShuffle(): Flow<Boolean> = playerRepository.observeShuffle()
    fun observeRepeat(): Flow<RepeatMode> = playerRepository.observeRepeat()
    fun observeVolume(): Flow<Float> = playerRepository.observeVolume()
    fun observeIsFullScreenPlayerOpen(): Flow<Boolean> = playerRepository.observeIsFullScreenPlayerOpen()
    fun observeIsPlayerMuted(): Flow<Boolean> = playerRepository.observeIsPlayerMuted()
    fun observeAudioAmplitude(): Flow<Float> = playerRepository.observeAudioAmplitude()
    fun observeAudioVisualization(): Flow<AudioVisualizationData> = playerRepository.observeAudioVisualization()
}


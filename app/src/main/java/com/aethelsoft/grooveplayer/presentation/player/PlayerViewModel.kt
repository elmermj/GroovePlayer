package com.aethelsoft.grooveplayer.presentation.player

import android.app.Application
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.data.player.AudioVisualizationData
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.usecase.SetFullScreenPlayerOpenUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.ControlsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.NextSongUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.ObservePlayerStateUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.PlayPauseUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.PlaySongUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.PreviousSongUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.QueueUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.SeekUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.GetSongsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.SetMuteUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.SetVolumeUseCase
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import com.aethelsoft.grooveplayer.presentation.player.layouts.GlowEffectConfig
import com.aethelsoft.grooveplayer.utils.rememberDeviceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val observePlayerStateUseCase: ObservePlayerStateUseCase,
    private val playSongUseCase: PlaySongUseCase,
    private val playPauseUseCase: PlayPauseUseCase,
    private val seekUseCase: SeekUseCase,
    private val nextSongUseCase: NextSongUseCase,
    private val previousSongUseCase: PreviousSongUseCase,
    private val queueUseCase: QueueUseCase,
    private val controlsUseCase: ControlsUseCase,
    private val setVolumeUseCase: SetVolumeUseCase,
    private val setFullScreenPlayerOpenUseCase: SetFullScreenPlayerOpenUseCase,
    private val setMuteUseCase: SetMuteUseCase,
    private val getSongsUseCase: GetSongsUseCase,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    val currentSong: StateFlow<Song?> = observePlayerStateUseCase.observeCurrentSong().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val isPlaying: StateFlow<Boolean> = observePlayerStateUseCase.observeIsPlaying().stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val position: StateFlow<Long> = observePlayerStateUseCase.observePosition().stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val duration: StateFlow<Long> = observePlayerStateUseCase.observeDuration().stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val queue: StateFlow<List<Song>> = observePlayerStateUseCase.observeQueue().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val shuffle: StateFlow<Boolean> = observePlayerStateUseCase.observeShuffle().stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val repeat: StateFlow<RepeatMode> = observePlayerStateUseCase.observeRepeat().stateIn(viewModelScope, SharingStarted.Eagerly, RepeatMode.OFF)
    val volume: StateFlow<Float> = observePlayerStateUseCase.observeVolume().stateIn(viewModelScope, SharingStarted.Eagerly, 0.5f)
    val isFullScreenPlayerOpened: StateFlow<Boolean> = observePlayerStateUseCase.observeIsFullScreenPlayerOpen().stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isPlayerMuted: StateFlow<Boolean> = observePlayerStateUseCase.observeIsPlayerMuted().stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val audioAmplitude: StateFlow<Float> = observePlayerStateUseCase.observeAudioAmplitude().stateIn(viewModelScope, SharingStarted.Eagerly, 0f)
    val audioVisualization: StateFlow<AudioVisualizationData> = observePlayerStateUseCase.observeAudioVisualization().stateIn(
        viewModelScope, SharingStarted.Eagerly, AudioVisualizationData()
    )
    val visualizationMode: StateFlow<com.aethelsoft.grooveplayer.domain.model.VisualizationMode> =
        userRepository.observeUserSettings()
            .map { it.visualizationMode }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                com.aethelsoft.grooveplayer.domain.model.VisualizationMode.SIMULATED
            )

    val showMiniPlayerOnStart: StateFlow<Boolean> =
        userRepository.observeUserSettings()
            .map { it.showMiniPlayerOnStart }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _glowEffectConfig = MutableStateFlow<GlowEffectConfig>(GlowEffectConfig.Dramatic)

    val glowEffectConfig: StateFlow<GlowEffectConfig> = _glowEffectConfig.asStateFlow()

    fun setGlowEffect(config: GlowEffectConfig) {
        _glowEffectConfig.value = config
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0, isEndlessQueue: Boolean = false, autoPlay: Boolean = true) = viewModelScope.launch {
        queueUseCase(songs, startIndex, isEndlessQueue, autoPlay)
    }

    fun setQueueFromLastPlayedSongs(songs: List<Song>, startSongId: String) = viewModelScope.launch {
        val shuffledSongs = songs.shuffled()
        val startIndex = shuffledSongs.indexOfFirst { it.id == startSongId }
        queueUseCase(
            songs = shuffledSongs,
            startIndex = if (startIndex >= 0) startIndex else 0,
            isEndlessQueue = true
        )
    }


    fun playPauseToggle() = viewModelScope.launch {
        if (isPlaying.value) playPauseUseCase.pause() else playPauseUseCase.play()
    }

    fun next() = viewModelScope.launch { nextSongUseCase.next() }
    fun previous() = viewModelScope.launch { previousSongUseCase.previous() }

    fun seekTo(ms: Long) = viewModelScope.launch { seekUseCase(ms) }

    fun setShuffle(enabled: Boolean) = viewModelScope.launch { controlsUseCase.setShuffle(enabled) }
    fun setRepeat(mode: RepeatMode) = viewModelScope.launch { controlsUseCase.setRepeat(mode) }

    fun setVolume(volume: Float) = viewModelScope.launch { setVolumeUseCase.setVolume(volume) }

    fun setFullScreenPlayerOpen(isOpen: Boolean) = viewModelScope.launch {
        setFullScreenPlayerOpenUseCase.setFullScreenPlayerOpen(isOpen = isOpen)
    }
    fun setMute(mute: Boolean) = viewModelScope.launch { setMuteUseCase.setMute(mute) }
    
    fun stop() = viewModelScope.launch {
        // Stop playback completely: pause and clear queue
        playPauseUseCase.pause()
        queueUseCase(emptyList(), 0, false, false)
    }
    
    fun setVisualizationMode(mode: com.aethelsoft.grooveplayer.domain.model.VisualizationMode) =
        viewModelScope.launch {
            userRepository.updateVisualizationMode(mode)
        }
    
    fun restoreLastPlayedSong() = viewModelScope.launch {
        try {
            val settings = userRepository.getUserSettings()
            if (settings.lastPlayedSongId != null && settings.lastPlayedPosition > 0) {
                val allSongs = getSongsUseCase()
                
                // Restore queue if available
                val queueSongs = if (settings.queueSongIds.isNotEmpty()) {
                    settings.queueSongIds.mapNotNull { songId ->
                        allSongs.find { it.id == songId }
                    }
                } else {
                    // Fallback to just the last played song
                    val lastPlayedSong = allSongs.find { it.id == settings.lastPlayedSongId }
                    if (lastPlayedSong != null) listOf(lastPlayedSong) else emptyList()
                }
                
                if (queueSongs.isNotEmpty()) {
                    // Restore queue without auto-playing
                    setQueue(
                        songs = queueSongs,
                        startIndex = settings.queueStartIndex.coerceIn(0, queueSongs.lastIndex),
                        isEndlessQueue = settings.isEndlessQueue,
                        autoPlay = false // Don't auto-play on restore
                    )
                    
                    // Restore shuffle and repeat
                    setShuffle(settings.shuffleEnabled)
                    val repeatMode = try {
                        com.aethelsoft.grooveplayer.domain.model.RepeatMode.valueOf(settings.repeatMode)
                    } catch (e: Exception) {
                        com.aethelsoft.grooveplayer.domain.model.RepeatMode.OFF
                    }
                    setRepeat(repeatMode)
                    
                    // Seek to the saved position after a short delay to ensure player is ready
                    kotlinx.coroutines.delay(500)
                    seekTo(settings.lastPlayedPosition)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerViewModel", "Error restoring last played song: ${e.message}", e)
        }
    }
}
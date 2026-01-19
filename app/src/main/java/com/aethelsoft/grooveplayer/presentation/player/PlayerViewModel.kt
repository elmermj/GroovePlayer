package com.aethelsoft.grooveplayer.presentation.player

import android.app.Application
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
import com.aethelsoft.grooveplayer.domain.usecase.player_category.SetMuteUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.SetVolumeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val setMuteUseCase: SetMuteUseCase
) : AndroidViewModel(application) {

    val currentSong: StateFlow<Song?> = observePlayerStateUseCase.observeCurrentSong().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val isPlaying: StateFlow<Boolean> = observePlayerStateUseCase.observeIsPlaying().stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val position: StateFlow<Long> = observePlayerStateUseCase.observePosition().stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val duration: StateFlow<Long> = observePlayerStateUseCase.observeDuration().stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val queue: StateFlow<List<Song>> = observePlayerStateUseCase.observeQueue().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val shuffle: StateFlow<Boolean> = observePlayerStateUseCase.observeShuffle().stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val repeat: StateFlow<RepeatMode> = observePlayerStateUseCase.observeRepeat().stateIn(viewModelScope, SharingStarted.Eagerly, RepeatMode.OFF)
    val volume: StateFlow<Float> = observePlayerStateUseCase.observeVolume().stateIn(viewModelScope, SharingStarted.Eagerly, 0.5f)
    val isFullScreenPlayerOpened: StateFlow<Boolean> = observePlayerStateUseCase.observeIsFullScreenPlayerOpen().stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )
    val isPlayerMuted: StateFlow<Boolean> = observePlayerStateUseCase.observeIsPlayerMuted().stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val audioAmplitude: StateFlow<Float> = observePlayerStateUseCase.observeAudioAmplitude().stateIn(viewModelScope, SharingStarted.Eagerly, 0f)
    val audioVisualization: StateFlow<AudioVisualizationData> = observePlayerStateUseCase.observeAudioVisualization().stateIn(
        viewModelScope, SharingStarted.Eagerly, AudioVisualizationData()
    )



    fun setQueue(songs: List<Song>, startIndex: Int = 0, isEndlessQueue: Boolean = false) = viewModelScope.launch {
        queueUseCase(songs, startIndex, isEndlessQueue)
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
}
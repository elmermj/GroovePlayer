package com.aethelsoft.grooveplayer.data.player

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Collections.emptyList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExoPlayer implementation of PlayerRepository.
 * This is the data layer implementation that should not depend on UseCases.
 * It can depend on other repositories.
 */
@Singleton
class ExoPlayerManager @Inject constructor(
    private val ctx: Context,
    private val playbackHistoryRepository: PlaybackHistoryRepository,
    private val userRepository: UserRepository
) : PlayerRepository {

    private val player: ExoPlayer = ExoPlayer.Builder(ctx).build()
    private val audioManager: AudioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentSong = MutableStateFlow<Song?>(null)
    private val _isPlaying = MutableStateFlow(false)
    private val _position = MutableStateFlow(0L)
    private val _duration = MutableStateFlow(0L)
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    private val _shuffle = MutableStateFlow(false)
    private val _repeat = MutableStateFlow(RepeatMode.OFF)
    private val _volume = MutableStateFlow(getCurrentVolume())
    private val _isFullScreenPlayerOpen = MutableStateFlow(false)
    private val _isPlayerMuted = MutableStateFlow(false)
    
    private var lastRecordedSongId: String? = null
    private var lastRecordedTimestamp: Long = 0L
    
    // Observe fade timer from user_category settings
    private var fadeTimerSeconds = 0
    private var isFading = false

    private val scope = CoroutineScope(Dispatchers.Main)
    
    private fun getCurrentVolume(): Float {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (maxVolume > 0) currentVolume.toFloat() / maxVolume.toFloat() else 0f
    }

    init {
        // Observe fade timer from user_category settings
        scope.launch {
            userRepository.observeUserSettings().collect { settings ->
                fadeTimerSeconds = settings.fadeTimer
            }
        }
        
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // find matching Song in queue by uri
                val uri = mediaItem?.localConfiguration?.uri?.toString()
                val song = _queue.value.firstOrNull { it.uri == uri }
                _currentSong.value = song
                _duration.value = _currentSong.value?.durationMs ?: player.duration.coerceAtLeast(0L)
                
                // Record playback when song transitions and starts playing
                // Avoid duplicate records within 1 second
                if (song != null && player.isPlaying) {
                    val now = System.currentTimeMillis()
                    if (song.id != lastRecordedSongId || (now - lastRecordedTimestamp) > 1000) {
                        lastRecordedSongId = song.id
                        lastRecordedTimestamp = now
                        scope.launch(Dispatchers.IO) {
                            playbackHistoryRepository.recordPlayback(song)
                        }
                    }
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                _duration.value = player.duration.coerceAtLeast(0L)
                // When playback ends, update isPlaying state
                if (state == Player.STATE_ENDED) {
                    _isPlaying.value = false
                }
            }
        })

        // position ticker
        scope.launch {
            while (true) {
                _position.value = player.currentPosition
                kotlinx.coroutines.delay(300)
            }
        }
        
        // volume ticker - listen to system volume changes
        scope.launch {
            while (true) {
                val newVolume = getCurrentVolume()
                if (kotlin.math.abs(newVolume - _volume.value) > 0.01f) {
                    _volume.value = newVolume
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    private fun prepareFromSong(song: Song) {
        player.setMediaItem(MediaItem.fromUri(Uri.parse(song.uri)))
        player.prepare()
        _currentSong.value = song
        _duration.value = song.durationMs
    }

    override suspend fun setQueue(songs: List<Song>, startIndex: Int) {
        _queue.value = songs
        player.clearMediaItems()
        songs.forEach { s -> player.addMediaItem(MediaItem.fromUri(Uri.parse(s.uri))) }
        player.prepare()
        val idx = startIndex.coerceIn(0, songs.lastIndex.coerceAtLeast(0))
        player.seekTo(idx, 0)
        val song = songs.getOrNull(idx)
        _currentSong.value = song
        _duration.value = song?.durationMs ?: 0L
        player.playWhenReady = true
    }

    override suspend fun play() {
        // If the song has finished (reached the end), seek to the beginning
        val duration = player.duration
        val currentPosition = player.currentPosition
        if (duration > 0 && currentPosition >= duration - 100) { // 100ms threshold to account for timing differences
            player.seekTo(0)
        }
        player.playWhenReady = true
        player.play()
    }

    override suspend fun pause() {
        player.playWhenReady = false
        player.pause()
    }

    override suspend fun playSong(song: Song) {
        prepareFromSong(song)
        player.playWhenReady = true
        player.play()
    }

    override suspend fun next() {
        if (fadeTimerSeconds > 0 && _isPlaying.value) {
            applyFadeOut()
        }
        player.seekToNext()
        player.play()
        if (fadeTimerSeconds > 0) {
            applyFadeIn()
        }
    }

    override suspend fun previous() {
        // if position > 3s, restart; else previous track
        if (player.currentPosition > 3000) {
            player.seekTo(0)
            player.play()
        } else {
            if (fadeTimerSeconds > 0 && _isPlaying.value) {
                applyFadeOut()
            }
            player.seekToPrevious()
            player.play()
            if (fadeTimerSeconds > 0) {
                applyFadeIn()
            }
        }
    }
    
    /**
     * Applies fade-out effect by gradually reducing volume.
     * Duration is controlled by fadeTimerSeconds from user_category settings.
     */
    private suspend fun applyFadeOut() {
        if (isFading) return // Prevent concurrent fades
        isFading = true
        
        val originalVolume = player.volume
        val steps = 20 // Number of volume reduction steps
        val delayMs = (fadeTimerSeconds * 1000L) / steps
        
        for (i in steps downTo 0) {
            val newVolume = originalVolume * (i.toFloat() / steps)
            player.volume = newVolume
            delay(delayMs)
        }
        
        isFading = false
    }
    
    /**
     * Applies fade-in effect by gradually increasing volume.
     * Duration is controlled by fadeTimerSeconds from user_category settings.
     */
    private suspend fun applyFadeIn() {
        if (isFading) return // Prevent concurrent fades
        isFading = true
        
        val targetVolume = if (_isPlayerMuted.value) 0f else 1f
        val steps = 20 // Number of volume increase steps
        val delayMs = (fadeTimerSeconds * 1000L) / steps
        
        for (i in 0..steps) {
            val newVolume = targetVolume * (i.toFloat() / steps)
            player.volume = newVolume
            delay(delayMs)
        }
        
        isFading = false
    }

    override suspend fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        _position.value = player.currentPosition
    }

    override suspend fun setShuffle(enable: Boolean) {
        _shuffle.value = enable
        player.shuffleModeEnabled = enable
    }

    override suspend fun setRepeat(mode: RepeatMode) {
        _repeat.value = mode
        player.repeatMode = when (mode) {
            RepeatMode.OFF -> ExoPlayer.REPEAT_MODE_OFF
            RepeatMode.ONE -> ExoPlayer.REPEAT_MODE_ONE
            RepeatMode.ALL -> ExoPlayer.REPEAT_MODE_ALL
        }
    }

    override suspend fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = (clampedVolume * maxVolume).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        _volume.value = clampedVolume
    }

    override suspend fun setFullScreenPlayerOpen(isOpen: Boolean) {
        if (_isFullScreenPlayerOpen.value != isOpen) {
            _isFullScreenPlayerOpen.value = isOpen
        }
    }

    override suspend fun setMute(mute: Boolean) {
        _isPlayerMuted.value = mute
        if (!isFading) {  // Don't interfere with fade effects
            player.volume = if (mute) 0f else 1f
        }
    }


    override fun observeCurrentSong(): Flow<Song?> = _currentSong.asStateFlow()
    override fun observeIsPlaying(): Flow<Boolean> = _isPlaying.asStateFlow()
    override fun observePosition(): Flow<Long> = _position.asStateFlow()
    override fun observeDuration(): Flow<Long> = _duration.asStateFlow()
    override fun observeQueue(): Flow<List<Song>> = _queue.asStateFlow()
    override fun observeShuffle(): Flow<Boolean> = _shuffle.asStateFlow()
    override fun observeRepeat(): Flow<RepeatMode> = _repeat.asStateFlow()
    override fun observeVolume(): Flow<Float> = _volume.asStateFlow()
    override fun observeIsFullScreenPlayerOpen(): Flow<Boolean> = _isFullScreenPlayerOpen.asStateFlow()
    override fun observeIsPlayerMuted(): Flow<Boolean> = _isPlayerMuted.asStateFlow()
}
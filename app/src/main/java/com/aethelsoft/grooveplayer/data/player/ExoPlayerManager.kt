package com.aethelsoft.grooveplayer.data.player

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.domain.model.VisualizationMode
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import com.aethelsoft.grooveplayer.services.MusicPlaybackServiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections.emptyList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Audio visualization data containing frequency and stereo information
 */
data class AudioVisualizationData(
    val bass: Float = 0f,        // Low frequencies (20-250 Hz)
    val mid: Float = 0f,         // Mid frequencies / Voice (250-4000 Hz)
    val treble: Float = 0f,      // High frequencies (4000-20000 Hz)
    val stereoBalance: Float = 0f, // -1.0 (left) to 1.0 (right)
    val beat: Float = 0f,        // Beat detection intensity
    val overall: Float = 0f      // Overall amplitude
)

/**
 * ExoPlayer implementation of PlayerRepository.
 * This is the data layer implementation that should not depend on UseCases.
 * It can depend on other repositories.
 */
@Singleton
class ExoPlayerManager @OptIn(UnstableApi::class)
@Inject constructor(
    private val ctx: Context,
    private val playbackHistoryRepository: PlaybackHistoryRepository,
    private val userRepository: UserRepository,
    private val serviceManager: MusicPlaybackServiceManager,
    private val equalizerManager: EqualizerManager,
    private val equalizerRepository: com.aethelsoft.grooveplayer.domain.repository.EqualizerRepository
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
    private val _audioVisualization = MutableStateFlow(AudioVisualizationData())
    
    private var lastRecordedSongId: String? = null
    private var lastRecordedTimestamp: Long = 0L
    private var visualizer: Visualizer? = null
    private var lastBeatEnergy = 0.0
    private var beatHistory = mutableListOf<Double>()

    /** Throttle visualization updates to ~15fps on mid-range devices to reduce glow redraw cost. */
    @Volatile
    private var lastVisualizationEmitTime = 0L
    @Volatile
    private var pendingVisualization: AudioVisualizationData? = null
    private companion object {
        const val VISUALIZATION_EMIT_INTERVAL_MS = 66L
    }
    
    // Observe fade timer from user_category settings
    private var fadeTimerSeconds = 0
    private var isFading = false
    private var visualizationMode: VisualizationMode = VisualizationMode.REAL_TIME
    
    // Endless queue feature
    private var isEndlessQueue = false
    private var allAvailableSongs = listOf<Song>()

    private val scope = CoroutineScope(Dispatchers.Main)
    
    private fun getCurrentVolume(): Float {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (maxVolume > 0) currentVolume.toFloat() / maxVolume.toFloat() else 0f
    }

    val livePlayer: ExoPlayer
        get() = this.player

    init {
        // Observe fade timer and visualization mode from user_category settings
        scope.launch {
            try {
                userRepository.observeUserSettings().collect { settings ->
                    fadeTimerSeconds = settings.fadeTimer
                    visualizationMode = settings.visualizationMode
                }
            } catch (e: Exception) {
                android.util.Log.e("ExoPlayerManager", "Failed to observe user settings", e)
                fadeTimerSeconds = 0  // Default to no fade
            }
        }
        
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                
                // Start foreground service when playback begins
                if (isPlaying) {
                    serviceManager.startService()
                    _currentSong.value?.let { song ->
                        recordPlaybackIfNeeded(song)
                    }
                    
                    // Ensure visualizer is initialized once we actually have playback.
                    // On some devices the audioSessionId is 0 at app startup and only
                    // becomes valid after playback begins, which previously left us
                    // stuck on the fallback "template" visualization.
                    initializeVisualizerIfNeeded(reason = "onIsPlayingChanged")
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // find matching Song in queue by uri
                val uri = mediaItem?.localConfiguration?.uri?.toString()
                val song = _queue.value.firstOrNull { it.uri == uri }
                _currentSong.value = song
                _duration.value = _currentSong.value?.durationMs ?: player.duration.coerceAtLeast(0L)
                
                // Record playback when song transitions
                if (song != null && player.isPlaying) {
                    recordPlaybackIfNeeded(song)
                }
                
                // Check if we need to extend the queue for endless playback
                if (isEndlessQueue && song != null) {
                    checkAndExtendQueue(song)
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
        
        // Save player state periodically
        scope.launch {
            var lastSaveTime = 0L
            while (true) {
                val now = System.currentTimeMillis()
                if (now - lastSaveTime >= 5000) { // Save every 5 seconds
                    _currentSong.value?.let { song ->
                        if (_position.value > 0) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    userRepository.updatePlayerState(
                                        songId = song.id,
                                        position = _position.value,
                                        shuffle = _shuffle.value,
                                        repeat = _repeat.value.name,
                                        queueSongIds = _queue.value.map { it.id },
                                        queueStartIndex = _queue.value.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                                        isEndlessQueue = isEndlessQueue
                                    )
                                    lastSaveTime = now
                                } catch (e: Exception) {
                                    android.util.Log.e("ExoPlayerManager", "Error saving player state: ${e.message}", e)
                                }
                            }
                        }
                    }
                }
                kotlinx.coroutines.delay(1000)
            }
        }
        
        // Also save on pause/stop
        scope.launch {
            _isPlaying.collect { isPlaying ->
                if (!isPlaying && _currentSong.value != null && _position.value > 0) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val song = _currentSong.value!!
                            userRepository.updatePlayerState(
                                songId = song.id,
                                position = _position.value,
                                shuffle = _shuffle.value,
                                repeat = _repeat.value.name,
                                queueSongIds = _queue.value.map { it.id },
                                queueStartIndex = _queue.value.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                                isEndlessQueue = isEndlessQueue
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("ExoPlayerManager", "Error saving player state on pause: ${e.message}", e)
                        }
                    }
                }
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
        
        // Start simulation loop for visualization.
        // This drives the visualization only when:
        // - Mode is SIMULATED, or
        // - Mode is REAL_TIME but the Visualizer is not available (fallback).
        scope.launch {
            while (true) {
                val useSimulation = when (visualizationMode) {
                    VisualizationMode.OFF -> false
                    VisualizationMode.SIMULATED -> true
                    VisualizationMode.REAL_TIME -> visualizer == null || visualizer?.enabled != true
                }

                if (_isPlaying.value && useSimulation) {
                    // Animated pulsing effect based on time for fallback
                    val time = System.currentTimeMillis() / 150.0
                    val bassPulse = kotlin.math.sin(time * 0.8) * 0.5 + 0.5
                    val midPulse = kotlin.math.sin(time * 1.2) * 0.5 + 0.5
                    val treblePulse = kotlin.math.sin(time * 2.0) * 0.3 + 0.3
                    val stereo = kotlin.math.sin(time * 0.5).toFloat()
                    val beat = if (kotlin.math.sin(time * 2.0) > 0.8) 0.8f else 0.2f
                    
                    _audioVisualization.value = AudioVisualizationData(
                        bass = bassPulse.toFloat().coerceIn(0f, 1f),
                        mid = midPulse.toFloat().coerceIn(0f, 1f),
                        treble = treblePulse.toFloat().coerceIn(0f, 1f),
                        stereoBalance = stereo.coerceIn(-1f, 1f),
                        beat = beat,
                        overall = ((bassPulse + midPulse + treblePulse) / 3.0).toFloat().coerceIn(0f, 1f)
                    )
                } else if (!_isPlaying.value) {
                    _audioVisualization.value = AudioVisualizationData()
                }
                delay(VISUALIZATION_EMIT_INTERVAL_MS)
            }
        }
        
        // Try to initialize audio visualizer for real waveform data once the player is ready.
        // We also retry later from onIsPlayingChanged when playback actually starts.
        scope.launch(Dispatchers.Main) {
            delay(1000) // Initial attempt after player construction
            initializeVisualizerIfNeeded(reason = "init_delay")
        }
    }

    /**
     * Initialize the Visualizer + Equalizer pipeline if we have a valid audioSessionId
     * and haven't successfully initialized yet.
     *
     * This is idempotent and can be safely called from multiple places.
     */
    private fun initializeVisualizerIfNeeded(reason: String) {
        // If we already have an enabled visualizer, nothing to do.
        val existing = visualizer
        if (existing != null && existing.enabled) {
            android.util.Log.d("ExoPlayerManager", "Visualizer already initialized (reason=$reason)")
            return
        }

        try {
            val sessionId = player.audioSessionId
            android.util.Log.d("ExoPlayerManager", "Attempting to initialize Visualizer (reason=$reason) with session ID: $sessionId")

            if (sessionId == 0) {
                // This often happens before playback starts; we'll retry on next call.
                android.util.Log.w("ExoPlayerManager", "⚠️ Audio session ID is 0, deferring Visualizer initialization (reason=$reason)")
                return
            }

            // Initialize equalizer with the same audio session ID
            val equalizerInitialized = equalizerManager.initialize(sessionId)
            if (equalizerInitialized) {
                android.util.Log.d("ExoPlayerManager", "✅ Equalizer initialized successfully (reason=$reason)")
                // Load saved equalizer settings (defer to avoid blocking initialization)
                scope.launch(Dispatchers.IO) {
                    try {
                        // Small delay to ensure database is ready
                        kotlinx.coroutines.delay(100)
                        equalizerRepository.loadSettings()
                    } catch (e: Exception) {
                        android.util.Log.e("ExoPlayerManager", "Error loading equalizer settings: ${e.message}", e)
                    }
                }
            } else {
                android.util.Log.w("ExoPlayerManager", "⚠️ Equalizer initialization failed (reason=$reason)")
            }

            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            vis: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            waveform?.let { data ->
                                // Extract stereo balance from waveform
                                // Waveform contains interleaved left/right samples
                                var leftSum = 0.0
                                var rightSum = 0.0
                                val halfSize = data.size / 2

                                for (i in 0 until halfSize) {
                                    val leftValue = kotlin.math.abs(data[i].toInt() - 128).toDouble()
                                    leftSum += leftValue * leftValue
                                }
                                for (i in halfSize until data.size) {
                                    val rightValue = kotlin.math.abs(data[i].toInt() - 128).toDouble()
                                    rightSum += rightValue * rightValue
                                }

                                        val leftRms = sqrt(leftSum / halfSize)
                                        val rightRms = sqrt(rightSum / (data.size - halfSize))
                                        val totalRms = leftRms + rightRms
                                        
                                        // Calculate stereo balance: -1 (left) to 1 (right)
                                        // Use RMS-based channel energy difference, then:
                                        // - apply a small dead-zone to avoid jitter around center
                                        // - apply a non-linear curve for slightly more dramatic separation
                                        val rawStereo = if (totalRms > 0.01) {
                                            ((rightRms - leftRms) / totalRms).toFloat().coerceIn(-1f, 1f)
                                        } else {
                                            0f
                                        }
                                        
                                        // Dead-zone: treat tiny differences as center
                                        val stereoWithDeadZone = if (kotlin.math.abs(rawStereo) < 0.05f) {
                                            0f
                                        } else {
                                            rawStereo
                                        }
                                        
                                        // Slightly dramatic curve: abs(x)^0.7 keeps sign, pushes small
                                        // imbalances farther from 0 while keeping -1..1 bounds.
                                        val emphasizedStereo = if (stereoWithDeadZone != 0f) {
                                            val sign = kotlin.math.sign(stereoWithDeadZone)
                                            val boosted = kotlin.math.abs(stereoWithDeadZone.toDouble())
                                                .pow(0.7)
                                                .toFloat()
                                            (sign * boosted).coerceIn(-1f, 1f)
                                        } else {
                                            0f
                                        }

                                // Calculate overall amplitude
                                val overallRms = sqrt((leftSum + rightSum) / data.size)
                                val rawNormalized = (overallRms / 128.0).coerceIn(0.0, 1.0)
                                val logScaled = kotlin.math.log10(1.0 + rawNormalized * 9.0) / kotlin.math.log10(10.0)
                                val overall = logScaled.toFloat()

                                        // Moderate stereo smoothing for smooth but responsive panning.
                                        // Bias slightly toward the new emphasized value so that the
                                        // glow feels more reactive to pan changes.
                                        val currentData = pendingVisualization ?: _audioVisualization.value
                                        val smoothedStereo = (currentData.stereoBalance * 0.4f +
                                                emphasizedStereo * 0.6f).coerceIn(-1f, 1f)

                                val next = currentData.copy(
                                    stereoBalance = smoothedStereo,
                                    overall = overall
                                )
                                pendingVisualization = next
                                val now = System.currentTimeMillis()
                                if (now - lastVisualizationEmitTime >= VISUALIZATION_EMIT_INTERVAL_MS) {
                                    _audioVisualization.value = next
                                    lastVisualizationEmitTime = now
                                    pendingVisualization = null
                                }
                            }
                        }

                        override fun onFftDataCapture(
                            vis: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            fft?.let { data ->
                                // Android FFT format: [DC, Nyquist, real1, imag1, real2, imag2, ...]
                                // Index 0: DC component, Index 1: Nyquist frequency
                                // Then alternating real/imaginary pairs

                                val numFrequencies = data.size / 2
                                val magnitudes = FloatArray(numFrequencies)

                                // DC component (index 0)
                                magnitudes[0] = kotlin.math.abs(data[0].toFloat())

                                // Process real/imaginary pairs (indices 2 onwards)
                                for (i in 1 until numFrequencies) {
                                    val real = data[i * 2].toFloat()
                                    val imag = data[i * 2 + 1].toFloat()
                                    magnitudes[i] = sqrt((real * real + imag * imag).toDouble()).toFloat()
                                }

                                // Frequency bins mapping
                                // Android's samplingRate parameter is unreliable, use standard audio rate
                                // Most music is 44.1kHz or 48kHz - using 44100 as safe default
                                val audioSampleRate = 44100f
                                val nyquist = audioSampleRate / 2f  // 22050 Hz
                                val binFreq = nyquist / numFrequencies

                                // Extract frequency bands
                                var bassSum = 0.0
                                var bassCount = 0
                                var midSum = 0.0
                                var midCount = 0
                                var trebleSum = 0.0
                                var trebleCount = 0

                                for (i in magnitudes.indices) {
                                    val freq = i * binFreq
                                    val magnitude = magnitudes[i].toDouble()

                                    when {
                                        freq < 250 -> { // Bass: 20-250 Hz
                                            bassSum += magnitude
                                            bassCount++
                                        }
                                        freq < 4000 -> { // Mid/Voice: 250-4000 Hz
                                            midSum += magnitude
                                            midCount++
                                        }
                                        freq < 20000 -> { // Treble: 4000-20000 Hz
                                            trebleSum += magnitude
                                            trebleCount++
                                        }
                                    }
                                }

                                // Normalize frequency bands
                                val bassAvg = if (bassCount > 0) (bassSum / bassCount) / 128.0 else 0.0
                                val midAvg = if (midCount > 0) (midSum / midCount) / 128.0 else 0.0
                                val trebleAvg = if (trebleCount > 0) (trebleSum / trebleCount) / 128.0 else 0.0

                                // Apply logarithmic scaling for better visualization
                                val bassScaled = kotlin.math.log10(1.0 + bassAvg * 9.0) / kotlin.math.log10(10.0)
                                val midScaled = kotlin.math.log10(1.0 + midAvg * 9.0) / kotlin.math.log10(10.0)
                                val trebleScaled = kotlin.math.log10(1.0 + trebleAvg * 9.0) / kotlin.math.log10(10.0)

                                // Beat detection: sudden increase in bass energy
                                val currentEnergy = bassSum / bassCount.coerceAtLeast(1)
                                beatHistory.add(currentEnergy)
                                if (beatHistory.size > 30) { // Keep ~0.7 second of history (shorter = more sensitive)
                                    beatHistory.removeAt(0)
                                }

                                val avgEnergy = if (beatHistory.isNotEmpty()) {
                                    beatHistory.average()
                                } else currentEnergy

                                val energyRatio = if (avgEnergy > 0.01) {
                                    (currentEnergy / avgEnergy).coerceIn(0.0, 4.0)
                                } else 0.0

                                // More sensitive beat detection with amplification
                                val beatIntensity = if (energyRatio > 1.15) { // Lower threshold (was 1.3)
                                    // Amplified mapping: 1.15->0.0, 2.5->1.0
                                    val normalized = ((energyRatio - 1.15) / 1.35).coerceIn(0.0, 1.0)
                                    // Apply power curve for more dramatic beats
                                    normalized.pow(0.7) // Power < 1 = more sensitive
                                } else {
                                    0.0
                                }

                                // Balanced smoothing: responsive but not jittery
                                val currentData = pendingVisualization ?: _audioVisualization.value

                                // Bass: smooth but responsive
                                val smoothedBass = currentData.bass * 0.5f + bassScaled.toFloat() * 0.5f

                                // Mid: balanced for vocals
                                val smoothedMid = currentData.mid * 0.45f + midScaled.toFloat() * 0.55f

                                // Treble: responsive for highs with slight smoothing
                                val smoothedTreble = currentData.treble * 0.35f + trebleScaled.toFloat() * 0.65f

                                // Beat: fast rise, moderate fall for punchy but smooth impact
                                val smoothedBeat = if (beatIntensity.toFloat() > currentData.beat) {
                                    // Rise fast
                                    currentData.beat * 0.25f + beatIntensity.toFloat() * 0.75f
                                } else {
                                    // Fall moderately
                                    currentData.beat * 0.6f + beatIntensity.toFloat() * 0.4f
                                }

                                val next = currentData.copy(
                                    bass = smoothedBass.coerceIn(0f, 1f),
                                    mid = smoothedMid.coerceIn(0f, 1f),
                                    treble = smoothedTreble.coerceIn(0f, 1f),
                                    beat = smoothedBeat.coerceIn(0f, 1f)
                                )
                                pendingVisualization = next
                                val now = System.currentTimeMillis()
                                if (now - lastVisualizationEmitTime >= VISUALIZATION_EMIT_INTERVAL_MS) {
                                    _audioVisualization.value = next
                                    lastVisualizationEmitTime = now
                                    pendingVisualization = null
                                }

                                // Debug logging with raw and processed values
                                // if (bassCount > 0 || midCount > 0 || trebleCount > 0) {
                                //     android.util.Log.v(
                                //         "ExoPlayerManager",
                                //         "🎵 Bass: ${"%.2f".format(smoothedBass)} (bins:$bassCount) | Mid: ${"%.2f".format(smoothedMid)} (bins:$midCount) | Treble: ${"%.2f".format(smoothedTreble)} (bins:$trebleCount) | Stereo: ${"%.2f".format(currentData.stereoBalance)} | Beat: ${"%.2f".format(smoothedBeat)}"
                                //     )
                                //     android.util.Log.v(
                                //         "ExoPlayerManager",
                                //         "   Raw→ BassAvg: ${"%.3f".format(bassAvg)} | MidAvg: ${"%.3f".format(midAvg)} | TrebleAvg: ${"%.3f".format(trebleAvg)} | BinFreq: ${"%.1f".format(binFreq)}Hz | NumBins: $numFrequencies"
                                //     )
                                // }
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 1.3.toInt(), // Maximum rate for lowest latency
                    true,
                    true // Enable FFT capture
                )
                enabled = true
            }
            android.util.Log.d("ExoPlayerManager", "✅ Visualizer initialized successfully - real waveform with frequency analysis (reason=$reason)")
        } catch (e: SecurityException) {
            android.util.Log.e("ExoPlayerManager", "❌ RECORD_AUDIO permission not granted - using simulation (reason=$reason): ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerManager", "❌ Failed to initialize visualizer - using simulation (reason=$reason): ${e.message}")
        }
    }

    /**
     * Records playback to history, avoiding duplicates within 1 second.
     * @param force If true, records even if same song was recently recorded (for manual song selection)
     */
    private fun recordPlaybackIfNeeded(song: Song, force: Boolean = false) {
        val now = System.currentTimeMillis()
        val shouldRecord = force || song.id != lastRecordedSongId || (now - lastRecordedTimestamp) > 1000
        
        if (shouldRecord) {
            lastRecordedSongId = song.id
            lastRecordedTimestamp = now
            scope.launch(Dispatchers.IO) {
                try {
                    playbackHistoryRepository.recordPlayback(song)
                    android.util.Log.d("ExoPlayerManager", "Recorded playback: ${song.title} by ${song.artist} (force=$force)")
                } catch (e: Exception) {
                    android.util.Log.e("ExoPlayerManager", "Failed to record playback", e)
                }
            }
        } else {
            android.util.Log.d("ExoPlayerManager", "Skipped duplicate recording for: ${song.title}")
        }
    }
    
    private suspend fun prepareFromSong(song: Song) {
        withContext(Dispatchers.Main) {
            player.setMediaItem(MediaItem.fromUri(Uri.parse(song.uri)))
            player.prepare()
        }
        _currentSong.value = song
        _duration.value = song.durationMs
    }

    override suspend fun setQueue(songs: List<Song>, startIndex: Int, isEndlessQueue: Boolean, autoPlay: Boolean) {
        this.isEndlessQueue = isEndlessQueue
        this.allAvailableSongs = songs
        
        _queue.value = songs
        
        // All ExoPlayer operations MUST run on Main thread
        withContext(Dispatchers.Main) {
            player.clearMediaItems()
            songs.forEach { s -> player.addMediaItem(MediaItem.fromUri(s.uri.toUri())) }
            player.prepare()
            val idx = startIndex.coerceIn(0, songs.lastIndex.coerceAtLeast(0))
            player.seekTo(idx, 0)
            player.playWhenReady = autoPlay
        }
        
        val idx = startIndex.coerceIn(0, songs.lastIndex.coerceAtLeast(0))
        val song = songs.getOrNull(idx)
        _currentSong.value = song
        _duration.value = song?.durationMs ?: 0L
        
        // Record playback immediately when queue is set (force=true for manual selection)
        // Only record if auto-playing
        if (autoPlay) {
            song?.let { recordPlaybackIfNeeded(it, force = true) }
        }
        
        // Save player state
        scope.launch(Dispatchers.IO) {
            try {
                userRepository.updatePlayerState(
                    songId = song?.id,
                    position = player.currentPosition,
                    shuffle = _shuffle.value,
                    repeat = _repeat.value.name,
                    queueSongIds = songs.map { it.id },
                    queueStartIndex = idx,
                    isEndlessQueue = isEndlessQueue
                )
            } catch (e: Exception) {
                android.util.Log.e("ExoPlayerManager", "Error saving player state: ${e.message}", e)
            }
        }
    }

    /**
     * Checks if we're approaching the end of the queue and extends it with random songs.
     * Called when isEndlessQueue is true.
     */
    private fun checkAndExtendQueue(currentSong: Song) {
        val currentQueue = _queue.value
        val currentIndex = currentQueue.indexOfFirst { it.id == currentSong.id }
        val remainingSongs = currentQueue.size - currentIndex - 1
        
        // Extend queue when we're within 2 songs of the end
        if (remainingSongs <= 2) {
            scope.launch(Dispatchers.IO) {
                extendQueue()
            }
        }
    }
    
    /**
     * Extends the queue with 10 random songs from allAvailableSongs.
     * Avoids adding songs that are already in the current queue.
     */
    private suspend fun extendQueue() {
        val currentQueue = _queue.value
        if (currentQueue.isEmpty() || allAvailableSongs.isEmpty()) return
        
        // Get songs not in current queue
        val availableSongs = allAvailableSongs.filter { song ->
            !currentQueue.any { it.id == song.id }
        }
        
        // If we've played all songs, reset and use all available songs
        val songsToPickFrom = availableSongs.ifEmpty {
            allAvailableSongs
        }
        
        // Pick 10 random songs
        val randomSongs = songsToPickFrom.shuffled().take(10)
        
        // Add to queue
        val newQueue = currentQueue + randomSongs
        _queue.value = newQueue
        
        // Add to ExoPlayer - MUST run on Main thread
        withContext(Dispatchers.Main) {
            randomSongs.forEach { song ->
                player.addMediaItem(MediaItem.fromUri(Uri.parse(song.uri)))
            }
        }
        
        android.util.Log.d("ExoPlayerManager", "Extended queue with ${randomSongs.size} songs. Total queue: ${newQueue.size}")
    }

    override suspend fun play() {
        withContext(Dispatchers.Main) {
            // If the song has finished (reached the end), seek to the beginning
            val duration = player.duration
            val currentPosition = player.currentPosition
            if (duration > 0 && currentPosition >= duration - 100) { // 100ms threshold to account for timing differences
                player.seekTo(0)
            }
            player.playWhenReady = true
            player.play()
        }
    }

    override suspend fun pause() {
        withContext(Dispatchers.Main) {
            player.playWhenReady = false
            player.pause()
        }
    }

    override suspend fun playSong(song: Song) {
        prepareFromSong(song)
        withContext(Dispatchers.Main) {
            player.playWhenReady = true
            player.play()
        }
    }

    override suspend fun next() {
        if (fadeTimerSeconds > 0 && _isPlaying.value) {
            applyFadeOut()
        }
        withContext(Dispatchers.Main) {
            player.seekToNext()
            player.play()
        }
        if (fadeTimerSeconds > 0) {
            applyFadeIn()
        }
    }

    override suspend fun previous() {
        val currentPos = withContext(Dispatchers.Main) {
            player.currentPosition
        }
        
        // if position > 3s, restart; else previous track
        if (currentPos > 3000) {
            withContext(Dispatchers.Main) {
                player.seekTo(0)
                player.play()
            }
        } else {
            if (fadeTimerSeconds > 0 && _isPlaying.value) {
                applyFadeOut()
            }
            withContext(Dispatchers.Main) {
                player.seekToPrevious()
                player.play()
            }
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
        
        withContext(Dispatchers.Main) {
            val originalVolume = player.volume
            val steps = 20 // Number of volume reduction steps
            val delayMs = (fadeTimerSeconds * 1000L) / steps
            
            for (i in steps downTo 0) {
                val newVolume = originalVolume * (i.toFloat() / steps)
                player.volume = newVolume
                delay(delayMs)
            }
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
        
        withContext(Dispatchers.Main) {
            val targetVolume = if (_isPlayerMuted.value) 0f else 1f
            val steps = 20 // Number of volume increase steps
            val delayMs = (fadeTimerSeconds * 1000L) / steps
            
            for (i in 0..steps) {
                val newVolume = targetVolume * (i.toFloat() / steps)
                player.volume = newVolume
                delay(delayMs)
            }
        }
        
        isFading = false
    }

    override suspend fun seekTo(positionMs: Long) {
        withContext(Dispatchers.Main) {
            player.seekTo(positionMs.coerceAtLeast(0L))
            _position.value = player.currentPosition
        }
    }

    override suspend fun setShuffle(enable: Boolean) {
        _shuffle.value = enable
        withContext(Dispatchers.Main) {
            player.shuffleModeEnabled = enable
        }
    }

    override suspend fun setRepeat(mode: RepeatMode) {
        _repeat.value = mode
        withContext(Dispatchers.Main) {
            player.repeatMode = when (mode) {
                RepeatMode.OFF -> ExoPlayer.REPEAT_MODE_OFF
                RepeatMode.ONE -> ExoPlayer.REPEAT_MODE_ONE
                RepeatMode.ALL -> ExoPlayer.REPEAT_MODE_ALL
            }
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
            withContext(Dispatchers.Main) {
                player.volume = if (mute) 0f else 1f
            }
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
    override fun observeAudioAmplitude(): Flow<Float> = _audioVisualization.asStateFlow().map { it.overall }
    override fun observeAudioVisualization(): Flow<AudioVisualizationData> = _audioVisualization.asStateFlow()
}
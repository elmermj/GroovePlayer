package com.aethelsoft.grooveplayer.data.player

import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.aethelsoft.grooveplayer.data.playback.PremiumStreamSignals
import com.aethelsoft.grooveplayer.data.playback.StreamPlaybackCache
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.domain.model.VisualizationMode
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import com.aethelsoft.grooveplayer.domain.playback.PlaybackDropReason
import com.aethelsoft.grooveplayer.domain.playback.STREAM_PREFETCH_MAX_BYTES
import com.aethelsoft.grooveplayer.domain.playback.STREAM_REFRESH_FAILED_MESSAGE
import com.aethelsoft.grooveplayer.domain.playback.StreamPlaybackFault
import com.aethelsoft.grooveplayer.domain.playback.StreamRecoveryStep
import com.aethelsoft.grooveplayer.domain.playback.nextCloudStreamUri
import com.aethelsoft.grooveplayer.domain.playback.playbackStreamSongId
import com.aethelsoft.grooveplayer.domain.playback.streamRecoveryStep
import com.aethelsoft.grooveplayer.domain.usecase.player_category.ResolvePlaybackSourceUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.ResolvedPlayback
import com.aethelsoft.grooveplayer.domain.usecase.player_category.ResolvedQueue
import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import com.aethelsoft.grooveplayer.services.MusicPlaybackServiceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    val bass: Float = 0f,        // max(sub 20–60 Hz, bass 60–250 Hz)
    val mid: Float = 0f,         // 0.4*lowMid (250–1000) + 0.6*presence (1000–3000, voice)
    val treble: Float = 0f,      // 0.35*highMid (3000–6000) + 0.65*treble (6000–16000)
    val stereoBalance: Float = 0f, // -1.0 (left) to 1.0 (right)
    val beat: Float = 0f,        // Onset intensity; decays between hits
    val overall: Float = 0f      // Overall amplitude
)

/**
 * ExoPlayer implementation of PlayerRepository.
 * This is the data layer implementation that should not depend on UseCases.
 * It can depend on other repositories.
 *
 * ExoPlayer must be created and touched on the main looper. Hilt may construct
 * this @Singleton from a background dispatcher on cold start, so construction
 * and player API calls always hop to Dispatchers.Main.immediate.
 */
@Singleton
class ExoPlayerManager @OptIn(UnstableApi::class)
@Inject constructor(
    private val ctx: Context,
    private val playbackHistoryRepository: PlaybackHistoryRepository,
    private val userRepository: UserRepository,
    private val serviceManager: MusicPlaybackServiceManager,
    private val equalizerManager: EqualizerManager,
    private val equalizerRepository: com.aethelsoft.grooveplayer.domain.repository.EqualizerRepository,
    private val resolvePlaybackSource: ResolvePlaybackSourceUseCase,
    private val streamPlaybackCache: StreamPlaybackCache,
    private val premiumStreamSignals: PremiumStreamSignals,
) : PlayerRepository {

    private val player: ExoPlayer = createPlayerOnMainThread()
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
    private val bandAnalyzer = AudioBandAnalyzer()

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

    /** Queue order from before shuffle was turned on; kept in sync with edits made while shuffled. Null when shuffle is off. */
    private var preShuffleOrder: MutableList<Song>? = null
    private var allAvailableSongs = listOf<Song>()
    /** One re-fetch of an expired or rejected playback stream_url for the current song. */
    private var streamRetrySongId: String? = null
    /** Refresh already failed for this song. Further errors stay quiet until the user retries. */
    private var streamRefreshGaveUpSongId: String? = null
    private val _streamRefreshFailures = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private var prefetchJob: Job? = null
    private var prefetchUri: String? = null

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, error ->
            android.util.Log.e("ExoPlayerManager", "Player coroutine failed", error)
        },
    )

    /**
     * ExoPlayer binds to the current thread's looper at construction time.
     * Always build on the main looper, even if Hilt injects this singleton off-main.
     */
    private fun createPlayerOnMainThread(): ExoPlayer {
        fun build(): ExoPlayer {
            // Cloud items are groove-playback://. StreamPlaybackCache resolves the
            // signed URL, caches the bytes, and leaves local files uncached.
            // Range on that playback URL is allowed. Backup restore uses another client.
            return ExoPlayer.Builder(ctx)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(streamPlaybackCache.playbackDataSourceFactory),
                )
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    /* handleAudioFocus = */ true,
                )
                // Pauses when headphones / Bluetooth audio disconnect (AUDIO_BECOMING_NOISY).
                .setHandleAudioBecomingNoisy(true)
                .build()
        }
        return if (Looper.myLooper() == Looper.getMainLooper()) {
            build()
        } else {
            runBlocking(Dispatchers.Main.immediate) { build() }
        }
    }

    private inline fun runOnMainThread(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            runBlocking(Dispatchers.Main.immediate) { block() }
        }
    }

    private fun attachPlayerListener() {
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
                val mime = mediaItem?.localConfiguration?.mimeType
                val isM4a = isM4AMimeType(mime) || (song != null && isM4A(song.uri))
                // M4A + Equalizer causes severe distortion; release Equalizer only. Keep Visualizer for Dynamic viz.
                if (isM4a) {
                    releaseEqualizerOnly()
                }
                // Re-init: for M4A we init Visualizer only; for others we init both
                initializeVisualizerIfNeeded(reason = "media_item_transition")
                _duration.value = _currentSong.value?.durationMs ?: player.duration.coerceAtLeast(0L)

                // Record playback when song transitions
                if (song != null && player.isPlaying) {
                    recordPlaybackIfNeeded(song)
                }

                // Check if we need to extend the queue for endless playback
                if (isEndlessQueue && song != null) {
                    checkAndExtendQueue(song)
                }
                clearStreamRecovery()
                scheduleNextCloudPrefetch()
            }

            override fun onPlaybackStateChanged(state: Int) {
                _duration.value = player.duration.coerceAtLeast(0L)
                // When playback ends, update isPlaying state
                if (state == Player.STATE_ENDED) {
                    _isPlaying.value = false
                }
                if (state == Player.STATE_READY) {
                    scheduleNextCloudPrefetch()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val song = _currentSong.value ?: return
                if (playbackStreamSongId(song.uri) == null) return
                if (streamRefreshGaveUpSongId == song.id) return
                when (streamRecoveryStep(faultOf(error), alreadyRefreshed = streamRetrySongId == song.id)) {
                    StreamRecoveryStep.REFRESH_URL -> {
                        streamRetrySongId = song.id
                        android.util.Log.i(
                            "ExoPlayerManager",
                            "Refreshing stream URL for ${song.id} at ${player.currentPosition}ms",
                        )
                        resumeStreamAfterRefresh()
                    }
                    StreamRecoveryStep.SKIP_ABSENT -> {
                        clearStreamRecovery()
                        if (player.hasNextMediaItem()) {
                            player.seekToNext()
                            player.prepare()
                            player.playWhenReady = true
                        } else {
                            player.playWhenReady = false
                        }
                    }
                    StreamRecoveryStep.HOLD_FOR_PREMIUM -> {
                        // Resolver already asked for the Premium snackbar. Keep the queue.
                        streamRefreshGaveUpSongId = song.id
                        player.playWhenReady = false
                    }
                    StreamRecoveryStep.SURFACE_FAILURE -> {
                        streamRefreshGaveUpSongId = song.id
                        player.playWhenReady = false
                        _streamRefreshFailures.tryEmit(STREAM_REFRESH_FAILED_MESSAGE)
                    }
                    StreamRecoveryStep.IGNORE -> Unit
                }
            }
        })
    }
    
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
        
        // Listener + player touches must run on the player's application thread (main).
        runOnMainThread {
            attachPlayerListener()
        }

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
        
        // Time-based template used for SIMULATED, and as a fallback when Dynamic
        // mode is selected but the Visualizer is not capturing. This loop is not
        // live FFT analysis and must not be treated as real-time audio.
        scope.launch {
            while (true) {
                val useSimulation = when (visualizationMode) {
                    VisualizationMode.OFF -> false
                    VisualizationMode.SIMULATED -> true
                    VisualizationMode.REAL_TIME -> visualizer == null || visualizer?.enabled != true
                }

                if (_isPlaying.value && useSimulation) {
                    bandAnalyzer.reset()
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
                    bandAnalyzer.reset()
                    _audioVisualization.value = AudioVisualizationData()
                }
                delay(VISUALIZATION_EMIT_INTERVAL_MS)
            }
        }
        
        // Try to initialize audio visualizer for real waveform data once the player is ready.
        // We also retry later from onIsPlayingChanged when playback actually starts.
        scope.launch(Dispatchers.Main.immediate) {
            delay(1000) // Initial attempt after player construction
            initializeVisualizerIfNeeded(reason = "init_delay")
        }
    }

    /** M4A + Equalizer/Visualizer causes severe distortion (e.g. Samsung Galaxy). Bypass effects for M4A. */
    private fun isM4AMimeType(mime: String?): Boolean {
        if (mime == null) return false
        val lower = mime.lowercase()
        return lower == "audio/mp4" || lower == "audio/x-m4a" || lower.contains("m4a") || lower == "audio/aac"
    }

    private fun isM4A(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            when (uri.scheme) {
                "content" -> {
                    val mime = ctx.contentResolver.getType(uri)
                        ?: run {
                            // Fallback: ContentResolver can return null for some system files (e.g. Samsung)
                            try {
                                MediaMetadataRetriever().use { retriever ->
                                    retriever.setDataSource(ctx, uri)
                                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                                }
                            } catch (e: Exception) { null }
                        }
                    isM4AMimeType(mime)
                }
                "file" -> uri.lastPathSegment?.lowercase()?.endsWith(".m4a") == true
                else -> false
            }
        } catch (e: Exception) { false }
    }

    private fun isCurrentTrackM4A(): Boolean {
        val mime = player.currentMediaItem?.localConfiguration?.mimeType
        if (isM4AMimeType(mime)) return true
        return _currentSong.value?.let { isM4A(it.uri) } == true
    }

    /** Release Equalizer only (M4A bypass). Visualizer stays for Dynamic visualization. */
    private fun releaseEqualizerOnly() {
        try {
            equalizerManager.release()
            android.util.Log.d("ExoPlayerManager", "Released Equalizer (M4A bypass)")
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerManager", "Error releasing equalizer", e)
        }
    }

    /**
     * Initialize the Visualizer + Equalizer pipeline if we have a valid audioSessionId.
     * For M4A: Visualizer only (Equalizer causes distortion on some devices).
     * For other formats: both Equalizer and Visualizer.
     */
    private fun initializeVisualizerIfNeeded(reason: String) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            scope.launch(Dispatchers.Main.immediate) {
                initializeVisualizerIfNeeded(reason)
            }
            return
        }
        val isM4a = isCurrentTrackM4A()
        if (isM4a) {
            releaseEqualizerOnly()
        }
        // If we already have an enabled visualizer, only init Equalizer if switching from M4A to non-M4A
        val existing = visualizer
        if (existing != null && existing.enabled) {
            if (!isM4a) {
                val sessionId = player.audioSessionId
                if (sessionId != 0) {
                    equalizerManager.initialize(sessionId)
                    scope.launch(Dispatchers.IO) {
                        kotlinx.coroutines.delay(100)
                        equalizerRepository.loadSettings()
                    }
                }
            }
            return
        }

        try {
            val sessionId = player.audioSessionId
            android.util.Log.d("ExoPlayerManager", "Attempting to initialize Visualizer (reason=$reason) with session ID: $sessionId, isM4A=$isM4a")

            if (sessionId == 0) {
                android.util.Log.w("ExoPlayerManager", "⚠️ Audio session ID is 0, deferring Visualizer initialization (reason=$reason)")
                return
            }

            // Equalizer causes M4A distortion on some devices (e.g. Samsung); skip for M4A only
            if (!isM4a) {
                val equalizerInitialized = equalizerManager.initialize(sessionId)
                if (equalizerInitialized) {
                    android.util.Log.d("ExoPlayerManager", "✅ Equalizer initialized (reason=$reason)")
                    scope.launch(Dispatchers.IO) {
                        kotlinx.coroutines.delay(100)
                        equalizerRepository.loadSettings()
                    }
                } else {
                    android.util.Log.w("ExoPlayerManager", "⚠️ Equalizer init failed (reason=$reason)")
                }
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
                                val numFrequencies = data.size / 2
                                if (numFrequencies <= 0) return@let
                                val magnitudes = FloatArray(numFrequencies)
                                magnitudes[0] = kotlin.math.abs(data[0].toFloat())
                                for (i in 1 until numFrequencies) {
                                    val realIndex = i * 2
                                    val imagIndex = realIndex + 1
                                    if (imagIndex >= data.size) break
                                    val real = data[realIndex].toFloat()
                                    val imag = data[imagIndex].toFloat()
                                    magnitudes[i] = sqrt((real * real + imag * imag).toDouble()).toFloat()
                                }

                                // samplingRate is milliHertz; 0 falls back to 44.1 kHz inside visualizerSampleRateHz.
                                val sampleRateHz = visualizerSampleRateHz(samplingRate)
                                val binHz = (sampleRateHz / 2f) / numFrequencies
                                val analyzed = bandAnalyzer.analyze(
                                    magnitudes = magnitudes,
                                    binHz = binHz,
                                    nowMs = System.currentTimeMillis(),
                                )
                                val currentData = pendingVisualization ?: _audioVisualization.value
                                val next = currentData.copy(
                                    bass = analyzed.bass,
                                    mid = analyzed.mid,
                                    treble = analyzed.treble,
                                    beat = analyzed.beat,
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
    
    /**
     * Local file, or `groove-playback://` which [PlaybackStreamResolver] turns
     * into `stream_url` on open. Range GET on that playback URL is allowed.
     * Backup restore still must not Range-GET R2.
     */
    private fun buildMediaItem(uri: Uri): MediaItem {
        val mimeType = if (uri.scheme == "content") {
            try {
                ctx.contentResolver.getType(uri)
                    ?: MediaMetadataRetriever().use { retriever ->
                        retriever.setDataSource(ctx, uri)
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                    }
            } catch (e: Exception) { null }
        } else null
        return if (mimeType != null && mimeType.startsWith("audio/")) {
            MediaItem.Builder().setUri(uri).setMimeType(mimeType).build()
        } else {
            MediaItem.fromUri(uri)
        }
    }

    private suspend fun prepareFromSong(song: Song) {
        withContext(Dispatchers.Main.immediate) {
            player.setMediaItem(buildMediaItem(Uri.parse(song.uri)))
            player.prepare()
        }
        _currentSong.value = song
        _duration.value = song.durationMs
    }

    override fun observePremiumStreamRequired(): Flow<Unit> = premiumStreamSignals.events

    override fun observeStreamRefreshFailure(): Flow<String> = _streamRefreshFailures.asSharedFlow()

    private fun clearStreamRecovery() {
        streamRetrySongId = null
        streamRefreshGaveUpSongId = null
    }

    private fun faultOf(error: PlaybackException): StreamPlaybackFault {
        val messages = ArrayList<String>()
        var http: Int? = null
        generateSequence<Throwable>(error) { it.cause }.forEach { throwable ->
            throwable.message?.let { messages += it }
            if (http == null && throwable is HttpDataSource.InvalidResponseCodeException) {
                http = throwable.responseCode
            }
        }
        val io = error.errorCode in
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED..PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE
        return StreamPlaybackFault(
            httpStatus = http,
            detail = messages.joinToString(" "),
            ioFailure = io,
        )
    }

    /**
     * Re-open the current item so [PlaybackStreamResolver] mints a new stream URL.
     * The queue, the position, and the signed-in session stay.
     */
    private fun resumeStreamAfterRefresh() {
        val index = player.currentMediaItemIndex.coerceAtLeast(0)
        val position = player.currentPosition.coerceAtLeast(0L)
        player.seekTo(index, position)
        player.prepare()
        player.playWhenReady = true
    }

    /**
     * Download a short prefix of the next cloud item. Local files are skipped.
     * One track at a time, capped by [STREAM_PREFETCH_MAX_BYTES].
     */
    private fun scheduleNextCloudPrefetch() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            scope.launch(Dispatchers.Main.immediate) { scheduleNextCloudPrefetch() }
            return
        }
        val uris = _queue.value.map { it.uri }
        val index = player.currentMediaItemIndex
        val next = nextCloudStreamUri(uris, index, _repeat.value)
        if (next == null) {
            prefetchJob?.cancel()
            streamPlaybackCache.cancelPrefetch()
            prefetchUri = null
            return
        }
        if (next == prefetchUri && prefetchJob?.isActive == true) return
        prefetchJob?.cancel()
        streamPlaybackCache.cancelPrefetch()
        prefetchUri = next
        prefetchJob = scope.launch(Dispatchers.IO) {
            val job = coroutineContext[Job]
            streamPlaybackCache.prefetchPrefix(next, STREAM_PREFETCH_MAX_BYTES) {
                job?.isActive != false
            }
        }
    }

    private fun notePremiumGate(resolved: ResolvedQueue) {
        if (resolved.premiumStreamBlocked) premiumStreamSignals.notifyRequired()
    }

    private fun notePremiumGate(resolved: ResolvedPlayback) {
        if (resolved is ResolvedPlayback.Dropped && resolved.reason == PlaybackDropReason.NOT_ENTITLED) {
            premiumStreamSignals.notifyRequired()
        }
    }

    override suspend fun setQueue(songs: List<Song>, startIndex: Int, isEndlessQueue: Boolean, autoPlay: Boolean) {
        // Resolve before any MediaItem is opened: local, entitled stream ticket, or purge on 404.
        val resolved = try {
            resolvePlaybackSource.resolveQueue(songs, startIndex)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerManager", "Playback resolve failed; keeping the local queue", e)
            ResolvedQueue(
                songs = songs,
                startIndex = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0)),
            )
        }
        notePremiumGate(resolved)
        val playable = resolved.songs
        val resolvedStart = resolved.startIndex
        if (playable.isEmpty()) {
            preShuffleOrder = null
            setQueueInternal(emptyList(), 0, isEndlessQueue = false, autoPlay = false)
            return
        }
        if (_shuffle.value && playable.size > 1) {
            // Shuffle is on: the chosen song plays first, the rest follow in a shuffled (real) order.
            val start = resolvedStart.coerceIn(0, playable.lastIndex)
            val chosen = playable[start]
            val rest = playable.filterIndexed { i, _ -> i != start }.shuffled()
            preShuffleOrder = playable.toMutableList()
            return setQueueInternal(listOf(chosen) + rest, 0, isEndlessQueue, autoPlay)
        }
        preShuffleOrder = if (_shuffle.value) playable.toMutableList() else null
        setQueueInternal(playable, resolvedStart, isEndlessQueue, autoPlay)
    }

    private suspend fun setQueueInternal(songs: List<Song>, startIndex: Int, isEndlessQueue: Boolean, autoPlay: Boolean) {
        this.isEndlessQueue = isEndlessQueue
        this.allAvailableSongs = songs
        
        _queue.value = songs
        
        // All ExoPlayer operations MUST run on Main thread
        val savedPosition = withContext(Dispatchers.Main.immediate) {
            player.clearMediaItems()
            if (songs.isEmpty()) {
                player.stop()
                player.playWhenReady = false
                serviceManager.stopService()
                0L
            } else {
                songs.forEach { s -> player.addMediaItem(buildMediaItem(s.uri.toUri())) }
                player.prepare()
                val idx = startIndex.coerceIn(0, songs.lastIndex)
                player.seekTo(idx, 0)
                player.playWhenReady = autoPlay
                player.currentPosition
            }
        }
        
        val song = if (songs.isEmpty()) {
            null
        } else {
            songs.getOrNull(startIndex.coerceIn(0, songs.lastIndex))
        }
        _currentSong.value = song
        _duration.value = song?.durationMs ?: 0L
        _position.value = 0L
        
        // Record playback immediately when queue is set (force=true for manual selection)
        // Only record if auto-playing
        if (autoPlay) {
            song?.let { recordPlaybackIfNeeded(it, force = true) }
        }
        
        // Save player state (position already read on main)
        scope.launch(Dispatchers.IO) {
            try {
                userRepository.updatePlayerState(
                    songId = song?.id,
                    position = savedPosition,
                    shuffle = _shuffle.value,
                    repeat = _repeat.value.name,
                    queueSongIds = songs.map { it.id },
                    queueStartIndex = if (songs.isEmpty()) 0 else startIndex.coerceIn(0, songs.lastIndex),
                    isEndlessQueue = isEndlessQueue
                )
            } catch (e: Exception) {
                android.util.Log.e("ExoPlayerManager", "Error saving player state: ${e.message}", e)
            }
        }
        scheduleNextCloudPrefetch()
    }

    override suspend fun skipToQueueIndex(index: Int) {
        val q = _queue.value
        if (index !in q.indices) return
        withContext(Dispatchers.Main.immediate) {
            if (index >= player.mediaItemCount) return@withContext
            player.seekTo(index, 0)
            player.playWhenReady = true
        }
        // onMediaItemTransition updates _currentSong; set eagerly so UI reacts instantly.
        _currentSong.value = q[index]
        _duration.value = q[index].durationMs
        _position.value = 0L
        recordPlaybackIfNeeded(q[index], force = true)
        persistQueueState()
    }

    override suspend fun moveQueueItem(from: Int, to: Int) {
        val q = _queue.value
        if (from !in q.indices || to !in q.indices || from == to) return
        val moved = withContext(Dispatchers.Main.immediate) {
            if (from >= player.mediaItemCount || to >= player.mediaItemCount) return@withContext false
            player.moveMediaItem(from, to)
            true
        }
        if (!moved) return
        val newQueue = q.toMutableList().apply { add(to, removeAt(from)) }
        _queue.value = newQueue
        preShuffleOrder?.let { orig -> placeAfterPredecessor(orig, newQueue, to) }
        persistQueueState()
        scheduleNextCloudPrefetch()
    }

    override suspend fun removeQueueItem(index: Int): Boolean {
        val q = _queue.value
        if (index !in q.indices) return false
        val removed = withContext(Dispatchers.Main.immediate) {
            if (index >= player.mediaItemCount || index == player.currentMediaItemIndex) return@withContext false
            player.removeMediaItem(index)
            true
        }
        if (!removed) return false
        _queue.value = q.toMutableList().apply { removeAt(index) }
        preShuffleOrder?.let { orig ->
            val i = orig.indexOfFirst { it.id == q[index].id }
            if (i >= 0) orig.removeAt(i)
        }
        persistQueueState()
        scheduleNextCloudPrefetch()
        return true
    }

    override suspend fun insertQueueItem(index: Int, song: Song) {
        val resolved = resolvePlaybackSource.resolveOne(song)
        notePremiumGate(resolved)
        val playable = (resolved as? ResolvedPlayback.Playable)?.song ?: return
        val q = _queue.value
        val at = index.coerceIn(0, q.size)
        val inserted = withContext(Dispatchers.Main.immediate) {
            if (at > player.mediaItemCount) return@withContext false
            player.addMediaItem(at, buildMediaItem(playable.uri.toUri()))
            true
        }
        if (!inserted) return
        val newQueue = q.toMutableList().apply { add(at, playable) }
        _queue.value = newQueue
        preShuffleOrder?.let { orig -> placeAfterPredecessor(orig, newQueue, at) }
        persistQueueState()
        scheduleNextCloudPrefetch()
    }


    override suspend fun playNext(song: Song) {
        val q = _queue.value
        val currentIndex = withContext(Dispatchers.Main.immediate) {
            player.currentMediaItemIndex.coerceAtLeast(0)
        }
        // Empty queue: start playback with this song alone.
        if (q.isEmpty()) {
            setQueue(listOf(song), startIndex = 0, isEndlessQueue = false, autoPlay = true)
            return
        }
        val insertAt = (currentIndex + 1).coerceIn(0, q.size)
        // If already next, leave as-is; if elsewhere in queue, move after current.
        val existing = q.indexOfFirst { it.id == song.id }
        if (existing == insertAt) return
        if (existing >= 0) {
            // Don't move the currently playing item.
            if (existing == currentIndex) return
            moveQueueItem(existing, if (existing < insertAt) insertAt - 1 else insertAt)
            return
        }
        insertQueueItem(insertAt, song)
    }

    /**
     * Mirrors an edit made while shuffled into [orig]: the song at [queueIndex] in [queue] is placed right after
     * the song that precedes it in the queue, so reorders carry over when shuffle is turned off.
     */
    private fun placeAfterPredecessor(orig: MutableList<Song>, queue: List<Song>, queueIndex: Int) {
        val song = queue.getOrNull(queueIndex) ?: return
        val existing = orig.indexOfFirst { it.id == song.id }
        if (existing >= 0) orig.removeAt(existing)
        val predecessor = queue.getOrNull(queueIndex - 1)
        val predIndex = predecessor?.let { p -> orig.indexOfFirst { it.id == p.id } } ?: -1
        orig.add((predIndex + 1).coerceIn(0, orig.size), song)
    }

    /** Shuffle only the songs after the current one; the current song keeps playing where it is. */
    private suspend fun applyShuffleOrder() {
        val q = _queue.value
        if (q.isEmpty()) return
        preShuffleOrder = q.toMutableList()
        replaceUpcoming { upcoming -> upcoming.shuffled() }
    }

    /**
     * Restore [preShuffleOrder] (removals and moves already applied) and keep the current song playing.
     * When the songs before the current one are still in their original order, only the upcoming tracks are
     * rewritten so playback is not interrupted. Otherwise the whole queue is replaced and playback seeks
     * back to the current song in that restored order.
     */
    private suspend fun restoreUnshuffledOrder() {
        val orig = preShuffleOrder?.toList()
        preShuffleOrder = null
        val q = _queue.value
        if (orig.isNullOrEmpty() || q.isEmpty()) return
        val currentId = _currentSong.value?.id ?: return
        val origIndex = orig.indexOfFirst { it.id == currentId }
        if (origIndex < 0) return
        if (q.map { it.id } == orig.map { it.id }) return

        val queueIndex = q.indexOfFirst { it.id == currentId }
        val headMatches = queueIndex == origIndex &&
            q.take(queueIndex).map { it.id } == orig.take(origIndex).map { it.id }
        if (headMatches) {
            val desiredIds = orig.drop(origIndex + 1).map { it.id }
            val desiredSet = desiredIds.toSet()
            replaceUpcoming { upcoming ->
                val byId = upcoming.associateBy { it.id }
                val ordered = desiredIds.mapNotNull { byId[it] }
                val extras = upcoming.filter { it.id !in desiredSet }
                ordered + extras
            }
            return
        }

        val position = _position.value
        val playing = _isPlaying.value
        _queue.value = orig
        withContext(Dispatchers.Main.immediate) {
            player.setMediaItems(
                orig.map { buildMediaItem(it.uri.toUri()) },
                origIndex,
                position.coerceAtLeast(0L),
            )
            player.prepare()
            player.playWhenReady = playing
        }
        _queue.value = orig
        _currentSong.value = orig[origIndex]
        _position.value = position
        _duration.value = orig[origIndex].durationMs
        persistQueueState()
        scheduleNextCloudPrefetch()
    }

    /** Swap the items after the current one without interrupting playback. */
    private suspend fun replaceUpcoming(transform: (List<Song>) -> List<Song>) {
        val q = _queue.value
        val newQueue = withContext(Dispatchers.Main.immediate) {
            val count = player.mediaItemCount
            val idx = player.currentMediaItemIndex
            if (count != q.size || idx !in q.indices) return@withContext null
            val head = q.take(idx + 1)
            val upcoming = transform(q.drop(idx + 1))
            if (idx + 1 < count) player.removeMediaItems(idx + 1, count)
            if (upcoming.isNotEmpty()) {
                player.addMediaItems(idx + 1, upcoming.map { buildMediaItem(it.uri.toUri()) })
            }
            head + upcoming
        } ?: return
        _queue.value = newQueue
        persistQueueState()
        scheduleNextCloudPrefetch()
    }

    private fun persistQueueState() {
        val q = _queue.value
        val current = _currentSong.value
        val position = _position.value
        scope.launch(Dispatchers.IO) {
            try {
                userRepository.updatePlayerState(
                    songId = current?.id,
                    position = position,
                    shuffle = _shuffle.value,
                    repeat = _repeat.value.name,
                    queueSongIds = q.map { it.id },
                    queueStartIndex = q.indexOfFirst { it.id == current?.id }.coerceAtLeast(0),
                    isEndlessQueue = isEndlessQueue
                )
            } catch (e: Exception) {
                android.util.Log.e("ExoPlayerManager", "Error saving queue state: ${e.message}", e)
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
        val randomSongs = songsToPickFrom.shuffled().take(10).mapNotNull { song ->
            val resolved = resolvePlaybackSource.resolveOne(song)
            notePremiumGate(resolved)
            (resolved as? ResolvedPlayback.Playable)?.song
        }
        if (randomSongs.isEmpty()) return
        
        // Add to queue
        val newQueue = currentQueue + randomSongs
        _queue.value = newQueue
        preShuffleOrder?.addAll(randomSongs)
        
        // Add to ExoPlayer - MUST run on Main thread
        withContext(Dispatchers.Main.immediate) {
            randomSongs.forEach { song ->
                player.addMediaItem(buildMediaItem(Uri.parse(song.uri)))
            }
        }
        
        android.util.Log.d("ExoPlayerManager", "Extended queue with ${randomSongs.size} songs. Total queue: ${newQueue.size}")
        scheduleNextCloudPrefetch()
    }

    override suspend fun play() {
        clearStreamRecovery()
        withContext(Dispatchers.Main.immediate) {
            // If the song has finished (reached the end), seek to the beginning
            val duration = player.duration
            val currentPosition = player.currentPosition
            if (duration > 0 && currentPosition >= duration - 100) { // 100ms threshold to account for timing differences
                player.seekTo(0)
            }
            // An expired stream leaves the player idle. Prepare again so play
            // re-opens the item and can mint a fresh URL.
            if (player.playbackState == Player.STATE_IDLE && player.mediaItemCount > 0) {
                player.prepare()
            }
            player.playWhenReady = true
            player.play()
        }
    }

    override suspend fun pause() {
        withContext(Dispatchers.Main.immediate) {
            player.playWhenReady = false
            player.pause()
        }
    }

    override suspend fun playSong(song: Song) {
        val resolved = resolvePlaybackSource.resolveOne(song)
        notePremiumGate(resolved)
        val playable = (resolved as? ResolvedPlayback.Playable)?.song ?: return
        prepareFromSong(playable)
        withContext(Dispatchers.Main.immediate) {
            player.playWhenReady = true
            player.play()
        }
    }

    override suspend fun next() {
//        if (fadeTimerSeconds > 0 && _isPlaying.value) {
//            applyFadeOut()
//        }
        withContext(Dispatchers.Main.immediate) {
            player.seekToNext()
            player.play()
        }
//        if (fadeTimerSeconds > 0) {
//            applyFadeIn()
//        }
    }

    override suspend fun previous() {
        val currentPos = withContext(Dispatchers.Main.immediate) {
            player.currentPosition
        }
        
        // if position > 3s, restart; else previous track
        if (currentPos > 3000) {
            withContext(Dispatchers.Main.immediate) {
                player.seekTo(0)
                player.play()
            }
        } else {
//            if (fadeTimerSeconds > 0 && _isPlaying.value) {
//                applyFadeOut()
//            }
            withContext(Dispatchers.Main.immediate) {
                player.seekToPrevious()
                player.play()
            }
//            if (fadeTimerSeconds > 0) {
//                applyFadeIn()
//            }
        }
    }
    
    /**
     * Applies fade-out effect by gradually reducing volume.
     * Duration is controlled by fadeTimerSeconds from user_category settings.
     */
    private suspend fun applyFadeOut() {
        if (isFading) return // Prevent concurrent fades
        isFading = true
        
        withContext(Dispatchers.Main.immediate) {
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
        
        withContext(Dispatchers.Main.immediate) {
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
        clearStreamRecovery()
        withContext(Dispatchers.Main.immediate) {
            player.seekTo(positionMs.coerceAtLeast(0L))
            _position.value = player.currentPosition
        }
    }

    override suspend fun setShuffle(enable: Boolean, reorderQueue: Boolean) {
        val wasEnabled = _shuffle.value
        _shuffle.value = enable
        withContext(Dispatchers.Main.immediate) {
            // The queue itself holds the real playback order; ExoPlayer's hidden shuffle order stays off.
            player.shuffleModeEnabled = false
        }
        if (reorderQueue && enable != wasEnabled) {
            if (enable) applyShuffleOrder() else restoreUnshuffledOrder()
        } else if (!reorderQueue) {
            preShuffleOrder = if (enable) _queue.value.toMutableList() else null
        }
        withContext(Dispatchers.IO) {
            userRepository.updateRepeatAndShuffle(_shuffle.value, _repeat.value.name)
        }
    }

    override suspend fun setRepeat(mode: RepeatMode) {
        _repeat.value = mode
        withContext(Dispatchers.Main.immediate) {
            player.repeatMode = when (mode) {
                RepeatMode.OFF -> ExoPlayer.REPEAT_MODE_OFF
                RepeatMode.ONE -> ExoPlayer.REPEAT_MODE_ONE
                RepeatMode.ALL -> ExoPlayer.REPEAT_MODE_ALL
            }
        }
        withContext(Dispatchers.IO) {
            userRepository.updateRepeatAndShuffle(_shuffle.value, _repeat.value.name)
        }
        scheduleNextCloudPrefetch()
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
            withContext(Dispatchers.Main.immediate) {
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
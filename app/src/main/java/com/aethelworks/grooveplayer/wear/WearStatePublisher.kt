package com.aethelworks.grooveplayer.wear

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aethelworks.grooveplayer.data.artwork.SongArtworkFiles
import com.aethelworks.grooveplayer.data.backup.GrooveDownloadsLocator
import com.aethelworks.grooveplayer.domain.artwork.EmbeddedArtworkKeys
import com.aethelworks.grooveplayer.domain.repository.PlayerRepository
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pushes `/grooveplayer/state` while a watch node is connected.
 * The player singleton is created on the first publish, not when this object is.
 */
@Singleton
class WearStatePublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val player: Lazy<PlayerRepository>,
    private val artworkFiles: Lazy<SongArtworkFiles>,
    private val library: Lazy<GrooveDownloadsLocator>,
) : CapabilityClient.OnCapabilityChangedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val started = AtomicBoolean(false)
    private val sendMutex = Mutex()

    @Volatile private var watchConnected = false
    @Volatile private var latest: WearPlaybackSample? = null
    private var lastSent: WearPlaybackSample? = null
    private var lastSentAtMs = 0L
    private var cachedArtSongId: String? = null
    private var cachedArt: ByteArray? = null

    fun start() {
        if (!started.compareAndSet(false, true)) return
        val clients = runCatching { dataClient to capabilityClient }
        if (clients.isFailure) {
            started.set(false)
            Log.w(TAG, "Wearable data layer is unavailable", clients.exceptionOrNull())
            return
        }
        runCatching {
            capabilityClient.addListener(
                this,
                Uri.parse("wear://*/${WearProtocol.CAPABILITY_WATCH}"),
                CapabilityClient.FILTER_REACHABLE,
            )
        }.onFailure { error ->
            Log.w(TAG, "Watch capability listener was not registered", error)
        }
        scope.launch {
            refreshWatch()
            observePlayer()
        }
    }

    fun noteWatchReachable() {
        watchConnected = true
    }

    fun publishNow() {
        if (!started.get()) start()
        scope.launch {
            val sample = latest ?: readSample()
            sendLocked(sample, force = true)
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        if (capabilityInfo.name != WearProtocol.CAPABILITY_WATCH) return
        val connected = capabilityInfo.nodes.isNotEmpty()
        watchConnected = connected
        if (connected) publishNow()
    }

    private suspend fun observePlayer() {
        combine(
            player.get().observeCurrentSong(),
            player.get().observeIsPlaying(),
            player.get().observePosition(),
            player.get().observeDuration(),
        ) { song, playing, position, duration ->
            WearPlaybackSample(
                songId = song?.id,
                title = song?.title.orEmpty(),
                artist = song?.artist.orEmpty(),
                isPlaying = playing,
                positionMs = position.coerceAtLeast(0L),
                durationMs = duration.coerceAtLeast(0L),
                filePath = song?.filePath,
                artworkUrl = song?.artworkUrl,
            )
        }.collect { sample ->
            latest = sample
            sendLocked(sample, force = false)
        }
    }

    private suspend fun readSample(): WearPlaybackSample {
        val repository = player.get()
        val song = repository.observeCurrentSong().first()
        return WearPlaybackSample(
            songId = song?.id,
            title = song?.title.orEmpty(),
            artist = song?.artist.orEmpty(),
            isPlaying = repository.observeIsPlaying().first(),
            positionMs = repository.observePosition().first().coerceAtLeast(0L),
            durationMs = repository.observeDuration().first().coerceAtLeast(0L),
            filePath = song?.filePath,
            artworkUrl = song?.artworkUrl,
        )
    }

    private suspend fun sendLocked(sample: WearPlaybackSample, force: Boolean) {
        sendMutex.withLock {
            val now = System.currentTimeMillis()
            if (!force && !WearPublishPolicy.shouldPublish(watchConnected, lastSent, sample, now, lastSentAtMs)) {
                return
            }
            if (!watchConnected) {
                if (!force || !refreshWatch()) return
            }
            val jpeg = artworkFor(sample)
            try {
                put(sample, jpeg, now)
            } catch (error: Throwable) {
                if (jpeg == null) {
                    Log.w(TAG, "Wear state publish failed", error)
                    return
                }
                Log.w(TAG, "Wear state with artwork failed, retrying without it", error)
                cachedArt = null
                cachedArtSongId = sample.songId
                try {
                    put(sample, null, now)
                } catch (again: Throwable) {
                    Log.w(TAG, "Wear state publish failed", again)
                    return
                }
            }
            lastSent = sample
            lastSentAtMs = now
        }
    }

    private suspend fun artworkFor(sample: WearPlaybackSample): ByteArray? {
        val songId = sample.songId ?: return null
        if (songId == cachedArtSongId) return cachedArt
        val jpeg = withContext(Dispatchers.IO) { encode(sample) }
        cachedArtSongId = songId
        cachedArt = jpeg
        return jpeg
    }

    private suspend fun encode(sample: WearPlaybackSample): ByteArray? {
        val root = library.get().directory().absolutePath
        val hash = EmbeddedArtworkKeys.hashOf(sample.artworkUrl)
        val hashed = if (hash != null) artworkFiles.get().audioFile(hash) else null
        val file = when {
            hashed != null && hashed.isFile && library.get().isInside(hashed.absolutePath) -> hashed
            WearArtworkPolicy.allowed(sample.filePath, root) -> {
                File(sample.filePath!!).takeIf { it.isFile && it.canRead() }
            }
            else -> null
        } ?: return null
        return WearArtworkEncoder.jpegFromAudio(file)
    }

    private suspend fun put(sample: WearPlaybackSample, jpeg: ByteArray?, sampledAtMs: Long) {
        val request = PutDataMapRequest.create(WearProtocol.PATH_STATE).apply {
            dataMap.putString(WearProtocol.KEY_TITLE, sample.title)
            dataMap.putString(WearProtocol.KEY_ARTIST, sample.artist)
            dataMap.putBoolean(WearProtocol.KEY_IS_PLAYING, sample.isPlaying)
            dataMap.putLong(WearProtocol.KEY_POSITION_MS, sample.positionMs)
            dataMap.putLong(WearProtocol.KEY_DURATION_MS, sample.durationMs)
            dataMap.putLong(WearProtocol.KEY_UPDATED_AT_MS, sampledAtMs)
            if (jpeg != null && WearArtworkPolicy.fitsDataItem(jpeg.size)) {
                dataMap.putAsset(WearProtocol.KEY_ARTWORK, Asset.createFromBytes(jpeg))
            }
        }
        dataClient.putDataItem(request.asPutDataRequest().setUrgent()).await()
    }

    private suspend fun refreshWatch(): Boolean {
        return try {
            val info = capabilityClient.getCapability(
                WearProtocol.CAPABILITY_WATCH,
                CapabilityClient.FILTER_REACHABLE,
            ).await()
            val connected = info.nodes.isNotEmpty()
            watchConnected = connected
            connected
        } catch (error: Throwable) {
            Log.w(TAG, "Watch capability lookup failed", error)
            false
        }
    }

    private val dataClient by lazy { Wearable.getDataClient(context) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(context) }

    private companion object {
        const val TAG = "WearStatePublisher"
    }
}

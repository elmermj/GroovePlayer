package com.aethelsoft.grooveplayer.wear.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aethelsoft.grooveplayer.wear.WearProtocol
import com.aethelsoft.grooveplayer.wear.logic.WatchLink
import com.aethelsoft.grooveplayer.wear.logic.WatchSnapshot
import com.aethelsoft.grooveplayer.wear.logic.reduceWatchLink
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean

object WatchRemoteBus {
    private val snapshotState = MutableStateFlow<WatchSnapshot?>(null)
    private val artworkState = MutableStateFlow<ByteArray?>(null)
    private val linkState = MutableStateFlow(WatchLink())

    val snapshot: StateFlow<WatchSnapshot?> = snapshotState.asStateFlow()
    val artwork: StateFlow<ByteArray?> = artworkState.asStateFlow()
    val link: StateFlow<WatchLink> = linkState.asStateFlow()

    fun publish(snapshot: WatchSnapshot, artwork: ByteArray?) {
        val current = artworkState.value
        val kept = when {
            artwork == null -> null
            current != null && current.contentEquals(artwork) -> current
            else -> artwork
        }
        snapshotState.value = snapshot
        if (kept !== current) artworkState.value = kept
    }

    fun clearSnapshot() {
        snapshotState.value = null
        artworkState.value = null
    }

    fun applyNodes(reachablePhoneNodeIds: Set<String>): Boolean {
        val change = reduceWatchLink(linkState.value, reachablePhoneNodeIds)
        linkState.value = change.link
        return change.reconnected
    }
}

/**
 * Data Layer session for the watch. The activity and the listener service share it.
 * Preview composables do not touch this type.
 */
object WatchSession {
    private const val TAG = "WatchSession"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val started = AtomicBoolean(false)
    private lateinit var appContext: Context

    private val dataClient: DataClient by lazy { Wearable.getDataClient(appContext) }
    private val messageClient by lazy { Wearable.getMessageClient(appContext) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(appContext) }

    fun ensureStarted(context: Context) {
        appContext = context.applicationContext
        if (!started.compareAndSet(false, true)) return
        val registered = runCatching {
            dataClient.addListener { events -> handleData(events) }
            capabilityClient.addListener(
                { info -> onCapabilityChanged(info) },
                Uri.parse("wear://*/${WearProtocol.CAPABILITY_PHONE}"),
                CapabilityClient.FILTER_REACHABLE,
            )
        }
        if (registered.isFailure) {
            started.set(false)
            Log.w(TAG, "Wearable listeners were not registered", registered.exceptionOrNull())
        }
    }

    /** Screen came to the foreground: refresh the link and ask the phone for state. */
    fun onForeground(context: Context) {
        ensureStarted(context)
        if (!started.get()) return
        scope.launch { requestAndRefresh() }
    }

    fun send(path: String, payload: ByteArray = ByteArray(0)) {
        if (!started.get()) return
        scope.launch { deliver(path, payload) }
    }

    fun onCapabilityChanged(info: CapabilityInfo) {
        if (info.name != WearProtocol.CAPABILITY_PHONE) return
        val reconnected = WatchRemoteBus.applyNodes(info.nodes.map { it.id }.toSet())
        if (reconnected) send(WearProtocol.PATH_REQUEST_STATE)
    }

    fun handleData(events: DataEventBuffer) {
        val pending = mutableListOf<Pair<WatchSnapshot, Asset?>>()
        var cleared = false
        for (event in events) {
            val path = event.dataItem.uri.path ?: continue
            if (path != WearProtocol.PATH_STATE) continue
            if (event.type == DataEvent.TYPE_DELETED) {
                cleared = true
                continue
            }
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            pending += snapshotOf(map) to artworkAsset(map)
        }
        if (cleared && pending.isEmpty()) WatchRemoteBus.clearSnapshot()
        if (!started.get()) return
        pending.forEach { (snapshot, asset) ->
            scope.launch { WatchRemoteBus.publish(snapshot, asset?.let { readAsset(it) }) }
        }
    }

    private suspend fun requestAndRefresh() {
        refreshLink()
        deliver(WearProtocol.PATH_REQUEST_STATE, ByteArray(0))
        readCurrent()
    }

    private suspend fun refreshLink() {
        try {
            val info = capabilityClient.getCapability(
                WearProtocol.CAPABILITY_PHONE,
                CapabilityClient.FILTER_REACHABLE,
            ).await()
            val reconnected = WatchRemoteBus.applyNodes(info.nodes.map { it.id }.toSet())
            if (reconnected) deliver(WearProtocol.PATH_REQUEST_STATE, ByteArray(0))
        } catch (t: Throwable) {
            Log.w(TAG, "Phone capability lookup failed", t)
        }
    }

    private suspend fun readCurrent() {
        val buffer = try {
            dataClient.dataItems.await()
        } catch (t: Throwable) {
            Log.w(TAG, "Reading player state failed", t)
            return
        }
        try {
            for (item in buffer) {
                if (item.uri.path == WearProtocol.PATH_STATE) ingest(item)
            }
        } finally {
            buffer.release()
        }
    }

    private suspend fun ingest(item: DataItem) {
        val map = DataMapItem.fromDataItem(item).dataMap
        val asset = artworkAsset(map)
        WatchRemoteBus.publish(snapshotOf(map), asset?.let { readAsset(it) })
    }

    private fun snapshotOf(map: DataMap) = WatchSnapshot(
        title = map.getString(WearProtocol.KEY_TITLE, ""),
        artist = map.getString(WearProtocol.KEY_ARTIST, ""),
        isPlaying = map.getBoolean(WearProtocol.KEY_IS_PLAYING, false),
        positionMs = map.getLong(WearProtocol.KEY_POSITION_MS, 0L),
        durationMs = map.getLong(WearProtocol.KEY_DURATION_MS, 0L),
        updatedAtMs = map.getLong(WearProtocol.KEY_UPDATED_AT_MS, 0L),
    )

    private fun artworkAsset(map: DataMap): Asset? {
        if (!map.containsKey(WearProtocol.KEY_ARTWORK)) return null
        return runCatching { map.getAsset(WearProtocol.KEY_ARTWORK) }.getOrNull()
    }

    private suspend fun readAsset(asset: Asset): ByteArray? {
        return try {
            val response = dataClient.getFdForAsset(asset).await()
            response.inputStream.use { stream -> stream.readBytes() }.takeIf { it.isNotEmpty() }
        } catch (t: Throwable) {
            Log.w(TAG, "Artwork read failed", t)
            null
        }
    }

    private suspend fun deliver(path: String, payload: ByteArray) {
        try {
            val info = capabilityClient.getCapability(
                WearProtocol.CAPABILITY_PHONE,
                CapabilityClient.FILTER_REACHABLE,
            ).await()
            val nodes = info.nodes
            WatchRemoteBus.applyNodes(nodes.map { it.id }.toSet())
            for (node in nodes) {
                messageClient.sendMessage(node.id, path, payload).await()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Command $path failed", t)
        }
    }
}

package com.aethelworks.grooveplayer.wear.logic

/**
 * Connected means at least one reachable node advertising the phone capability.
 * [reconnected] is the edge the watch uses to send request_state again.
 */
data class WatchLink(val phoneNodeIds: Set<String> = emptySet()) {
    val connected: Boolean get() = phoneNodeIds.isNotEmpty()
}

data class WatchLinkChange(val link: WatchLink, val reconnected: Boolean)

fun reduceWatchLink(current: WatchLink, reachablePhoneNodeIds: Set<String>): WatchLinkChange {
    val next = WatchLink(reachablePhoneNodeIds)
    return WatchLinkChange(
        link = next,
        reconnected = !current.connected && next.connected,
    )
}

fun shouldRequestState(launch: Boolean, reconnected: Boolean): Boolean = launch || reconnected

data class WatchSnapshot(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long,
)

data class RemoteUiState(
    val connected: Boolean,
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val showTransport: Boolean,
)

fun remoteUi(snapshot: WatchSnapshot?, connected: Boolean, nowMs: Long): RemoteUiState {
    if (!connected) {
        return RemoteUiState(
            connected = false,
            title = "Disconnected",
            artist = "Open GroovePlayer on your phone",
            isPlaying = false,
            positionMs = 0L,
            durationMs = 0L,
            showTransport = false,
        )
    }
    if (snapshot == null) {
        return RemoteUiState(
            connected = true,
            title = "GroovePlayer",
            artist = "Waiting for the phone",
            isPlaying = false,
            positionMs = 0L,
            durationMs = 0L,
            showTransport = false,
        )
    }
    val position = interpolatedPositionMs(
        positionMs = snapshot.positionMs,
        isPlaying = snapshot.isPlaying,
        updatedAtMs = snapshot.updatedAtMs,
        nowMs = nowMs,
        durationMs = snapshot.durationMs,
    )
    return RemoteUiState(
        connected = true,
        title = snapshot.title.ifBlank { "Nothing playing" },
        artist = snapshot.artist,
        isPlaying = snapshot.isPlaying,
        positionMs = position,
        durationMs = snapshot.durationMs.coerceAtLeast(0L),
        showTransport = snapshot.title.isNotBlank() || snapshot.durationMs > 0L,
    )
}

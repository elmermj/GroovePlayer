package com.aethelsoft.grooveplayer.wear

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Phone/watch wire contract. Both APKs compile this file.
 * Connected on the watch means a reachable node advertising [CAPABILITY_PHONE].
 */
object WearProtocol {
    const val CAPABILITY_PHONE = "grooveplayer_phone"
    const val CAPABILITY_WATCH = "grooveplayer_watch"

    const val PATH_STATE = "/grooveplayer/state"
    const val PATH_PLAY_PAUSE = "/grooveplayer/cmd/play_pause"
    const val PATH_NEXT = "/grooveplayer/cmd/next"
    const val PATH_PREVIOUS = "/grooveplayer/cmd/previous"
    const val PATH_SEEK = "/grooveplayer/cmd/seek"
    const val PATH_REQUEST_STATE = "/grooveplayer/cmd/request_state"

    const val KEY_TITLE = "title"
    const val KEY_ARTIST = "artist"
    const val KEY_IS_PLAYING = "isPlaying"
    const val KEY_POSITION_MS = "positionMs"
    const val KEY_DURATION_MS = "durationMs"
    const val KEY_UPDATED_AT_MS = "updatedAtMs"
    const val KEY_ARTWORK = "artwork"

    /** Longest edge of the JPEG asset the phone may attach to [PATH_STATE]. */
    const val ARTWORK_MAX_EDGE_PX = 400

    const val SEEK_BYTES = 8

    fun encodeSeek(positionMs: Long): ByteArray =
        ByteBuffer.allocate(SEEK_BYTES).order(ByteOrder.BIG_ENDIAN).putLong(positionMs).array()

    fun decodeSeek(payload: ByteArray): Long? {
        if (payload.size != SEEK_BYTES) return null
        return ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN).long
    }

    fun parseCommand(path: String, payload: ByteArray?): WearCommand? {
        val body = payload ?: ByteArray(0)
        return when (path) {
            PATH_PLAY_PAUSE -> if (body.isEmpty()) WearCommand.PlayPause else null
            PATH_NEXT -> if (body.isEmpty()) WearCommand.Next else null
            PATH_PREVIOUS -> if (body.isEmpty()) WearCommand.Previous else null
            PATH_REQUEST_STATE -> if (body.isEmpty()) WearCommand.RequestState else null
            PATH_SEEK -> decodeSeek(body)?.let(WearCommand::Seek)
            else -> null
        }
    }
}

sealed interface WearCommand {
    data object PlayPause : WearCommand
    data object Next : WearCommand
    data object Previous : WearCommand
    data class Seek(val positionMs: Long) : WearCommand
    data object RequestState : WearCommand
}

package com.aethelworks.grooveplayer.wear

/**
 * Maps a watch command onto the existing player. Play/pause follows the same
 * rule as the phone controls: pause when the player is playing, otherwise play.
 */
sealed interface WearTransport {
    data object Play : WearTransport
    data object Pause : WearTransport
    data object Next : WearTransport
    data object Previous : WearTransport
    data class Seek(val positionMs: Long) : WearTransport
    data object Republish : WearTransport
}

object WearPlayerCommands {
    fun action(command: WearCommand, playing: Boolean): WearTransport = when (command) {
        WearCommand.PlayPause -> if (playing) WearTransport.Pause else WearTransport.Play
        WearCommand.Next -> WearTransport.Next
        WearCommand.Previous -> WearTransport.Previous
        is WearCommand.Seek -> WearTransport.Seek(command.positionMs)
        WearCommand.RequestState -> WearTransport.Republish
    }

    fun clampSeek(positionMs: Long, durationMs: Long): Long {
        if (durationMs <= 0L) return 0L
        return positionMs.coerceIn(0L, durationMs)
    }
}

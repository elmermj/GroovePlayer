package com.aethelworks.grooveplayer.wear

import com.aethelworks.grooveplayer.domain.repository.PlayerRepository
import dagger.Lazy
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Runs a watch command on the process-wide player. Playback stays in [PlayerRepository]. */
class WearCommandExecutor @Inject constructor(
    private val player: Lazy<PlayerRepository>,
    private val publisher: Lazy<WearStatePublisher>,
) {
    suspend fun execute(command: WearCommand) {
        val repository = player.get()
        when (val action = WearPlayerCommands.action(command, repository.observeIsPlaying().first())) {
            WearTransport.Play -> repository.play()
            WearTransport.Pause -> repository.pause()
            WearTransport.Next -> repository.next()
            WearTransport.Previous -> repository.previous()
            is WearTransport.Seek -> {
                val duration = repository.observeDuration().first()
                repository.seekTo(WearPlayerCommands.clampSeek(action.positionMs, duration))
            }
            WearTransport.Republish -> {
                publisher.get().noteWatchReachable()
                publisher.get().publishNow()
            }
        }
    }
}

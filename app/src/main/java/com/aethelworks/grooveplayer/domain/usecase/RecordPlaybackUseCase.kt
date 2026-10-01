package com.aethelworks.grooveplayer.domain.usecase

import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.repository.PlaybackHistoryRepository
import javax.inject.Inject

class RecordPlaybackUseCase @Inject constructor(
    private val playbackHistoryRepository: PlaybackHistoryRepository
) {
    suspend operator fun invoke(song: Song) {
        playbackHistoryRepository.recordPlayback(song)
    }
}


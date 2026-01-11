package com.aethelsoft.grooveplayer.domain.usecase

import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import javax.inject.Inject

class RecordPlaybackUseCase @Inject constructor(
    private val playbackHistoryRepository: PlaybackHistoryRepository
) {
    suspend operator fun invoke(song: Song) {
        playbackHistoryRepository.recordPlayback(song)
    }
}


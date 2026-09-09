package com.aethelsoft.grooveplayer.domain.usecase.home_category

import com.aethelsoft.grooveplayer.data.local.db.dao.PlaybackHistoryDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SongDao
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Refreshes music catalogs after excluded-folder changes are committed
 * (both newly excluded and newly included folders).
 *
 * - Prunes playback history and Room song index for songs that are no longer allowed
 * - Re-upserts the library index from the current (exclusion-aware) MediaStore set
 * - Bumps catalog generation so paging / home lists reload
 * - If the current track became excluded, plays a random included track; if none remain, stops
 */
class RefreshMusicCatalogUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playerRepository: PlayerRepository,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val songDao: SongDao,
    private val initializeLibraryIndexUseCase: InitializeLibraryIndexUseCase,
) {

    /**
     * @param removedSongIds song IDs that disappeared after the exclusion commit
     *   (present before commit, absent after). Empty when only folders were re-included.
     */
    suspend operator fun invoke(removedSongIds: Set<String> = emptySet()) = withContext(Dispatchers.IO) {
        if (removedSongIds.isNotEmpty()) {
            val ids = removedSongIds.toList()
            playbackHistoryDao.deleteBySongIds(ids)
            songDao.deleteBySongIds(ids)
        }

        // Always rebuild index + bump generation so includes show up and excludes disappear.
        initializeLibraryIndexUseCase()
        musicRepository.bumpCatalogGeneration()

        reconcilePlaybackAfterCatalogChange(removedSongIds)
    }

    private suspend fun reconcilePlaybackAfterCatalogChange(removedSongIds: Set<String>) {
        val allowedSongs = musicRepository.getAllSongs()
        val allowedIds = allowedSongs.map { it.id }.toSet()
        val currentSong = playerRepository.observeCurrentSong().first()
            ?: return

        val currentIsExcluded = currentSong.id !in allowedIds || currentSong.id in removedSongIds
        if (!currentIsExcluded) return

        if (allowedSongs.isEmpty()) {
            // Nothing left to play — stop and clear so the mini/full player disappears.
            playerRepository.pause()
            playerRepository.setQueue(emptyList(), startIndex = 0, isEndlessQueue = false, autoPlay = false)
            playerRepository.setFullScreenPlayerOpen(false)
            return
        }

        val shuffled = allowedSongs.shuffled()
        playerRepository.setQueue(
            songs = shuffled,
            startIndex = 0,
            isEndlessQueue = true,
            autoPlay = true
        )
    }
}

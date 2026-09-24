package com.aethelsoft.grooveplayer.data.playback

import androidx.room.withTransaction
import com.aethelsoft.grooveplayer.data.local.db.GroovePlayerDatabase
import com.aethelsoft.grooveplayer.data.local.db.dao.AlbumDao
import com.aethelsoft.grooveplayer.data.local.db.dao.ArtistDao
import com.aethelsoft.grooveplayer.data.local.db.dao.GenreDao
import com.aethelsoft.grooveplayer.data.local.db.dao.PlaybackHistoryDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SearchHistoryDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SongDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SongMetadataDao
import com.aethelsoft.grooveplayer.data.local.db.dao.UserSettingsDao
import com.aethelsoft.grooveplayer.domain.playback.SongCatalog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Removes a song from the device catalog and every derived row that would
 * leave a ghost: library links, playback history, search hits, and the saved queue.
 * There is no playlist table; album/artist/genre rows that no longer have songs go too.
 */
@Singleton
class SongCatalogStore @Inject constructor(
    private val database: GroovePlayerDatabase,
    private val songDao: SongDao,
    private val songMetadataDao: SongMetadataDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val genreDao: GenreDao,
    private val userSettingsDao: UserSettingsDao,
) : SongCatalog {

    override suspend fun contains(songId: String): Boolean = songDao.findSongId(songId) != null

    override suspend fun sourcePath(songId: String): String? = songDao.getSourcePath(songId)

    override suspend fun purge(songIds: List<String>) {
        val ids = songIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        database.withTransaction {
            playbackHistoryDao.deleteBySongIds(ids)
            searchHistoryDao.deleteSongEntries(ids)
            songMetadataDao.deleteBySongIds(ids)
            songDao.deleteBySongIds(ids)
            albumDao.deleteWithoutSongs()
            genreDao.deleteWithoutSongs()
            artistDao.deleteWithoutLinks()
            scrubPlayerSettings(idSet)
        }
    }

    private suspend fun scrubPlayerSettings(purged: Set<String>) {
        val settings = userSettingsDao.getUserSettings() ?: return
        val queue = settings.queueSongIds.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it !in purged }
        val lastCleared = settings.lastPlayedSongId != null && settings.lastPlayedSongId in purged
        val lastId = if (lastCleared) null else settings.lastPlayedSongId
        val lastPos = if (lastCleared) 0L else settings.lastPlayedPosition
        val start = settings.queueStartIndex.coerceIn(0, (queue.size - 1).coerceAtLeast(0))
        userSettingsDao.updatePlayerState(
            songId = lastId,
            position = lastPos,
            shuffle = settings.shuffleEnabled,
            repeat = settings.repeatMode,
            queueSongIds = queue.joinToString(","),
            queueStartIndex = start,
            isEndlessQueue = settings.isEndlessQueue,
        )
    }
}

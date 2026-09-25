package com.aethelsoft.grooveplayer.data.library

import androidx.room.withTransaction
import com.aethelsoft.grooveplayer.data.local.db.GroovePlayerDatabase
import com.aethelsoft.grooveplayer.data.local.db.dao.PlaybackHistoryDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SearchHistoryDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SongDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SongLikeDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SongMetadataDao
import com.aethelsoft.grooveplayer.data.local.db.dao.UserSettingsDao
import com.aethelsoft.grooveplayer.data.local.db.entity.SongEntity
import com.aethelsoft.grooveplayer.domain.library.HashRemap
import com.aethelsoft.grooveplayer.domain.library.LinkedSongIds
import com.aethelsoft.grooveplayer.domain.library.SongHashRemap
import com.aethelsoft.grooveplayer.domain.library.SongHashRow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps likes, play history, playlist links, and the queue on the song id that
 * owns a SHA-256. Rows for songs that used to live in shared folders are kept.
 */
@Singleton
class SongIdentityStore @Inject constructor(
    private val database: GroovePlayerDatabase,
    private val songDao: SongDao,
    private val songLikeDao: SongLikeDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val songMetadataDao: SongMetadataDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val userSettingsDao: UserSettingsDao,
) {
    suspend fun rows(): List<SongHashRow> {
        return songDao.getAll().map { song ->
            SongHashRow(
                songId = song.songId,
                contentHash = song.contentHash,
                inPrivateLibrary = song.inPrivateLibrary,
            )
        }
    }

    suspend fun links(): LinkedSongIds {
        val settings = userSettingsDao.getUserSettings()
        val queue = settings?.queueSongIds
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        return LinkedSongIds(
            likes = songLikeDao.allSongIds().toSet(),
            plays = playbackHistoryDao.allSongIds(),
            metadata = songMetadataDao.allSongIds().toSet(),
            playlistMembers = searchHistoryDao.songItemIds().toSet(),
            queue = queue,
            lastPlayed = settings?.lastPlayedSongId,
        )
    }

    suspend fun libraryHasHash(hash: String): Boolean {
        return songDao.findByContentHash(hash).any { song ->
            song.inPrivateLibrary && !song.sourcePath.isNullOrBlank() &&
                File(song.sourcePath).isFile
        }
    }

    /**
     * Attach a verified private-library file to the song id that already owns [hash],
     * or create `private:<hash>` when this content is new.
     */
    suspend fun adoptVerifiedCopy(
        hash: String,
        libraryPath: String,
        title: String,
        durationMs: Long,
    ): String {
        val key = hash.lowercase()
        val remap = SongHashRemap.remap(key, rows(), links())
        database.withTransaction {
            applyLinks(remap)
            if (remap.createdNewId) {
                songDao.insertOrUpdate(
                    SongEntity(
                        songId = remap.canonicalSongId,
                        uri = File(libraryPath).toURI().toString(),
                        title = title.ifBlank { File(libraryPath).name },
                        durationMs = durationMs,
                        sourcePath = libraryPath,
                        contentHash = key,
                        inPrivateLibrary = true,
                    ),
                )
            } else {
                val existing = songDao.getSong(remap.canonicalSongId)
                songDao.adoptPrivateFile(
                    songId = remap.canonicalSongId,
                    sourcePath = libraryPath,
                    uri = File(libraryPath).toURI().toString(),
                    title = title.ifBlank { existing?.title ?: File(libraryPath).name },
                    durationMs = durationMs.takeIf { it > 0L } ?: existing?.durationMs ?: 0L,
                    contentHash = key,
                )
            }
        }
        return remap.canonicalSongId
    }

    private suspend fun applyLinks(remap: HashRemap) {
        val canonical = remap.canonicalSongId
        for (from in remap.retiredSongIds) {
            if (from == canonical) continue
            if (songLikeDao.findSongId(canonical) != null) {
                songLikeDao.delete(from)
            } else {
                songLikeDao.retargetSongId(from, canonical)
            }
            playbackHistoryDao.retargetSongId(from, canonical)
            if (songMetadataDao.getMetadata(canonical) != null) {
                songMetadataDao.deleteBySongIds(listOf(from))
            } else {
                songMetadataDao.retargetSongId(from, canonical)
            }
            searchHistoryDao.retargetSong(from, canonical)
            songDao.copyArtistLinks(from, canonical)
            songDao.deleteArtistLinks(from)
            songDao.copyGenreLinks(from, canonical)
            songDao.deleteGenreLinks(from)
        }
        val settings = userSettingsDao.getUserSettings() ?: return
        val queue = remap.links.queue.joinToString(",")
        val last = remap.links.lastPlayed
        if (queue != settings.queueSongIds || last != settings.lastPlayedSongId) {
            userSettingsDao.updatePlayerState(
                songId = last,
                position = settings.lastPlayedPosition,
                shuffle = settings.shuffleEnabled,
                repeat = settings.repeatMode,
                queueSongIds = queue,
                queueStartIndex = settings.queueStartIndex.coerceAtMost(
                    (remap.links.queue.size - 1).coerceAtLeast(0),
                ),
                isEndlessQueue = settings.isEndlessQueue,
            )
        }
    }
}

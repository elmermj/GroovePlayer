package com.aethelsoft.grooveplayer.data.repository

import androidx.room.withTransaction
import com.aethelsoft.grooveplayer.data.local.db.GroovePlayerDatabase
import com.aethelsoft.grooveplayer.data.local.db.dao.PlaylistDao
import com.aethelsoft.grooveplayer.data.local.db.entity.PlaylistEntity
import com.aethelsoft.grooveplayer.data.mapper.PlaylistMapper
import com.aethelsoft.grooveplayer.domain.model.Playlist
import com.aethelsoft.grooveplayer.domain.model.PlaylistWithTracks
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.playlist.InvalidPlaylistNameException
import com.aethelsoft.grooveplayer.domain.playlist.PlaylistNames
import com.aethelsoft.grooveplayer.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    private val database: GroovePlayerDatabase,
    private val playlistDao: PlaylistDao,
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<Playlist>> =
        playlistDao.observeAll().map { rows -> rows.map(PlaylistMapper::toSummary) }

    override fun observePlaylist(id: Long): Flow<PlaylistWithTracks?> =
        playlistDao.observeById(id).map { row -> row?.let(PlaylistMapper::toDomain) }

    override suspend fun getPlaylist(id: Long): PlaylistWithTracks? =
        playlistDao.getById(id)?.let(PlaylistMapper::toDomain)

    override suspend fun create(name: String): Long {
        val sanitized = PlaylistNames.sanitize(name)
        val error = PlaylistNames.validationError(sanitized)
        if (error != null) throw InvalidPlaylistNameException(error)
        val now = System.currentTimeMillis()
        return playlistDao.insertPlaylist(
            PlaylistEntity(
                name = sanitized,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun rename(id: Long, name: String) {
        val sanitized = PlaylistNames.sanitize(name)
        val error = PlaylistNames.validationError(sanitized)
        if (error != null) throw InvalidPlaylistNameException(error)
        playlistDao.rename(id, sanitized, System.currentTimeMillis())
    }

    override suspend fun delete(id: Long) {
        database.withTransaction {
            playlistDao.deleteTracksFor(id)
            playlistDao.deletePlaylist(id)
        }
    }

    override suspend fun addSongs(playlistId: Long, songs: List<Song>) {
        if (songs.isEmpty()) return
        database.withTransaction {
            val start = playlistDao.tracksFor(playlistId).maxOfOrNull { it.position }?.plus(1) ?: 0
            playlistDao.insertTracks(
                songs.mapIndexed { index, song ->
                    PlaylistMapper.toEntity(playlistId, start + index, song)
                },
            )
            playlistDao.touch(playlistId, System.currentTimeMillis())
        }
    }

    override suspend fun removeTrack(playlistId: Long, entryId: Long) {
        database.withTransaction {
            playlistDao.deleteTrack(entryId)
            playlistDao.tracksFor(playlistId).forEachIndexed { index, track ->
                if (track.position != index) {
                    playlistDao.updatePosition(track.id, index)
                }
            }
            playlistDao.touch(playlistId, System.currentTimeMillis())
        }
    }

    override suspend fun reorder(playlistId: Long, orderedEntryIds: List<Long>) {
        database.withTransaction {
            val current = playlistDao.tracksFor(playlistId)
            if (current.size != orderedEntryIds.size) return@withTransaction
            if (current.map { it.id }.toSet() != orderedEntryIds.toSet()) return@withTransaction
            orderedEntryIds.forEachIndexed { index, entryId ->
                playlistDao.updatePosition(entryId, index)
            }
            playlistDao.touch(playlistId, System.currentTimeMillis())
        }
    }
}

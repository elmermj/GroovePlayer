package com.aethelsoft.grooveplayer.data.repository

import androidx.room.withTransaction
import com.aethelsoft.grooveplayer.data.library.PlaylistLibraryIndex
import com.aethelsoft.grooveplayer.data.local.db.GroovePlayerDatabase
import com.aethelsoft.grooveplayer.data.local.db.dao.PlaylistDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SongDao
import com.aethelsoft.grooveplayer.data.local.db.entity.PlaylistEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.PlaylistTrackEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.PlaylistWithTracksRelation
import com.aethelsoft.grooveplayer.data.local.db.entity.SongEntity
import com.aethelsoft.grooveplayer.data.mapper.PlaylistMapper
import com.aethelsoft.grooveplayer.domain.library.PrivateLibrarySongs
import com.aethelsoft.grooveplayer.domain.model.Playlist
import com.aethelsoft.grooveplayer.domain.model.PlaylistTrack
import com.aethelsoft.grooveplayer.domain.model.PlaylistWithTracks
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.playlist.InvalidPlaylistNameException
import com.aethelsoft.grooveplayer.domain.playlist.PlaylistNames
import com.aethelsoft.grooveplayer.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    private val database: GroovePlayerDatabase,
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    private val libraryIndex: PlaylistLibraryIndex,
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<Playlist>> =
        playlistDao.observeAll().map { rows -> rows.map(PlaylistMapper::toSummary) }

    override fun observePlaylist(id: Long): Flow<PlaylistWithTracks?> =
        playlistDao.observeById(id).map { row -> row?.let { resolve(it) } }

    override suspend fun getPlaylist(id: Long): PlaylistWithTracks? =
        playlistDao.getById(id)?.let { resolve(it) }

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
        val keyed = songs.mapNotNull { song ->
            val hash = libraryIndex.hashFor(song) ?: return@mapNotNull null
            song to hash
        }
        if (keyed.isEmpty()) return
        database.withTransaction {
            val start = playlistDao.tracksFor(playlistId).maxOfOrNull { it.position }?.plus(1) ?: 0
            playlistDao.insertTracks(
                keyed.mapIndexed { index, (song, hash) ->
                    PlaylistMapper.toEntity(playlistId, start + index, song, hash)
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

    private suspend fun resolve(relation: PlaylistWithTracksRelation): PlaylistWithTracks {
        val base = PlaylistMapper.toDomain(relation)
        val tracks = relation.tracks
            .sortedWith(compareBy({ it.position }, { it.id }))
            .map { entity -> resolveTrack(entity) }
        return base.copy(tracks = tracks)
    }

    private suspend fun resolveTrack(entity: PlaylistTrackEntity): PlaylistTrack {
        val hash = entity.contentHash.lowercase()
        val candidates = songDao.findByContentHash(hash).filter { it.inPrivateLibrary }
        val row = candidates.firstOrNull { it.songId == entity.songId && fileReady(it) }
            ?: candidates.firstOrNull { fileReady(it) }
        if (row != null) {
            val path = row.sourcePath.orEmpty()
            val file = File(path)
            val song = PrivateLibrarySongs.toSong(
                absolutePath = path,
                displayName = file.name,
                sizeBytes = file.length(),
                durationMs = entity.durationMs,
                title = row.title.ifBlank { entity.title },
                artist = entity.artist,
                albumName = entity.albumName ?: PrivateLibrarySongs.ALBUM,
                genre = entity.genre,
                artworkUrl = entity.artworkUrl,
            ).copy(id = row.songId)
            return PlaylistTrack(
                entryId = entity.id,
                position = entity.position,
                song = song,
                contentHash = hash,
                available = true,
            )
        }
        val stored = songDao.getSong(entity.songId)
        return PlaylistTrack(
            entryId = entity.id,
            position = entity.position,
            song = PlaylistMapper.snapshot(entity, stored?.sourcePath),
            contentHash = hash,
            available = false,
        )
    }

    private fun fileReady(row: SongEntity): Boolean {
        val path = row.sourcePath ?: return false
        val file = File(path)
        return file.isFile && file.length() > 0L
    }
}

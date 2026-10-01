package com.aethelworks.grooveplayer.data.repository

import androidx.room.withTransaction
import com.aethelworks.grooveplayer.data.artwork.SongArtworkModels
import com.aethelworks.grooveplayer.data.local.db.GroovePlayerDatabase
import com.aethelworks.grooveplayer.data.local.db.dao.SongLikeDao
import com.aethelworks.grooveplayer.data.local.db.entity.SongLikeEntity
import com.aethelworks.grooveplayer.domain.artwork.EmbeddedArtworkKeys
import com.aethelworks.grooveplayer.domain.library.SongLikeIndex
import com.aethelworks.grooveplayer.domain.model.FavoriteAlbum
import com.aethelworks.grooveplayer.domain.model.FavoriteArtist
import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.model.SongLike
import com.aethelworks.grooveplayer.domain.model.toSongLike
import com.aethelworks.grooveplayer.domain.repository.SongLikeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongLikeRepositoryImpl @Inject constructor(
    private val database: GroovePlayerDatabase,
    private val dao: SongLikeDao,
    private val artworkModels: SongArtworkModels,
) : SongLikeRepository {

    private val toggleMutex = Mutex()

    override fun observeLikedSongIds(): Flow<Set<String>> {
        return dao.observeSongIds().map { ids -> ids.toSet() }
    }

    override fun observeFavoriteTracks(limit: Int): Flow<List<Song>> {
        return dao.observeAll().map { rows ->
            SongLikeIndex.tracks(rows.toLikes(), limit)
        }
    }

    override fun observeFavoriteArtists(limit: Int): Flow<List<FavoriteArtist>> {
        return dao.observeAll().map { rows ->
            SongLikeIndex.artists(rows.toLikes(), limit)
        }
    }

    override fun observeFavoriteAlbums(limit: Int): Flow<List<FavoriteAlbum>> {
        return dao.observeAll().map { rows ->
            SongLikeIndex.albums(rows.toLikes(), limit)
        }
    }

    override suspend fun toggle(song: Song) {
        if (song.id.isBlank()) return
        toggleMutex.withLock {
            database.withTransaction {
                if (dao.findSongId(song.id) != null) {
                    dao.delete(song.id)
                } else {
                    dao.insert(song.toSongLike(System.currentTimeMillis()).toEntity())
                }
            }
        }
    }

    private suspend fun List<SongLikeEntity>.toLikes(): List<SongLike> {
        val models = artworkModels.urls(map { it.songId })
        return map { row ->
            val like = row.toDomain()
            val url = EmbeddedArtworkKeys.prefer(like.artworkUrl, models[row.songId])
            if (url == like.artworkUrl) like else like.copy(artworkUrl = url)
        }
    }
}

private fun SongLikeEntity.toDomain(): SongLike {
    return SongLike(
        songId = songId,
        title = title,
        artist = artist,
        album = album,
        genre = genre,
        uri = uri,
        artworkUrl = artworkUrl,
        durationMs = durationMs,
        likedAt = likedAt,
    )
}

private fun SongLike.toEntity(): SongLikeEntity {
    return SongLikeEntity(
        songId = songId,
        title = title,
        artist = artist,
        album = album,
        genre = genre,
        uri = uri,
        artworkUrl = artworkUrl,
        durationMs = durationMs,
        likedAt = likedAt,
    )
}

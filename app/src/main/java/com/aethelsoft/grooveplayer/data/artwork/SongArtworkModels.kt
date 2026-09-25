package com.aethelsoft.grooveplayer.data.artwork

import com.aethelsoft.grooveplayer.data.local.db.dao.SongDao
import com.aethelsoft.grooveplayer.domain.artwork.EmbeddedArtworkKeys
import javax.inject.Inject
import javax.inject.Singleton

/** Maps song ids to the hash-keyed artwork model already stored on `songs`. */
@Singleton
class SongArtworkModels @Inject constructor(
    private val songDao: SongDao,
) {
    suspend fun urls(songIds: Collection<String>): Map<String, String> {
        if (songIds.isEmpty()) return emptyMap()
        val wanted = songIds.toSet()
        return songDao.getAll().mapNotNull { song ->
            if (song.songId !in wanted) return@mapNotNull null
            val model = EmbeddedArtworkKeys.uri(song.contentHash) ?: return@mapNotNull null
            song.songId to model
        }.toMap()
    }
}

package com.aethelsoft.grooveplayer.data.playback

import com.aethelsoft.grooveplayer.domain.model.BackupObject
import com.aethelsoft.grooveplayer.domain.playback.cloudObjectMatchesSong
import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory view of GET /v1/backup/objects?kind=song for premium availability
 * badges. Playback does not use this list. Not loaded for Free/Basic.
 */
@Singleton
class CloudSongCatalog @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    private val mutex = Mutex()
    private val confirmedSongIds = ConcurrentHashMap.newKeySet<String>()
    private var objects: List<BackupObject>? = null
    private var loadedAtMs = 0L

    fun confirm(songId: String) {
        if (songId.isNotBlank()) confirmedSongIds.add(songId)
    }

    suspend fun isBackedUp(songId: String, path: String?, sizeBytes: Long?): Boolean {
        if (songId in confirmedSongIds) return true
        val listed = load() ?: return false
        return listed.any { obj ->
            cloudObjectMatchesSong(path, sizeBytes, obj.logicalPath, obj.sizeBytes)
        }
    }

    /**
     * @return null when the catalog could not be read (do not treat as "absent").
     */
    suspend fun load(): List<BackupObject>? = mutex.withLock {
        val now = System.currentTimeMillis()
        val cached = objects
        if (cached != null && now - loadedAtMs < TTL_MS) return@withLock cached
        val fresh = backupRepository.listRemoteObjects("song").getOrNull() ?: return@withLock null
        objects = fresh
        loadedAtMs = now
        fresh
    }

    private companion object {
        const val TTL_MS = 60_000L
    }
}

package com.aethelsoft.grooveplayer.data.playback

import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.playback.SongAvailabilityMark
import com.aethelsoft.grooveplayer.domain.playback.SongCatalog
import com.aethelsoft.grooveplayer.domain.playback.songAvailabilityMark
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongAvailabilityRepository @Inject constructor(
    private val entitlement: PremiumCloudEntitlement,
    private val localAudio: LocalAudioProbe,
    private val cache: CloudPlaybackCacheStore,
    private val catalog: SongCatalog,
    private val cloudSongs: CloudSongCatalog,
) {
    val showAvailability: Flow<Boolean> = entitlement.showAvailability

    fun isPremiumNow(): Boolean = entitlement.isPremiumNow()

    suspend fun markFor(song: Song): SongAvailabilityMark? {
        if (!entitlement.isPremiumNow()) return null
        val path = song.filePath ?: catalog.sourcePath(song.id)
        val local = localAudio.isReadable(song.uri, path) || cache.existingUri(song.id) != null
        val cloud = cloudSongs.isBackedUp(song.id, path, song.fileSizeBytes)
        return songAvailabilityMark(
            isPremium = true,
            localAvailable = local,
            cloudAvailable = cloud,
        )
    }
}

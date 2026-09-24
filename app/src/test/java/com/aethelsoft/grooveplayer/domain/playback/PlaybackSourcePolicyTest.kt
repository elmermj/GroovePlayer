package com.aethelsoft.grooveplayer.domain.playback

import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.usecase.player_category.ResolvePlaybackSourceUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.ResolvedPlayback
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSourcePolicyTest {

    @Test
    fun localFileNeverConsultsCloud() {
        assertEquals(
            PlaybackDecision.PLAY_LOCAL,
            playbackDecision(
                localAvailable = true,
                inCatalog = true,
                cloud = CloudAudioPresence.PRESENT,
                canStreamCloud = true,
            ),
        )
        assertEquals(
            PlaybackDecision.PLAY_LOCAL,
            playbackDecision(
                localAvailable = true,
                inCatalog = false,
                cloud = CloudAudioPresence.ABSENT,
                canStreamCloud = false,
            ),
        )
    }

    @Test
    fun catalogSongStreamsOnlyWhenServerSaysEntitled() {
        assertEquals(
            PlaybackDecision.STREAM_CLOUD,
            playbackDecision(false, true, CloudAudioPresence.PRESENT, canStreamCloud = true),
        )
        assertEquals(
            PlaybackDecision.SKIP,
            playbackDecision(false, true, CloudAudioPresence.PRESENT, canStreamCloud = false),
        )
    }

    @Test
    fun openGraceIsNotAnActiveStreamEntitlement() {
        val now = 1_000L
        assertTrue(activeCloudStreamEntitled(PrivilegeTier.PREMIUM, graceUntilEpochMs = null, now))
        assertTrue(activeCloudStreamEntitled(PrivilegeTier.PREMIUM, graceUntilEpochMs = now, now))
        assertTrue(!activeCloudStreamEntitled(PrivilegeTier.PREMIUM, graceUntilEpochMs = now + 1, now))
        assertTrue(!activeCloudStreamEntitled(PrivilegeTier.FREE, graceUntilEpochMs = null, now))
        assertTrue(!activeCloudStreamEntitled(PrivilegeTier.BASIC, graceUntilEpochMs = null, now))
    }

    @Test
    fun objects404IsTheOnlyPurgeSignal() {
        assertEquals(
            CloudAudioPresence.ABSENT,
            interpretPlaybackObjects(404, exists = false, entitled = true).first,
        )
        assertEquals(
            CloudAudioPresence.PRESENT to false,
            interpretPlaybackObjects(200, exists = true, entitled = false),
        )
        assertEquals(
            CloudAudioPresence.PRESENT to true,
            interpretPlaybackObjects(200, exists = true, entitled = true),
        )
        assertEquals(
            CloudAudioPresence.UNKNOWN,
            interpretPlaybackObjects(401, exists = null, entitled = null).first,
        )
        assertEquals(
            CloudAudioPresence.UNKNOWN,
            interpretPlaybackObjects(503, exists = null, entitled = null).first,
        )
    }

    @Test
    fun streamUrl403KeepsTheRowAndDryRunIsNotPlayable() {
        assertTrue(interpretStreamUrl(403, exists = true, entitled = false, dryRun = false, streamUrl = null) is CloudStreamOpen.NotEntitled)
        assertTrue(interpretStreamUrl(404, exists = false, entitled = true, dryRun = false, streamUrl = null) is CloudStreamOpen.Absent)
        assertTrue(
            interpretStreamUrl(
                200,
                exists = true,
                entitled = true,
                dryRun = true,
                streamUrl = "https://dry-run.r2.local/x",
            ) is CloudStreamOpen.Unusable,
        )
        val play = interpretStreamUrl(
            200,
            exists = true,
            entitled = true,
            dryRun = false,
            streamUrl = "https://bucket.r2.cloudflarestorage.com/o?X-Amz-Signature=1",
        )
        assertTrue(play is CloudStreamOpen.Play)
        assertEquals(
            "https://bucket.r2.cloudflarestorage.com/o?X-Amz-Signature=1",
            (play as CloudStreamOpen.Play).url,
        )
        assertTrue(interpretStreamUrl(401, exists = null, entitled = true, dryRun = false, streamUrl = null) is CloudStreamOpen.Unknown)
        assertTrue(interpretStreamUrl(500, exists = null, entitled = true, dryRun = false, streamUrl = null) is CloudStreamOpen.Unknown)
    }

    @Test
    fun missingEverywhereInCatalogPurges() {
        assertEquals(
            PlaybackDecision.PURGE,
            playbackDecision(false, true, CloudAudioPresence.ABSENT, canStreamCloud = true),
        )
        assertEquals(
            PlaybackDecision.PURGE,
            playbackDecision(false, true, CloudAudioPresence.ABSENT, canStreamCloud = false),
        )
    }

    @Test
    fun unknownCloudOrMissingCatalogDoesNotPurge() {
        assertEquals(
            PlaybackDecision.SKIP,
            playbackDecision(false, true, CloudAudioPresence.UNKNOWN, canStreamCloud = true),
        )
        assertEquals(
            PlaybackDecision.SKIP,
            playbackDecision(false, false, CloudAudioPresence.ABSENT, canStreamCloud = true),
        )
    }

    @Test
    fun queueStartFollowsTheNextSurvivingSong() {
        val ids = listOf("a", "b", "c", "d")
        assertEquals(1, adjustedQueueStartIndex(ids, startIndex = 1, playableIds = listOf("a", "b", "c", "d")))
        assertEquals(1, adjustedQueueStartIndex(ids, startIndex = 1, playableIds = listOf("a", "c", "d")))
        assertEquals(0, adjustedQueueStartIndex(ids, startIndex = 3, playableIds = listOf("a")))
        assertEquals(0, adjustedQueueStartIndex(ids, startIndex = 0, playableIds = emptyList()))
    }

    @Test
    fun signedPlaybackUrlsAreRecognizedAndDryRunIsNot() {
        assertTrue(looksLikeSignedObjectUrl("https://bucket.r2.cloudflarestorage.com/a?X-Amz-Signature=abc"))
        assertTrue(!looksLikeSignedObjectUrl("https://stream.example.com/audio.mp3"))
        assertTrue(isDryRunPlaybackUrl("https://dry-run.r2.local/backups/u/h"))
        assertTrue(!isDryRunPlaybackUrl("https://bucket.r2.cloudflarestorage.com/a?X-Amz-Signature=abc"))
    }

    @Test
    fun cloudMatchUsesBasenameAndSize() {
        assertTrue(
            cloudObjectMatchesSong(
                songPath = "/Music/Album/Track.mp3",
                songSizeBytes = 10L,
                objectLogicalPath = "/backup/Track.mp3",
                objectSizeBytes = 10L,
            ),
        )
        assertTrue(
            !cloudObjectMatchesSong(
                songPath = "/Music/Track.mp3",
                songSizeBytes = 10L,
                objectLogicalPath = "/backup/Track.mp3",
                objectSizeBytes = 11L,
            ),
        )
        assertTrue(
            !cloudObjectMatchesSong(
                songPath = null,
                songSizeBytes = null,
                objectLogicalPath = "/backup/Track.mp3",
                objectSizeBytes = 10L,
            ),
        )
    }
}

class SongAvailabilityPolicyTest {
    @Test
    fun premiumSeesBothOrCloudOnly() {
        assertEquals(
            SongAvailabilityMark.LOCAL_AND_CLOUD,
            songAvailabilityMark(isPremium = true, localAvailable = true, cloudAvailable = true),
        )
        assertEquals(
            SongAvailabilityMark.CLOUD_ONLY,
            songAvailabilityMark(isPremium = true, localAvailable = false, cloudAvailable = true),
        )
    }

    @Test
    fun localOnlyAndFreeRenderNothing() {
        assertNull(songAvailabilityMark(true, localAvailable = true, cloudAvailable = false))
        assertNull(songAvailabilityMark(false, localAvailable = true, cloudAvailable = true))
        assertNull(songAvailabilityMark(false, localAvailable = false, cloudAvailable = true))
        assertEquals(
            "Available on device and in cloud",
            songAvailabilityContentDescription(SongAvailabilityMark.LOCAL_AND_CLOUD),
        )
        assertEquals(
            "Available in cloud only",
            songAvailabilityContentDescription(SongAvailabilityMark.CLOUD_ONLY),
        )
    }
}

class ResolvePlaybackSourceUseCaseTest {

    @Test
    fun localPlayDoesNotCallCloudOrPurge() = runBlocking {
        val cloud = FakeCloud()
        val catalog = FakeCatalog(ids = setOf("1"))
        val useCase = useCase(local = true, cloud = cloud, catalog = catalog)
        val resolved = useCase.resolveOne(song("1"))
        assertTrue(resolved is ResolvedPlayback.Playable)
        assertEquals("content://1", (resolved as ResolvedPlayback.Playable).song.uri)
        assertEquals(0, cloud.calls)
        assertTrue(catalog.purged.isEmpty())
    }

    @Test
    fun entitledCloudSongDefersStreamUrlUntilOpen() = runBlocking {
        val cloud = FakeCloud(
            CloudAudioHit(
                presence = CloudAudioPresence.PRESENT,
                entitled = true,
                contentHash = "abc",
            ),
        )
        val cache = FakeCache()
        val tickets = FakeTickets()
        val useCase = useCase(
            local = false,
            cloud = cloud,
            catalog = FakeCatalog(setOf("1")),
            cache = cache,
            tickets = tickets,
        )
        val resolved = useCase.resolveOne(song("1"))
        assertTrue(resolved is ResolvedPlayback.Playable)
        assertEquals(playbackStreamUri("1"), (resolved as ResolvedPlayback.Playable).song.uri)
        assertEquals(0, cloud.streamOpens)
        assertTrue(cache.stored.isEmpty())
        assertEquals("abc", tickets.get("1")?.contentHash)
    }

    @Test
    fun openGraceKeepsCatalogWhenCloudExists() = runBlocking {
        val catalog = FakeCatalog(setOf("1"))
        val cache = FakeCache()
        val useCase = useCase(
            local = false,
            cloud = FakeCloud(CloudAudioHit(CloudAudioPresence.PRESENT, entitled = false)),
            catalog = catalog,
            cache = cache,
        )
        val resolved = useCase.resolveOne(song("1"))
        assertTrue(resolved is ResolvedPlayback.Dropped)
        assertEquals(PlaybackDropReason.NOT_ENTITLED, (resolved as ResolvedPlayback.Dropped).reason)
        assertTrue(!resolved.purged)
        assertTrue(catalog.purged.isEmpty())
        assertTrue(cache.stored.isEmpty())
    }

    @Test
    fun confirmedMissingPurgesCatalogAndCache() = runBlocking {
        val catalog = FakeCatalog(setOf("1"))
        val cache = FakeCache(existing = "file:///stale")
        val useCase = useCase(
            local = false,
            cloud = FakeCloud(CloudAudioHit(CloudAudioPresence.ABSENT)),
            catalog = catalog,
            cache = cache,
        )
        // Cache file counts as local and must win. Use a cache that is empty so purge runs.
        cache.existing = null
        val resolved = useCase.resolveOne(song("1"))
        assertTrue(resolved is ResolvedPlayback.Dropped && resolved.purged)
        assertEquals(listOf(listOf("1")), catalog.purged)
        assertEquals(listOf("1"), cache.deleted)
    }

    @Test
    fun queueDropsPurgedSongAndStartsOnTheNext() = runBlocking {
        val catalog = FakeCatalog(setOf("b"))
        val localIds = setOf("a", "c")
        val useCase = ResolvePlaybackSourceUseCase(
            localAudio = object : LocalAudioAvailability {
                override fun isReadable(uri: String, filePath: String?) = uri.substringAfterLast("/") in localIds
            },
            cache = FakeCache(),
            catalog = catalog,
            cloud = FakeCloud(CloudAudioHit(CloudAudioPresence.ABSENT)),
            tickets = FakeTickets(),
        )
        val queue = useCase.resolveQueue(
            songs = listOf(song("a"), song("b"), song("c")),
            startIndex = 1,
        )
        assertEquals(listOf("a", "c"), queue.songs.map { it.id })
        assertEquals(1, queue.startIndex)
        assertEquals(listOf(listOf("b")), catalog.purged)
        assertTrue(!queue.premiumStreamBlocked)
    }

    @Test
    fun corruptCatalogKeepsReadableLocalSongAndDropsTheRest() = runBlocking {
        val useCase = ResolvePlaybackSourceUseCase(
            localAudio = object : LocalAudioAvailability {
                override fun isReadable(uri: String, filePath: String?) = uri == "content://local"
            },
            cache = FakeCache(),
            catalog = object : SongCatalog {
                override suspend fun contains(songId: String): Boolean = error("room closed")
                override suspend fun sourcePath(songId: String): String? = error("no such column: sourcePath")
                override suspend fun purge(songIds: List<String>) = error("room closed")
            },
            cloud = object : CloudAudioLookup {
                override suspend fun lookup(song: Song, logicalPath: String?): CloudAudioHit =
                    error("playback api failed")
                override suspend fun openStream(ticket: PlaybackStreamTicket): CloudStreamOpen =
                    error("playback api failed")
            },
            tickets = FakeTickets(),
        )
        val queue = useCase.resolveQueue(
            songs = listOf(
                song("local").copy(uri = "content://local", filePath = null),
                song("cloud").copy(uri = "content://cloud", filePath = null),
            ),
            startIndex = 1,
        )
        assertEquals(listOf("local"), queue.songs.map { it.id })
        assertEquals(0, queue.startIndex)
    }

    @Test
    fun nonPremiumQueueSkipsCloudSongWithoutPurging() = runBlocking {
        val catalog = FakeCatalog(setOf("a", "b"))
        val useCase = ResolvePlaybackSourceUseCase(
            localAudio = object : LocalAudioAvailability {
                override fun isReadable(uri: String, filePath: String?) = uri.endsWith("/a")
            },
            cache = FakeCache(),
            catalog = catalog,
            cloud = FakeCloud(CloudAudioHit(CloudAudioPresence.PRESENT, entitled = false)),
            tickets = FakeTickets(),
        )
        val queue = useCase.resolveQueue(listOf(song("a"), song("b")), startIndex = 1)
        assertEquals(listOf("a"), queue.songs.map { it.id })
        assertEquals(0, queue.startIndex)
        assertTrue(queue.premiumStreamBlocked)
        assertTrue(catalog.purged.isEmpty())
    }

    private fun useCase(
        local: Boolean,
        cloud: FakeCloud,
        catalog: FakeCatalog,
        cache: FakeCache = FakeCache(),
        tickets: FakeTickets = FakeTickets(),
    ) = ResolvePlaybackSourceUseCase(
        localAudio = object : LocalAudioAvailability {
            override fun isReadable(uri: String, filePath: String?) = local
        },
        cache = cache,
        catalog = catalog,
        cloud = cloud,
        tickets = tickets,
    )

    private fun song(id: String) = Song(
        id = id,
        title = "T",
        artist = "A",
        uri = "content://$id",
        genre = "",
        durationMs = 1L,
        filePath = "/music/$id.mp3",
    )

    private class FakeCloud(
        private val hit: CloudAudioHit = CloudAudioHit(CloudAudioPresence.UNKNOWN),
    ) : CloudAudioLookup {
        var calls = 0
        var streamOpens = 0
        override suspend fun lookup(song: Song, logicalPath: String?): CloudAudioHit {
            calls++
            return hit
        }
        override suspend fun openStream(ticket: PlaybackStreamTicket): CloudStreamOpen {
            streamOpens++
            return CloudStreamOpen.Unknown
        }
    }

    private class FakeTickets : PlaybackStreamTickets {
        private val saved = mutableMapOf<String, PlaybackStreamTicket>()
        override fun put(ticket: PlaybackStreamTicket) {
            saved[ticket.songId] = ticket
        }
        override fun get(songId: String): PlaybackStreamTicket? = saved[songId]
    }

    private class FakeCatalog(ids: Set<String>) : SongCatalog {
        private val ids = ids.toMutableSet()
        val purged = mutableListOf<List<String>>()
        override suspend fun contains(songId: String) = songId in ids
        override suspend fun sourcePath(songId: String) = "/music/$songId.mp3"
        override suspend fun purge(songIds: List<String>) {
            purged += songIds
            ids.removeAll(songIds.toSet())
        }
    }

    private class FakeCache(var existing: String? = null) : CloudPlaybackCache {
        val stored = mutableListOf<String>()
        val deleted = mutableListOf<String>()
        override fun existingUri(songId: String) = existing
        override suspend fun storeFromUrl(songId: String, url: String): String? {
            stored += songId
            return "file:///cache/$songId"
        }
        override fun delete(songId: String) {
            deleted += songId
            existing = null
        }
    }
}

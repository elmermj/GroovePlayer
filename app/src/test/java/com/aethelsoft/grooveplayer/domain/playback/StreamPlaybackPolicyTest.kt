package com.aethelsoft.grooveplayer.domain.playback

import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamPlaybackPolicyTest {

    private val cloud = "groove-playback://song-2"
    private val local = "content://media/audio/1"

    @Test
    fun cacheLimitStaysSmallWhenTheVolumeIsTight() {
        assertEquals(0L, streamCacheLimitBytes(0))
        assertEquals(0L, streamCacheLimitBytes(200L * 1024 * 1024))
        assertEquals(
            STREAM_CACHE_TIGHT_CAP_BYTES,
            streamCacheLimitBytes(400L * 1024 * 1024),
        )
        val eightyEightMb = 88L * 1024 * 1024
        assertEquals(eightyEightMb, streamCacheLimitBytes(600L * 1024 * 1024))
        assertEquals(
            STREAM_CACHE_PREFERRED_BYTES,
            streamCacheLimitBytes(20L * 1024 * 1024 * 1024),
        )
    }

    @Test
    fun cacheKeyIgnoresTheSignedUrlAndChangesWithContentHash() {
        val uri = "groove-playback://song-2"
        assertEquals(uri, streamCacheKey(uri, null))
        assertEquals(uri, streamCacheKey(uri, "  "))
        assertEquals("$uri#abc", streamCacheKey(uri, "abc"))
        assertEquals("$uri#def", streamCacheKey(uri, "def"))
    }

    @Test
    fun prefetchOnlyTheNextCloudItem() {
        val queue = listOf(local, cloud, "file:///sdcard/c.mp3")
        assertEquals(cloud, nextCloudStreamUri(queue, currentIndex = 0, RepeatMode.OFF))
        assertNull(nextCloudStreamUri(queue, currentIndex = 1, RepeatMode.OFF))
        assertNull(nextCloudStreamUri(queue, currentIndex = 0, RepeatMode.ONE))
        assertNull(nextCloudStreamUri(emptyList(), currentIndex = 0, RepeatMode.OFF))
    }

    @Test
    fun repeatAllWrapsToACloudOpener() {
        val queue = listOf(cloud, local)
        assertEquals(cloud, nextCloudStreamUri(queue, currentIndex = 1, RepeatMode.ALL))
        assertNull(nextCloudStreamUri(queue, currentIndex = 1, RepeatMode.OFF))
        assertNull(nextCloudStreamUri(listOf(cloud), currentIndex = 0, RepeatMode.ALL))
    }

    @Test
    fun expiredOrRejectedStreamRefreshesOnceThenSurfaces() {
        val rejected = StreamPlaybackFault(httpStatus = 403, detail = "Response code: 403")
        assertEquals(StreamRecoveryStep.REFRESH_URL, streamRecoveryStep(rejected, alreadyRefreshed = false))
        assertEquals(
            StreamRecoveryStep.SURFACE_FAILURE,
            streamRecoveryStep(rejected, alreadyRefreshed = true),
        )
        val unauthorized = StreamPlaybackFault(httpStatus = 401, detail = "Response code: 401")
        assertEquals(
            StreamRecoveryStep.REFRESH_URL,
            streamRecoveryStep(unauthorized, alreadyRefreshed = false),
        )
        val expired = StreamPlaybackFault(detail = "Request has expired")
        assertEquals(StreamRecoveryStep.REFRESH_URL, streamRecoveryStep(expired, alreadyRefreshed = false))
        val fromMessage = StreamPlaybackFault(detail = "Response code: 403")
        assertEquals(
            StreamRecoveryStep.REFRESH_URL,
            streamRecoveryStep(fromMessage, alreadyRefreshed = false),
        )
    }

    @Test
    fun networkMintFailureRefreshesOnce() {
        val minted = StreamPlaybackFault(
            detail = "playback stream unavailable",
            ioFailure = true,
        )
        assertEquals(StreamRecoveryStep.REFRESH_URL, streamRecoveryStep(minted, alreadyRefreshed = false))
        assertEquals(
            StreamRecoveryStep.SURFACE_FAILURE,
            streamRecoveryStep(minted, alreadyRefreshed = true),
        )
    }

    @Test
    fun premiumAndAbsenceDoNotLookLikeABadUrl() {
        assertEquals(
            StreamRecoveryStep.HOLD_FOR_PREMIUM,
            streamRecoveryStep(
                StreamPlaybackFault(detail = "premium required", ioFailure = true),
                alreadyRefreshed = false,
            ),
        )
        assertEquals(
            StreamRecoveryStep.SKIP_ABSENT,
            streamRecoveryStep(
                StreamPlaybackFault(detail = "object not found", ioFailure = true),
                alreadyRefreshed = true,
            ),
        )
    }

    @Test
    fun decoderErrorsAreNotRefreshed() {
        assertEquals(
            StreamRecoveryStep.IGNORE,
            streamRecoveryStep(
                StreamPlaybackFault(detail = "format unsupported", ioFailure = false),
                alreadyRefreshed = false,
            ),
        )
    }
}

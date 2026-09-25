package com.aethelsoft.grooveplayer.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreProgressTest {

    @Test
    fun byteBasedBarMovesThroughALargeFileAndNeverGoesBackward() {
        val progress = RestoreProgress()
        progress.planDownloads(listOf(100L, 100L))
        val steps = mutableListOf<Float>()
        steps += progress.snapshot().fraction!!

        progress.onFileBytes(0, 50L, 100L)
        steps += progress.snapshot().fraction!!
        assertEquals("Downloading 1 of 2 files", progress.snapshot().status)
        assertEquals("50 B of 100 B", progress.snapshot().detail)
        assertEquals(RestoreUiPhase.DOWNLOADING, progress.snapshot().phase)

        progress.onFileBytes(0, 100L, 100L)
        progress.beginVerify(0)
        steps += progress.snapshot().fraction!!
        assertEquals("Verifying 1 of 2 files", progress.snapshot().status)
        progress.onVerifyBytes(0, 40L, 100L)
        assertEquals("40 B of 100 B", progress.snapshot().detail)
        steps += progress.snapshot().fraction!!
        progress.finishFile(0)

        progress.beginFile(1)
        progress.onFileBytes(1, 50L, 100L)
        steps += progress.snapshot().fraction!!
        assertEquals("Downloading 2 of 2 files", progress.snapshot().status)

        progress.beginVerify(1)
        progress.finishFile(1)
        progress.onCatalogVerify()
        steps += progress.snapshot().fraction!!
        assertEquals(RestoreProgressLabel.VERIFYING_CATALOG, progress.snapshot().status)
        assertEquals(RestoreUiPhase.VERIFYING, progress.snapshot().phase)

        progress.onApplying()
        steps += progress.snapshot().fraction!!
        assertEquals(RestoreProgressLabel.APPLYING, progress.snapshot().status)
        assertEquals(RestoreUiPhase.APPLYING, progress.snapshot().phase)
        assertNull(progress.snapshot().retry)
        assertNull(progress.snapshot().detail)

        steps.zipWithNext { previous, next ->
            assertTrue("fraction went from $previous to $next", next + 0.0001f >= previous)
        }
        assertEquals(0f, steps.first(), 0.0001f)
        assertEquals(0.25f, steps[1], 0.0001f)
        assertEquals(1f, steps.last(), 0.0001f)
    }

    @Test
    fun fileCountBarUsesKnownBytesInsideTheCurrentFile() {
        val progress = RestoreProgress()
        progress.planDownloads(listOf(null, 80L))
        assertEquals(0f, progress.snapshot().fraction!!, 0.0001f)

        progress.onFileBytes(0, 40L, 80L)
        assertEquals(0.25f, progress.snapshot().fraction!!, 0.0001f)
        assertEquals("40 B of 80 B", progress.snapshot().detail)

        progress.finishFile(0)
        progress.beginFile(1)
        progress.onFileBytes(1, 40L, 80L)
        assertEquals(0.75f, progress.snapshot().fraction!!, 0.0001f)
    }

    @Test
    fun unknownFileSizeDoesNotInventASliceUntilTheFileFinishes() {
        val progress = RestoreProgress()
        progress.planDownloads(listOf(null))
        progress.onFileBytes(0, 4_000L, -1L)
        assertEquals(0f, progress.snapshot().fraction!!, 0.0001f)
        assertNull(progress.snapshot().detail)
        progress.finishFile(0)
        assertEquals(1f, progress.snapshot().fraction!!, 0.0001f)
    }

    @Test
    fun repeatedOrLowerByteReportsDoNotMoveTheBarBackward() {
        val progress = RestoreProgress()
        progress.planDownloads(listOf(100L))
        progress.onFileBytes(0, 60L, 100L)
        val atSixty = progress.snapshot().fraction!!
        progress.onFileBytes(0, 60L, 100L)
        progress.onFileBytes(0, 20L, 100L)
        assertEquals(atSixty, progress.snapshot().fraction!!, 0.0001f)
        assertEquals("60 B of 100 B", progress.snapshot().detail)
    }

    @Test
    fun retryDropsOnlyTheFailedAttemptAndShowsTheRetryLabel() {
        val progress = RestoreProgress()
        progress.planDownloads(listOf(100L, 100L))
        progress.finishFile(0)
        progress.beginFile(1)
        progress.onFileBytes(1, 80L, 100L)
        assertEquals(0.9f, progress.snapshot().fraction!!, 0.0001f)

        progress.onRetry(1, retryNumber = 1, maxAttempts = 3)
        val retried = progress.snapshot()
        assertEquals("Retrying (1/3)…", retried.retry)
        assertEquals("Downloading 2 of 2 files", retried.status)
        assertEquals(0.5f, retried.fraction!!, 0.0001f)
        assertEquals("0 B of 100 B", retried.detail)

        progress.onFileBytes(1, 40L, 100L)
        val duringRetry = progress.snapshot()
        val duringFraction = duringRetry.fraction ?: error("fraction")
        assertEquals("Retrying (1/3)…", duringRetry.retry)
        assertEquals(0.7f, duringFraction, 0.0001f)
        assertTrue(duringFraction + 0.0001f >= 0.5f)

        progress.beginVerify(1)
        val verified = progress.snapshot()
        val verifiedFraction = verified.fraction ?: error("fraction")
        assertNull(verified.retry)
        assertEquals(RestoreUiPhase.VERIFYING, verified.phase)
        assertTrue(verifiedFraction + 0.0001f >= duringFraction)
    }

    @Test
    fun librarySnapshotKeepsRetryVisibleUntilSongsArePlanned() {
        val progress = RestoreProgress()
        progress.onLibrarySnapshot(0L, 2_000L)
        assertEquals(RestoreProgressLabel.LIBRARY_SNAPSHOT, progress.snapshot().status)
        assertNull(progress.snapshot().fraction)
        assertEquals("0 B of 2.0 KB", progress.snapshot().detail)

        progress.onSnapshotRetry(1, 3)
        assertEquals("Retrying (1/3)…", progress.snapshot().retry)
        assertNull(progress.snapshot().fraction)

        progress.onLibrarySnapshot(1_000L, 2_000L)
        assertEquals("Retrying (1/3)…", progress.snapshot().retry)
        assertEquals("1000 B of 2.0 KB", progress.snapshot().detail)
        assertNull(progress.snapshot().fraction)

        progress.planDownloads(emptyList())
        assertNull(progress.snapshot().retry)
        assertEquals(1f, progress.snapshot().fraction!!, 0.0001f)
        assertEquals(RestoreUiPhase.VERIFYING, progress.snapshot().phase)
    }

    @Test
    fun applyingHoldIsLongEnoughToBeSeen() {
        assertTrue(RestoreProgressLabel.MIN_APPLYING_VISIBLE_MS >= 500L)
        assertEquals("Applying restored data…", RestoreProgressLabel.APPLYING)
    }
}

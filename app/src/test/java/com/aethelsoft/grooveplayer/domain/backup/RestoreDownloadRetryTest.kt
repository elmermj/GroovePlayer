package com.aethelsoft.grooveplayer.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CancellationException

class RestoreDownloadRetryTest {

    @Test
    fun expiredAccessTokenIsRefreshedOnceAndADeadSessionStopsTheBatch() {
        assertTrue(RestoreDownloadRetry.apiNeedsAccessRefresh(401))
        assertFalse(RestoreDownloadRetry.apiNeedsAccessRefresh(403))
        assertTrue(
            RestoreDownloadRetry.isTerminalUnauthorized(
                IllegalStateException("unauthorized — session expired. Sign in again, then retry restore."),
            ),
        )
        assertFalse(
            RestoreDownloadRetry.isTerminalUnauthorized(
                R2GetException(401, "R2 GET failed HTTP 401"),
            ),
        )
    }

    @Test
    fun presignedGetRetriesUnauthorizedForbiddenAndConnectionAbort() {
        assertTrue(RestoreDownloadRetry.r2GetShouldRetry(401, R2GetException(401, "R2 GET failed HTTP 401")))
        assertTrue(RestoreDownloadRetry.r2GetShouldRetry(403, R2GetException(403, "R2 GET failed HTTP 403")))
        assertTrue(RestoreDownloadRetry.r2GetShouldRetry(503, R2GetException(503, "R2 GET failed HTTP 503")))
        assertTrue(
            RestoreDownloadRetry.r2GetShouldRetry(
                null,
                IOException("Software caused connection abort"),
            ),
        )
        assertTrue(
            RestoreDownloadRetry.isTransientTransferError(
                IOException("timeout", IOException("stream was reset: CANCEL")),
            ),
        )
    }

    @Test
    fun missingObjectAndCancellationDoNotRetry() {
        assertFalse(RestoreDownloadRetry.r2GetShouldRetry(404, R2GetException(404, "R2 GET failed HTTP 404")))
        assertFalse(RestoreDownloadRetry.r2GetShouldRetry(400, R2GetException(400, "R2 GET failed HTTP 400")))
        assertFalse(
            RestoreDownloadRetry.r2GetShouldRetry(
                null,
                CancellationException("StandaloneCoroutine was cancelled"),
            ),
        )
        assertFalse(RestoreDownloadRetry.isTransientTransferError(null))
        assertFalse(RestoreDownloadRetry.r2GetShouldRetry(null, IOException("download-url missing download_url")))
    }

    @Test
    fun longRecordingIsNotCutOffByACallDeadline() {
        assertEquals(0, RestoreDownloadRetry.R2_CALL_TIMEOUT_MS)
        assertTrue(RestoreDownloadRetry.R2_HTTP1_ONLY)
        assertEquals(3, RestoreDownloadRetry.MAX_ATTEMPTS)
        assertEquals(400L, RestoreDownloadRetry.backoffMs(1))
        assertTrue(RestoreDownloadRetry.backoffMs(2) > RestoreDownloadRetry.backoffMs(1))
    }
}
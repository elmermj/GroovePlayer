package com.aethelsoft.grooveplayer.domain.backup

import java.io.IOException
import java.util.concurrent.CancellationException

/**
 * Restore file downloads (SCRUM-74).
 *
 * Benny access JWTs expire (default 15 minutes). Backup refreshes before upload.
 * Restore did not, so `GET /v1/backup/library` and `POST /v1/backup/download-url`
 * returned `{ "error": "unauthorized" }` and the screen stayed on Downloading.
 *
 * A 10-minute OkHttp call timeout also aborted long Call/Voice `*.m4a` GETs.
 * Android reports that as a connection abort, and one failed object failed the batch,
 * so restore never reached Applying.
 */
object RestoreDownloadRetry {
    const val MAX_ATTEMPTS = 3

    /**
     * No whole-call deadline. The read timeout still fails a stalled socket.
     * A fixed call timeout aborts a long recording while bytes are still arriving.
     */
    const val R2_CALL_TIMEOUT_MS = 0

    /**
     * Large whole-object bodies on HTTP/2 are reset mid-download.
     * The R2 client is backup PUT/GET only; playback does not use it.
     */
    const val R2_HTTP1_ONLY = true

    fun backoffMs(failedAttempt: Int): Long = when {
        failedAttempt <= 1 -> 400L
        else -> 1_200L
    }

    /** Expired or missing access JWT. Refresh once, then retry the same call. */
    fun apiNeedsAccessRefresh(httpCode: Int): Boolean = httpCode == 401

    /**
     * Benny 401 after the refresh retry. Another download attempt cannot succeed
     * until the user signs in again.
     */
    fun isTerminalUnauthorized(error: Throwable): Boolean {
        val msg = error.message?.trim()?.lowercase().orEmpty()
        return msg == "unauthorized" ||
            msg.startsWith("unauthorized ") ||
            msg.startsWith("unauthorized—") ||
            msg.startsWith("unauthorized-") ||
            msg == "http 401 unauthorized"
    }

    /**
     * Presigned GET failed in a way a new URL or another attempt can fix.
     * 401/403: signature rejected. 5xx: R2 blip. No status: connection abort / timeout.
     */
    fun r2GetShouldRetry(httpCode: Int?, error: Throwable?): Boolean {
        if (error != null && hasCancellation(error)) return false
        if (error != null && isTerminalUnauthorized(error)) return false
        if (httpCode == 401 || httpCode == 403) return true
        if (httpCode != null && httpCode in 500..599) return true
        if (httpCode != null) return false
        return isTransientTransferError(error)
    }

    fun isTransientTransferError(error: Throwable?): Boolean {
        if (error == null || hasCancellation(error)) return false
        var current: Throwable? = error
        while (current != null) {
            if (messageIsTransient(current.message)) return true
            current = current.cause
        }
        return false
    }

    private fun hasCancellation(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is CancellationException) return true
            current = current.cause
        }
        return false
    }

    private fun messageIsTransient(message: String?): Boolean {
        val text = message?.lowercase().orEmpty()
        if (text.isBlank()) return false
        return text.contains("connection abort") ||
            text.contains("connection reset") ||
            text.contains("unexpected end of stream") ||
            text.contains("stream was reset") ||
            text.contains("timeout") ||
            text.contains("broken pipe") ||
            text.contains("socket closed") ||
            text.contains("failed to connect") ||
            text.contains("econnreset") ||
            text.contains("econnaborted") ||
            text == "canceled" ||
            text == "cancelled"
    }
}

/** Whole-object R2 GET failed. [httpCode] is null when the socket died before a status. */
class R2GetException(
    val httpCode: Int?,
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

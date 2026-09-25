package com.aethelsoft.grooveplayer.domain.backup

import java.util.Locale
import kotlin.math.max

enum class RestoreUiPhase {
    DOWNLOADING,
    VERIFYING,
    APPLYING,
}

data class RestoreProgressSnapshot(
    val phase: RestoreUiPhase = RestoreUiPhase.DOWNLOADING,
    val status: String = RestoreProgressLabel.DOWNLOADING_LIBRARY,
    val retry: String? = null,
    val detail: String? = null,
    /** Null until the song download plan is known. 0f..1f after that. */
    val fraction: Float? = null,
)

object RestoreProgressLabel {
    const val DOWNLOADING_LIBRARY = "Downloading restored data…"
    const val LIBRARY_SNAPSHOT = "Downloading library snapshot…"
    const val VERIFYING_CATALOG = "Verifying restored data…"
    const val APPLYING = "Applying restored data…"

    /** Long enough for the applying screen to paint before the process restarts. */
    const val MIN_APPLYING_VISIBLE_MS = 800L

    /**
     * Hash checks finish faster than a frame, and the next status replaces them.
     * Hold the verifying line this long so it actually paints.
     */
    const val MIN_VERIFY_VISIBLE_MS = 600L

    fun downloading(current: Int, total: Int): String = "Downloading $current of $total files"

    fun verifying(current: Int, total: Int): String = "Verifying $current of $total files"

    /** [retryNumber] is the failed attempt (1 after the first failure). */
    fun retrying(retryNumber: Int, maxAttempts: Int): String =
        "Retrying ($retryNumber/$maxAttempts)…"

    fun bytes(read: Long, total: Long): String? {
        if (total <= 0L) return null
        val shown = read.coerceIn(0L, total)
        return "${formatBytes(shown)} of ${formatBytes(total)}"
    }

    fun formatBytes(bytes: Long): String {
        val n = bytes.coerceAtLeast(0L)
        if (n < 1024L) return "$n B"
        val kb = n / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
        return String.format(Locale.US, "%.1f GB", mb / 1024.0)
    }
}

/**
 * Maps restore work onto one screen.
 *
 * The bar is byte-based when every file that will be downloaded has a known size.
 * Otherwise each file is one slice, and a file whose size is known fills that slice
 * as bytes arrive so a long recording does not sit still until it finishes.
 *
 * Finished files only move the bar forward. A retry deletes the partial object, so
 * the bar returns to the last finished file instead of keeping bytes that are gone.
 * Inside one attempt, and from verifying through applying, the fraction does not decrease.
 *
 * The byte line is one total: the sum of manifest sizes from [planDownloads].
 * A file's Content-Length is never added to that total, so it cannot jump
 * between objects. The count moves forward during an attempt.
 */
class RestoreProgress {
    private var planned = false
    private var fileCount = 0
    private var sizes: List<Long?> = emptyList()
    private var learned = LongArray(0)
    private var inflight = LongArray(0)
    private var lastRead = LongArray(0)
    private var finished = BooleanArray(0)
    private var byteBased = false
    /** Sum of positive manifest sizes. Fixed for the whole download. */
    private var manifestTotal = 0L
    /** Bytes shown in the detail line. Drops only when a retry deletes a partial. */
    private var displayedBytes = 0L

    private var phase = RestoreUiPhase.DOWNLOADING
    private var status = RestoreProgressLabel.DOWNLOADING_LIBRARY
    private var retry: String? = null
    private var detail: String? = null
    private var fraction: Float? = null

    fun reset() {
        planned = false
        fileCount = 0
        sizes = emptyList()
        learned = LongArray(0)
        inflight = LongArray(0)
        lastRead = LongArray(0)
        finished = BooleanArray(0)
        byteBased = false
        manifestTotal = 0L
        displayedBytes = 0L
        phase = RestoreUiPhase.DOWNLOADING
        status = RestoreProgressLabel.DOWNLOADING_LIBRARY
        retry = null
        detail = null
        fraction = null
    }

    fun onLibrarySnapshot(bytesRead: Long, totalBytes: Long) {
        if (planned) return
        phase = RestoreUiPhase.DOWNLOADING
        status = RestoreProgressLabel.LIBRARY_SNAPSHOT
        detail = RestoreProgressLabel.bytes(bytesRead, totalBytes)
    }

    fun onSnapshotRetry(retryNumber: Int, maxAttempts: Int) {
        if (planned) return
        phase = RestoreUiPhase.DOWNLOADING
        status = RestoreProgressLabel.LIBRARY_SNAPSHOT
        retry = RestoreProgressLabel.retrying(retryNumber, maxAttempts)
        detail = null
        fraction = null
    }

    fun planDownloads(fileSizes: List<Long?>) {
        fileCount = fileSizes.size
        sizes = fileSizes.toList()
        learned = LongArray(fileCount)
        inflight = LongArray(fileCount)
        lastRead = LongArray(fileCount)
        finished = BooleanArray(fileCount)
        byteBased = fileCount > 0 && sizes.all { it != null && it > 0L }
        manifestTotal = fileSizes.sumOf { it?.takeIf { size -> size > 0L } ?: 0L }
        displayedBytes = 0L
        planned = true
        retry = null
        if (fileCount == 0) {
            phase = RestoreUiPhase.VERIFYING
            status = RestoreProgressLabel.VERIFYING_CATALOG
            detail = null
            fraction = 1f
        } else {
            phase = RestoreUiPhase.DOWNLOADING
            status = RestoreProgressLabel.downloading(1, fileCount)
            fraction = 0f
            refreshDetail(allowDrop = true)
        }
    }

    fun beginFile(index: Int) {
        if (!inRange(index)) return
        retry = null
        lastRead[index] = 0L
        inflight[index] = 0L
        phase = RestoreUiPhase.DOWNLOADING
        status = RestoreProgressLabel.downloading(index + 1, fileCount)
        refreshDetail(allowDrop = false)
        advance(allowDrop = false)
    }

    fun onFileBytes(index: Int, bytesRead: Long, totalBytes: Long) {
        if (!inRange(index) || finished[index]) return
        if (bytesRead < lastRead[index]) return
        lastRead[index] = bytesRead
        if (sizes.getOrNull(index) == null && totalBytes > 0L && learned[index] == 0L) {
            learned[index] = totalBytes
        }
        val cap = capOf(index)
        inflight[index] = if (cap != null) bytesRead.coerceIn(0L, cap) else 0L
        phase = RestoreUiPhase.DOWNLOADING
        status = RestoreProgressLabel.downloading(index + 1, fileCount)
        refreshDetail(allowDrop = false)
        advance(allowDrop = false)
    }

    fun onRetry(index: Int, retryNumber: Int, maxAttempts: Int) {
        if (!inRange(index) || finished[index]) return
        lastRead[index] = 0L
        inflight[index] = 0L
        retry = RestoreProgressLabel.retrying(retryNumber, maxAttempts)
        phase = RestoreUiPhase.DOWNLOADING
        status = RestoreProgressLabel.downloading(index + 1, fileCount)
        refreshDetail(allowDrop = true)
        advance(allowDrop = true)
    }

    fun beginVerify(index: Int) {
        if (!inRange(index) || finished[index]) return
        retry = null
        val cap = capOf(index)
        if (cap != null) {
            inflight[index] = cap
            lastRead[index] = cap
        }
        phase = RestoreUiPhase.VERIFYING
        status = RestoreProgressLabel.verifying(index + 1, fileCount)
        refreshDetail(allowDrop = false)
        advance(allowDrop = false)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onVerifyBytes(index: Int, bytesRead: Long, totalBytes: Long) {
        if (!inRange(index) || finished[index]) return
        // bytesRead/totalBytes are the hash scan. They start again at 0, so they
        // must not replace the manifest byte line.
        retry = null
        phase = RestoreUiPhase.VERIFYING
        status = RestoreProgressLabel.verifying(index + 1, fileCount)
        refreshDetail(allowDrop = false)
        advance(allowDrop = false)
    }

    fun finishFile(index: Int) {
        if (!inRange(index)) return
        finished[index] = true
        inflight[index] = 0L
        retry = null
        refreshDetail(allowDrop = false)
        advance(allowDrop = false)
    }

    /**
     * After every downloaded file has been hashed. Per-file verify updates are
     * shorter than a frame, so the screen publishes this once and holds it.
     */
    fun showVerifyingFiles() {
        if (!planned || fileCount == 0) {
            onCatalogVerify()
            return
        }
        phase = RestoreUiPhase.VERIFYING
        status = RestoreProgressLabel.verifying(fileCount, fileCount)
        retry = null
        refreshDetail(allowDrop = false)
        advance(allowDrop = false)
    }

    fun onCatalogVerify() {
        phase = RestoreUiPhase.VERIFYING
        status = RestoreProgressLabel.VERIFYING_CATALOG
        retry = null
        detail = null
        advance(allowDrop = false)
    }

    fun onApplying() {
        phase = RestoreUiPhase.APPLYING
        status = RestoreProgressLabel.APPLYING
        retry = null
        detail = null
        if (planned && (fileCount == 0 || finished.all { it })) {
            fraction = max(fraction ?: 0f, 1f)
        }
    }

    fun snapshot(): RestoreProgressSnapshot = RestoreProgressSnapshot(
        phase = phase,
        status = status,
        retry = retry,
        detail = detail,
        fraction = fraction,
    )

    private fun inRange(index: Int) = planned && index in 0 until fileCount

    /**
     * Manifest total is fixed at [planDownloads]. Learned Content-Lengths fill a
     * file's own slice of the bar, but they are not added to the displayed total.
     */
    private fun refreshDetail(allowDrop: Boolean) {
        if (!planned || manifestTotal <= 0L) {
            if (planned) detail = null
            return
        }
        val done = bytesDone().coerceIn(0L, manifestTotal)
        displayedBytes = if (allowDrop) done else max(displayedBytes, done)
        detail = RestoreProgressLabel.bytes(displayedBytes, manifestTotal)
    }

    private fun bytesDone(): Long {
        var done = 0L
        for (i in 0 until fileCount) {
            val size = sizes.getOrNull(i)?.takeIf { it > 0L } ?: continue
            done += if (finished[i]) size else inflight[i].coerceIn(0L, size)
        }
        return done
    }

    private fun capOf(index: Int): Long? {
        val plannedSize = sizes.getOrNull(index)
        if (plannedSize != null && plannedSize > 0L) return plannedSize
        val learnedSize = learned.getOrNull(index) ?: 0L
        return learnedSize.takeIf { it > 0L }
    }

    private fun advance(allowDrop: Boolean) {
        val computed = computeFraction() ?: return
        fraction = when {
            allowDrop || fraction == null -> computed
            else -> max(fraction!!, computed)
        }
    }

    private fun computeFraction(): Float? {
        if (!planned) return null
        if (fileCount == 0) return 1f
        return if (byteBased) byteFraction() else fileFraction()
    }

    private fun byteFraction(): Float {
        val total = sizes.sumOf { it ?: 0L }
        if (total <= 0L) return fileFraction()
        var done = 0L
        for (i in 0 until fileCount) {
            val size = sizes[i] ?: 0L
            done += if (finished[i]) size else inflight[i].coerceAtMost(size)
        }
        return (done.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    private fun fileFraction(): Float {
        var units = 0.0
        for (i in 0 until fileCount) {
            units += when {
                finished[i] -> 1.0
                else -> {
                    val cap = capOf(i)
                    if (cap != null && cap > 0L) {
                        inflight[i].coerceAtMost(cap).toDouble() / cap.toDouble()
                    } else {
                        0.0
                    }
                }
            }
        }
        return (units / fileCount.toDouble()).toFloat().coerceIn(0f, 1f)
    }
}

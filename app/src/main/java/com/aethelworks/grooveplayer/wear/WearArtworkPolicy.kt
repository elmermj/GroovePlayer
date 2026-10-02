package com.aethelworks.grooveplayer.wear

import com.aethelworks.grooveplayer.domain.backup.AppPrivateLibrary
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Artwork leaves the phone only for a file that already lives in the private
 * library. Shared Music paths and remote URLs are not encoded for the watch.
 */
object WearArtworkPolicy {
    const val MAX_EDGE_PX = WearProtocol.ARTWORK_MAX_EDGE_PX
    const val MAX_JPEG_BYTES = 90_000

    fun allowed(filePath: String?, privateRoot: String): Boolean {
        if (filePath.isNullOrBlank() || privateRoot.isBlank()) return false
        return AppPrivateLibrary.isInside(filePath, privateRoot)
    }

    fun fittedSize(width: Int, height: Int, maxEdge: Int = MAX_EDGE_PX): Pair<Int, Int> {
        if (width <= 0 || height <= 0 || maxEdge <= 0) return 0 to 0
        val longest = max(width, height)
        if (longest <= maxEdge) return width to height
        val scale = maxEdge.toDouble() / longest.toDouble()
        var fittedWidth = (width * scale).roundToInt().coerceAtLeast(1)
        var fittedHeight = (height * scale).roundToInt().coerceAtLeast(1)
        if (fittedWidth > maxEdge) fittedWidth = maxEdge
        if (fittedHeight > maxEdge) fittedHeight = maxEdge
        return fittedWidth to fittedHeight
    }

    fun fitsDataItem(jpegSize: Int, limit: Int = MAX_JPEG_BYTES): Boolean =
        jpegSize in 1..limit
}

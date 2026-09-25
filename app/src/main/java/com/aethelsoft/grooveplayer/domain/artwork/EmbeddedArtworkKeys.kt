package com.aethelsoft.grooveplayer.domain.artwork

/**
 * Cache identity for song artwork. The key is the song SHA-256 from `songs.contentHash`
 * plus the display edge, so a new hash misses both the memory and disk entries.
 */
object EmbeddedArtworkKeys {
    const val SCHEME = "groove-artwork"
    const val DEFAULT_EDGE_PX = 512
    const val MAX_EDGE_PX = 2048

    private val SHA256 = Regex("^[0-9a-f]{64}$")

    fun uri(contentHash: String?): String? {
        val hash = normalize(contentHash) ?: return null
        return "$SCHEME:$hash"
    }

    fun hashOf(model: String?): String? {
        if (model.isNullOrBlank()) return null
        val marker = "$SCHEME:"
        val index = model.indexOf(marker, ignoreCase = true)
        if (index < 0) return null
        var rest = model.substring(index + marker.length)
        while (rest.startsWith("/")) rest = rest.substring(1)
        val hash = rest.substringBefore('?').substringBefore('#').trim()
        return normalize(hash)
    }

    fun cacheKey(contentHash: String?, widthPx: Int, heightPx: Int): String? {
        val hash = normalize(contentHash) ?: return null
        return "$hash@${edge(widthPx, heightPx)}"
    }

    fun edge(widthPx: Int, heightPx: Int): Int {
        val longest = maxOf(widthPx, heightPx)
        if (longest <= 0) return DEFAULT_EDGE_PX
        return longest.coerceAtMost(MAX_EDGE_PX)
    }

    /**
     * Largest power-of-two sample size whose decoded bitmap still covers [edgePx].
     */
    fun sampleSize(width: Int, height: Int, edgePx: Int): Int {
        if (width <= 0 || height <= 0 || edgePx <= 0) return 1
        var sample = 1
        var sampledWidth = width
        var sampledHeight = height
        while (sampledWidth / 2 >= edgePx && sampledHeight / 2 >= edgePx) {
            sampledWidth /= 2
            sampledHeight /= 2
            sample *= 2
        }
        return sample
    }

    /**
     * Hash-keyed model when [contentHash] is a SHA-256. Otherwise the previous URL,
     * which may be empty for a private file that never had MediaStore album art.
     */
    fun displayUrl(contentHash: String?, fallback: String?): String? {
        return uri(contentHash) ?: fallback?.takeIf { it.isNotBlank() }
    }

    /**
     * Replace a blank or MediaStore (`content:`) URL with the hash-keyed model.
     * A model that already names a hash follows the new hash so the cache misses.
     */
    fun prefer(stored: String?, candidate: String?): String? {
        val model = candidate?.takeIf { hashOf(it) != null }
        if (model == null) return stored?.takeIf { it.isNotBlank() }
        if (stored.isNullOrBlank()) return model
        if (hashOf(stored) != null) return model
        if (stored.startsWith("content:", ignoreCase = true)) return model
        return stored
    }

    fun fileName(cacheKey: String): String {
        val safe = cacheKey.lowercase().filter { it.isDigit() || it in 'a'..'f' || it == '@' }
        return "$safe.jpg"
    }

    private fun normalize(contentHash: String?): String? {
        val hash = contentHash?.trim()?.lowercase() ?: return null
        return hash.takeIf { SHA256.matches(it) }
    }
}

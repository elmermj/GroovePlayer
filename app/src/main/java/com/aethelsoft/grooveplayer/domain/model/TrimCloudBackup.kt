package com.aethelsoft.grooveplayer.domain.model

/**
 * Cloud-only trim of R2 backup catalog rows (never local library files).
 * Strategies match docs/addon-cancel-trim.md / entitlements.SuggestedStrategies.
 */
enum class TrimStrategy(val apiValue: String, val label: String) {
    LATEST("latest", "Newest first"),
    OLDEST("oldest", "Oldest first"),
    SMALLEST("smallest", "Smallest first"),
    LARGEST("largest", "Largest first"),
    BY_ARTIST("by_artist", "By artist"),
    BY_ALBUM("by_album", "By album"),
    BY_YEAR("by_year", "By year"),
    RANDOM_FILL("random_fill", "Random fill"),
    ;

    companion object {
        fun fromApi(raw: String?): TrimStrategy? =
            entries.firstOrNull { it.apiValue.equals(raw, ignoreCase = true) }

        fun fromSuggested(list: List<String>): List<TrimStrategy> {
            val mapped = list.mapNotNull { fromApi(it) }
            return mapped.ifEmpty { entries.toList() }
        }
    }
}

data class TrimCloudBackupRequest(
    val strategy: TrimStrategy,
    val artist: String? = null,
    val album: String? = null,
    val year: Int? = null,
    /** Optional cap for multi-step trim; null = free until under quota. */
    val limitBytes: Long? = null,
)

data class TrimCloudBackupResult(
    val strategy: String,
    val deleted: Int,
    val bytesFreed: Long,
)

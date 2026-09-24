package com.aethelsoft.grooveplayer.domain.model

/**
 * One track line from an M3U / M3U8 playlist.
 * [location] is kept as written (path or URI). Matching against the library
 * normalizes it separately so export can round-trip the local file path.
 */
data class M3uTrack(
    val location: String,
    val title: String? = null,
    val durationSeconds: Long = -1,
)

data class M3uDocument(
    val name: String? = null,
    val tracks: List<M3uTrack> = emptyList(),
)

sealed class M3uImportResult {
    data class Success(
        val playlistId: Long,
        val playlistName: String,
        val importedCount: Int,
        val missingLocations: List<String>,
    ) : M3uImportResult()

    data class Failure(val message: String) : M3uImportResult()
}

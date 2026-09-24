package com.aethelsoft.grooveplayer.domain.backup

/**
 * A cloud song after its bytes have a single path under Groove Downloads.
 * [logicalPath] is the path stored in the backup catalog (cosmetic).
 */
data class PlacedCloudSong(
    val contentHash: String,
    val sizeBytes: Long,
    val logicalPath: String?,
    val localPath: String,
)

object BackupCatalogPaths {
    /**
     * Point a snapshot `sourcePath` at the local file for the same catalog entry.
     * Exact logical paths win. A unique basename match covers older snapshots whose
     * absolute path was on another device. Unmatched paths stay as stored.
     */
    fun rewrite(sourcePath: String, placements: List<PlacedCloudSong>): String {
        placements.firstOrNull { it.logicalPath == sourcePath }?.let { return it.localPath }
        val base = GrooveDownloadPlacement.fileName(sourcePath)
        if (base.isEmpty()) return sourcePath
        val byName = placements.filter { placement ->
            val logical = placement.logicalPath ?: return@filter false
            GrooveDownloadPlacement.fileName(logical) == base
        }
        return if (byName.size == 1) byName.single().localPath else sourcePath
    }

    /**
     * Required bytes are the cloud song objects, not every Room row.
     * Local-only tracks that were never uploaded are absent from [placements].
     */
    fun missingLocalBytes(
        placements: List<PlacedCloudSong>,
        lengthOf: (String) -> Long,
    ): List<String> {
        return placements
            .groupBy { it.localPath }
            .filter { (path, group) ->
                val actual = lengthOf(path)
                val expected = group.first().sizeBytes
                actual < 0L || (expected > 0L && actual != expected)
            }
            .keys
            .toList()
    }
}

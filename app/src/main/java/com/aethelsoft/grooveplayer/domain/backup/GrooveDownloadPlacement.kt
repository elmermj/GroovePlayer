package com.aethelsoft.grooveplayer.domain.backup

/**
 * Where a backed-up song lands inside Groove Downloads.
 * Same SHA-256 and size reuse the existing file. A free cosmetic name is kept.
 * A name already used by different bytes becomes `<sha256>.<ext>`, never `track (1).mp3`.
 */
data class HashedAudio(
    val path: String,
    val contentHash: String,
    val sizeBytes: Long,
)

data class DownloadPlacement(
    val destinationPath: String,
    val reusedExisting: Boolean,
)

object GrooveDownloadPlacement {
    const val FOLDER_NAME = "Groove Downloads"

    fun isInside(path: String, downloadsDir: String): Boolean {
        val root = downloadsDir.trimEnd('/', '\\').replace('\\', '/')
        val normalized = path.replace('\\', '/')
        return normalized == root || normalized.startsWith("$root/")
    }

    fun place(
        downloadsDir: String,
        cosmeticFileName: String,
        contentHash: String,
        sizeBytes: Long,
        existing: List<HashedAudio>,
    ): DownloadPlacement {
        val hash = contentHash.lowercase()
        existing.firstOrNull { audio ->
            audio.contentHash.equals(hash, ignoreCase = true) && audio.sizeBytes == sizeBytes
        }?.let { match ->
            return DownloadPlacement(destinationPath = match.path, reusedExisting = true)
        }

        val occupied = existing.map { fileName(it.path) }.toMutableSet()
        val cosmetic = sanitizeFileName(cosmeticFileName, hash)
        val name = if (cosmetic !in occupied) cosmetic else hashedFileName(cosmetic, hash)
        val root = downloadsDir.trimEnd('/', '\\')
        return DownloadPlacement(
            destinationPath = "$root/$name",
            reusedExisting = false,
        )
    }

    fun sanitizeFileName(raw: String, contentHash: String): String {
        val base = fileName(raw).trim().replace(Regex("[\\\\/]"), "_")
        if (base.isEmpty() || base == "." || base == "..") {
            return hashedFileName("audio.bin", contentHash)
        }
        return base
    }

    fun hashedFileName(cosmetic: String, contentHash: String): String {
        val ext = cosmetic.substringAfterLast('.', "")
        val hash = contentHash.lowercase()
        val usableExt = ext.isNotEmpty() && ext != cosmetic && ext.length <= 8 &&
            ext.all { it.isLetterOrDigit() }
        return if (usableExt) "$hash.$ext" else hash
    }

    fun fileName(path: String): String =
        path.substringAfterLast('/').substringAfterLast('\\')
}

/** Cloud catalog identity. A re-backup skips only when hash and size both match. */
object CloudHashDedup {
    fun alreadyStored(
        cloud: Collection<Pair<String, Long>>,
        contentHash: String,
        sizeBytes: Long,
    ): Boolean = cloud.any { (hash, size) ->
        hash.equals(contentHash, ignoreCase = true) && size == sizeBytes
    }
}

package com.aethelsoft.grooveplayer.domain.backup

import java.io.File

/**
 * Consolidated backup audio is stored only in app-private storage.
 * Shared `Music/Groove Downloads` is never created or written: on Android 10+
 * MediaProvider owns that directory and direct creates fail with EPERM even when
 * [java.io.File.canWrite] is true.
 */
fun interface AppLibraryPaths {
    fun isInside(path: String): Boolean
}

object AppPrivateLibrary {
    const val FOLDER_NAME = "groove-library"

    /** Older builds wrote this name under shared Music and under app external files. */
    const val LEGACY_SHARED_FOLDER = "Groove Downloads"

    fun isScratchFile(name: String): Boolean {
        return name.endsWith(".tmp") ||
            name.endsWith(".partial") ||
            name.startsWith(".groove_write_")
    }

    fun isInside(path: String, root: String): Boolean =
        GrooveDownloadPlacement.isInside(path, root)

    /** External app files first, then internal files. Shared Music is not a candidate. */
    fun privateCandidates(externalFilesDir: File?, filesDir: File): List<File> {
        return listOfNotNull(
            externalFilesDir?.let { File(it, FOLDER_NAME) },
            File(filesDir, FOLDER_NAME),
        )
    }

    fun firstCreatable(candidates: List<File>, canCreate: (File) -> Boolean): File? =
        candidates.firstOrNull(canCreate)

    /**
     * Directories a previous build may have filled. Callers must not create these.
     * Shared Music leftovers stay on disk. App-owned leftovers can be removed after copy.
     */
    fun legacyCandidates(
        publicMusicDir: File,
        externalFilesDir: File?,
        filesDir: File,
    ): List<LegacyLibraryDir> {
        return listOfNotNull(
            LegacyLibraryDir(File(publicMusicDir, LEGACY_SHARED_FOLDER), deleteAfterCopy = false),
            externalFilesDir?.let {
                LegacyLibraryDir(File(it, "Music/$LEGACY_SHARED_FOLDER"), deleteAfterCopy = true)
            },
            externalFilesDir?.let {
                LegacyLibraryDir(File(it, LEGACY_SHARED_FOLDER), deleteAfterCopy = true)
            },
            LegacyLibraryDir(File(filesDir, LEGACY_SHARED_FOLDER), deleteAfterCopy = true),
        )
    }
}

data class LegacyLibraryDir(
    val directory: File,
    val deleteAfterCopy: Boolean,
)

data class LegacyLibraryCopy(
    val source: File,
    val destination: File,
    val needsBytes: Boolean,
    val deleteSourceAfterCopy: Boolean,
)

object LegacyLibraryAdoption {
    fun plan(legacyDirs: List<LegacyLibraryDir>, privateRoot: File): List<LegacyLibraryCopy> {
        val root = privateRoot.absolutePath
        val reserved = privateRoot.listFiles()?.map { it.name }?.toMutableSet() ?: mutableSetOf()
        val planned = mutableListOf<LegacyLibraryCopy>()
        val seen = HashSet<String>()
        for (legacy in legacyDirs) {
            val dir = legacy.directory
            if (!dir.isDirectory || dir.absolutePath == root) continue
            val children = dir.listFiles() ?: continue
            for (source in children) {
                if (!source.isFile || source.length() <= 0L) continue
                if (AppPrivateLibrary.isScratchFile(source.name)) continue
                if (!source.canRead()) continue
                if (!seen.add(source.absolutePath)) continue
                val (dest, needsBytes) = destination(privateRoot, source, reserved)
                planned += LegacyLibraryCopy(
                    source = source,
                    destination = dest,
                    needsBytes = needsBytes,
                    deleteSourceAfterCopy = legacy.deleteAfterCopy,
                )
            }
        }
        return planned
    }

    /**
     * Same cosmetic name is reused only when SHA-256 and size both match.
     * A different file uses the content-hash name from [GrooveDownloadPlacement].
     * If that name is already a different file, the next free suffix is chosen.
     * An existing private file is never the copy target.
     */
    private fun destination(
        root: File,
        source: File,
        reserved: MutableSet<String>,
    ): Pair<File, Boolean> {
        val sourceHash = lazy { ContentHash.sha256(source) }
        val direct = File(root, source.name)
        if (sameBytes(direct, source, sourceHash)) return direct to false
        if (!direct.exists() && source.name !in reserved) {
            reserved += source.name
            return direct to true
        }
        val hashedName = GrooveDownloadPlacement.hashedFileName(source.name, sourceHash.value)
        val hashed = File(root, hashedName)
        if (sameBytes(hashed, source, sourceHash)) return hashed to false
        val free = freeName(root, hashedName, reserved)
        reserved += free
        return File(root, free) to true
    }

    private fun sameBytes(candidate: File, source: File, sourceHash: Lazy<String>): Boolean {
        if (!candidate.isFile || candidate.length() != source.length() || candidate.length() <= 0L) {
            return false
        }
        return ContentHash.sha256(candidate).equals(sourceHash.value, ignoreCase = true)
    }

    private fun freeName(root: File, preferred: String, reserved: MutableSet<String>): String {
        if (preferred !in reserved && !File(root, preferred).exists()) return preferred
        val ext = preferred.substringAfterLast('.', "")
        val usableExt = ext.isNotEmpty() && ext != preferred && ext.length <= 8 &&
            ext.all { it.isLetterOrDigit() }
        val stem = if (usableExt) preferred.substringBeforeLast('.') else preferred
        var index = 2
        while (true) {
            val name = if (usableExt) "${stem}__$index.$ext" else "${stem}__$index"
            if (name !in reserved && !File(root, name).exists()) return name
            index++
        }
    }
}

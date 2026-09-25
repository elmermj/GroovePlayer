package com.aethelsoft.grooveplayer.data.backup

import android.content.Context
import android.os.Environment
import com.aethelsoft.grooveplayer.domain.backup.AppLibraryPaths
import com.aethelsoft.grooveplayer.domain.backup.AppPrivateLibrary
import com.aethelsoft.grooveplayer.domain.backup.LegacyLibraryDir
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-private folder for consolidated backup audio.
 * `getExternalFilesDir(null)/groove-library`, then `filesDir/groove-library`.
 * Shared `Music/Groove Downloads` is never created or written.
 */
@Singleton
class GrooveDownloadsLocator @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppLibraryPaths {
    @Volatile
    private var cachedRoot: File? = null

    fun directory(): File {
        cachedRoot?.takeIf { it.isDirectory }?.let { return it }
        return synchronized(this) {
            cachedRoot?.takeIf { it.isDirectory } ?: resolve().also { cachedRoot = it }
        }
    }

    override fun isInside(path: String): Boolean =
        AppPrivateLibrary.isInside(path, directory().absolutePath)

    /**
     * Existing leftover folders only. Does not create shared Music or any legacy path.
     */
    fun legacyDirectories(): List<LegacyLibraryDir> {
        val current = directory().absolutePath
        @Suppress("DEPRECATION")
        val publicMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        return AppPrivateLibrary.legacyCandidates(
            publicMusicDir = publicMusic,
            externalFilesDir = context.getExternalFilesDir(null),
            filesDir = context.filesDir,
        ).filter { spec ->
            spec.directory.isDirectory && spec.directory.absolutePath != current
        }
    }

    private fun resolve(): File {
        val candidates = AppPrivateLibrary.privateCandidates(
            externalFilesDir = context.getExternalFilesDir(null),
            filesDir = context.filesDir,
        )
        val chosen = AppPrivateLibrary.firstCreatable(candidates, GrooveLibraryWriteProbe::canCreateFile)
        if (chosen != null) return chosen
        val internal = File(context.filesDir, AppPrivateLibrary.FOLDER_NAME)
        internal.mkdirs()
        return internal
    }
}

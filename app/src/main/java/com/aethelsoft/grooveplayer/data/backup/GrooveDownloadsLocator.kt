package com.aethelsoft.grooveplayer.data.backup

import android.content.Context
import android.os.Environment
import com.aethelsoft.grooveplayer.domain.backup.GrooveDownloadPlacement
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One folder for backed-up audio: public `Music/Groove Downloads` when that directory
 * can be created, otherwise the app-specific external-files folder of the same name.
 */
@Singleton
class GrooveDownloadsLocator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun directory(): File {
        @Suppress("DEPRECATION")
        val publicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            GrooveDownloadPlacement.FOLDER_NAME,
        )
        if ((publicDir.isDirectory || publicDir.mkdirs()) && publicDir.canWrite()) {
            return publicDir
        }
        val appDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir,
            GrooveDownloadPlacement.FOLDER_NAME,
        )
        appDir.mkdirs()
        return appDir
    }
}

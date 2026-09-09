package com.aethelsoft.grooveplayer.data.transfer

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.aethelsoft.grooveplayer.utils.helpers.logShareNearbyP2PTag
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes received audio out of app-private staging into MediaStore so it
 * appears under Music / Files and in GroovePlayer's MediaStore-backed library.
 *
 * App-specific dirs (`Android/data/...`) are never indexed by MediaScanner —
 * scanFile returns a null URI there (observed on Android 16).
 */
@Singleton
class ReceivedMediaPublisher @Inject constructor() {

    /**
     * Copies [sourceFile] into `Music/Groove Downloads/[displayName]` via MediaStore.
     * @return content [Uri] on success, null on failure
     */
    fun publishToMusicDownloads(
        context: Context,
        sourceFile: File,
        displayName: String,
    ): Uri? {
        val tag = logShareNearbyP2PTag(context)
        if (!sourceFile.exists() || sourceFile.length() <= 0L) {
            Log.w(tag, "Publisher: skip missing/empty ${sourceFile.absolutePath}")
            return null
        }

        val mime = mimeForFileName(displayName)
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, mime)
            put(MediaStore.Audio.Media.TITLE, displayName.substringBeforeLast('.'))
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MUSIC}/Groove Downloads"
                )
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            } else {
                @Suppress("DEPRECATION")
                val destDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    "Groove Downloads"
                ).apply { mkdirs() }
                @Suppress("DEPRECATION")
                put(MediaStore.Audio.Media.DATA, File(destDir, displayName).absolutePath)
            }
        }

        val uri = try {
            resolver.insert(collection, values)
        } catch (e: Exception) {
            Log.e(tag, "Publisher: MediaStore insert failed for $displayName", e)
            null
        } ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            } ?: run {
                Log.e(tag, "Publisher: openOutputStream null for $uri")
                resolver.delete(uri, null, null)
                return null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pending = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                resolver.update(uri, pending, null, null)
            }

            Log.d(
                tag,
                "Publisher: published ${sourceFile.name} (${sourceFile.length()} bytes) -> $uri " +
                    "(Music/Groove Downloads/$displayName)"
            )
            uri
        } catch (e: Exception) {
            Log.e(tag, "Publisher: copy failed for $displayName", e)
            try {
                resolver.delete(uri, null, null)
            } catch (_: Exception) {
            }
            null
        }
    }

    fun mimeForFileName(fileName: String): String =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a", "mp4", "aac" -> "audio/mp4"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "ogg", "opus" -> "audio/ogg"
            "wma" -> "audio/x-ms-wma"
            else -> "audio/*"
        }
}

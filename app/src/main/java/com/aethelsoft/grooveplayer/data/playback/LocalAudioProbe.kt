package com.aethelsoft.grooveplayer.data.playback

import android.content.Context
import android.net.Uri
import com.aethelsoft.grooveplayer.domain.playback.LocalAudioAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAudioProbe @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocalAudioAvailability {

    override fun isReadable(uri: String, filePath: String?): Boolean {
        if (isFile(filePath)) return true
        if (uri.isBlank()) return false
        val parsed = Uri.parse(uri)
        return when (parsed.scheme?.lowercase()) {
            null -> isFile(uri)
            "file" -> isFile(parsed.path)
            "content" -> isContent(parsed)
            else -> false
        }
    }

    private fun isFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return try {
            val file = File(path)
            file.isFile && file.length() > 0L
        } catch (_: Exception) {
            false
        }
    }

    private fun isContent(uri: Uri): Boolean {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize > 0L
            } == true
        } catch (_: Exception) {
            false
        }
    }
}

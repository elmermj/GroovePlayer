package com.aethelsoft.grooveplayer.data.repository

import android.content.Context
import android.net.Uri
import com.aethelsoft.grooveplayer.domain.model.AudioTags
import com.aethelsoft.grooveplayer.domain.repository.AudioTagRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioTagRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioTagRepository {

    override suspend fun readTags(contentUri: String): AudioTags? = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(contentUri)
            val mime = mimeType(uri)
            val file = localFile(uri)
            if (file != null) {
                AudioTagFiles.read(file, mime)
            } else {
                readContent(uri, mime)
            }
        }.getOrElse {
            android.util.Log.e(TAG, "Failed to read tags from $contentUri", it)
            null
        }
    }

    override suspend fun writeTags(
        contentUri: String,
        tags: AudioTags,
        replaceFrontCover: Boolean,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(contentUri)
            val mime = mimeType(uri)
            val file = localFile(uri)
            if (file != null) {
                AudioTagFiles.replace(file, tags, replaceFrontCover, mime)
            } else {
                writeContent(uri, tags, replaceFrontCover, mime)
            }
        }
    }

    private fun readContent(uri: Uri, mime: String?): AudioTags {
        val temp = stageContent(uri, mime)
        try {
            return AudioTagFiles.read(temp, mime)
        } finally {
            temp.delete()
        }
    }

    private fun writeContent(
        uri: Uri,
        tags: AudioTags,
        replaceFrontCover: Boolean,
        mime: String?,
    ) {
        val temp = stageContent(uri, mime)
        val backup = File(temp.parentFile, "${temp.name}.bak")
        try {
            temp.copyTo(backup, overwrite = true)
            AudioTagFiles.editCopy(temp, tags, replaceFrontCover, mime)
            var started = false
            try {
                val output = context.contentResolver.openOutputStream(uri, "w")
                    ?: throw IllegalStateException(
                        "Can't open this song for writing. The song was left unchanged.",
                    )
                started = true
                output.use { out ->
                    temp.inputStream().use { input -> input.copyTo(out) }
                }
            } catch (error: Exception) {
                if (started) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { out ->
                            backup.inputStream().use { input -> input.copyTo(out) }
                        }
                    }
                }
                if (error is IllegalStateException &&
                    error.message.orEmpty().contains("left unchanged")
                ) {
                    throw error
                }
                throw IllegalStateException(
                    "Couldn't save tags. The song was left unchanged.",
                    error,
                )
            }
        } finally {
            temp.delete()
            backup.delete()
        }
    }

    private fun stageContent(uri: Uri, mime: String?): File {
        val header = context.contentResolver.openInputStream(uri)?.use { input ->
            val buf = ByteArray(128)
            var offset = 0
            while (offset < buf.size) {
                val read = input.read(buf, offset, buf.size - offset)
                if (read < 0) break
                offset += read
            }
            if (offset == buf.size) buf else buf.copyOf(offset)
        } ?: throw IllegalStateException("Can't open this song. The song was left unchanged.")
        val name = uri.lastPathSegment
        val format = AudioContainerFormat.detect(name, mime, header)
        val suffix = format.writerSuffix ?: "bin"
        val temp = File.createTempFile("tag_", ".$suffix", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Can't open this song. The song was left unchanged.")
        return temp
    }

    private fun localFile(uri: Uri): File? {
        val scheme = uri.scheme?.lowercase()
        if (scheme != null && scheme != "file") return null
        val path = uri.path ?: return null
        return File(path).takeIf { it.isFile }
    }

    private fun mimeType(uri: Uri): String? =
        runCatching { context.contentResolver.getType(uri) }.getOrNull()

    private companion object {
        const val TAG = "AudioTagRepository"
    }
}

package com.aethelsoft.grooveplayer.domain.usecase.transfer

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.aethelsoft.grooveplayer.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Resolves song URIs to file paths for transfer.
 * Uses MediaStore DATA column when available; falls back to content URI path for compatibility.
 */
class GetTransferFilePathsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend operator fun invoke(songs: List<Song>): List<String> = withContext(Dispatchers.IO) {
        songs.mapNotNull { song -> resolvePath(song.uri) }
    }

    private fun resolvePath(uriString: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            when (uri.scheme) {
                "file" -> uri.path
                "content" -> resolveContentUri(uri)
                else -> uri.path
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveContentUri(uri: android.net.Uri): String? {
        return try {
            val id = ContentUris.parseId(uri)
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media._ID}=?",
                arrayOf(id.toString()),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    if (idx >= 0) {
                        val path = cursor.getString(idx)
                        if (path != null && File(path).exists()) path else null
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}

package com.aethelsoft.grooveplayer.data.playback

import android.content.Context
import com.aethelsoft.grooveplayer.di.NetworkModule
import com.aethelsoft.grooveplayer.domain.playback.CloudPlaybackCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Leftover whole-object copies. A file already here still plays as local.
 * New cloud playback uses ExoPlayer on `stream_url` (Range allowed) and does
 * not download through this store. Purge still deletes the file.
 */
@Singleton
class CloudPlaybackCacheStore @Inject constructor(
    @ApplicationContext context: Context,
    @Named(NetworkModule.R2_HTTP_CLIENT) private val r2HttpClient: OkHttpClient,
) : CloudPlaybackCache {

    private val dir = File(context.cacheDir, "cloud-audio")

    override fun existingUri(songId: String): String? {
        val file = fileFor(songId)
        return if (file.isFile && file.length() > 0L) file.toURI().toString() else null
    }

    override suspend fun storeFromUrl(songId: String, url: String): String? = withContext(Dispatchers.IO) {
        val dest = fileFor(songId)
        val tmp = File(dest.parentFile, dest.name + ".part")
        try {
            val request = Request.Builder().url(url).get().build()
            r2HttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body ?: return@withContext null
                dest.parentFile?.mkdirs()
                FileOutputStream(tmp).use { out ->
                    body.byteStream().use { input -> input.copyTo(out) }
                }
            }
            if (!tmp.isFile || tmp.length() <= 0L) {
                tmp.delete()
                return@withContext null
            }
            if (dest.exists() && !dest.delete()) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            } else if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            dest.toURI().toString()
        } catch (_: Exception) {
            tmp.delete()
            null
        }
    }

    override fun delete(songId: String) {
        fileFor(songId).delete()
        File(dir, fileName(songId) + ".part").delete()
    }

    private fun fileFor(songId: String): File = File(dir, fileName(songId))

    private fun fileName(songId: String): String =
        songId.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifBlank { "song" }
}

package com.aethelsoft.grooveplayer.data.artwork

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import coil3.ImageLoader
import coil3.Uri
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.size.pxOrElse
import com.aethelsoft.grooveplayer.domain.artwork.EmbeddedArtworkCache
import com.aethelsoft.grooveplayer.domain.artwork.EmbeddedArtworkKeys
import com.aethelsoft.grooveplayer.domain.artwork.FolderArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/** Memory-cache key for [EmbeddedArtworkFetcher]. A new content hash is a new key. */
class EmbeddedArtworkKeyer : Keyer<Uri> {
    override fun key(data: Uri, options: Options): String? {
        val hash = EmbeddedArtworkKeys.hashOf(data.toString()) ?: return null
        return EmbeddedArtworkKeys.cacheKey(
            hash,
            options.size.width.pxOrElse { 0 },
            options.size.height.pxOrElse { 0 },
        )
    }
}

/**
 * Loads a song's embedded picture, then `cover.jpg` / `folder.jpg` in the same folder.
 * Decode and cache work happen off the main thread. A miss returns null so the
 * existing artwork placeholder is shown.
 */
class EmbeddedArtworkFetcher(
    private val model: String,
    private val options: Options,
    private val files: SongArtworkFiles,
    private val cache: EmbeddedArtworkCache,
) : Fetcher {

    override suspend fun fetch(): ImageFetchResult? = withContext(Dispatchers.IO) {
        val hash = EmbeddedArtworkKeys.hashOf(model) ?: return@withContext null
        val edge = EmbeddedArtworkKeys.edge(
            options.size.width.pxOrElse { 0 },
            options.size.height.pxOrElse { 0 },
        )
        val key = EmbeddedArtworkKeys.cacheKey(hash, edge, edge) ?: return@withContext null
        cached(key, edge)?.let { return@withContext it }

        val audio = files.audioFile(hash) ?: return@withContext null
        val bytes = embeddedPicture(audio) ?: folderCover(audio) ?: return@withContext null
        val bitmap = decode(bytes, edge) ?: return@withContext null
        val jpeg = jpeg(bitmap)
        if (jpeg.isNotEmpty()) cache.write(key, jpeg)
        ImageFetchResult(
            image = bitmap.asImage(shareable = true),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    private fun cached(key: String, edge: Int): ImageFetchResult? {
        val bytes = cache.read(key) ?: return null
        val bitmap = decode(bytes, edge) ?: return null
        return ImageFetchResult(
            image = bitmap.asImage(shareable = true),
            isSampled = true,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory(
        private val files: SongArtworkFiles,
        private val cache: EmbeddedArtworkCache,
    ) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (EmbeddedArtworkKeys.hashOf(data.toString()) == null) return null
            return EmbeddedArtworkFetcher(data.toString(), options, files, cache)
        }
    }
}

private fun embeddedPicture(audio: File): ByteArray? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(audio.absolutePath)
        retriever.embeddedPicture?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun folderCover(audio: File): ByteArray? {
    val cover = FolderArtwork.find(audio.parentFile) ?: return null
    return runCatching { cover.readBytes() }.getOrNull()?.takeIf { it.isNotEmpty() }
}

private fun decode(bytes: ByteArray, edge: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = EmbeddedArtworkKeys.sampleSize(bounds.outWidth, bounds.outHeight, edge)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

private fun jpeg(bitmap: Bitmap): ByteArray {
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    return out.toByteArray()
}

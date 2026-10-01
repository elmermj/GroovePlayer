package com.aethelworks.grooveplayer.wear

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.aethelworks.grooveplayer.domain.artwork.EmbeddedArtworkKeys
import com.aethelworks.grooveplayer.domain.artwork.FolderArtwork
import java.io.ByteArrayOutputStream
import java.io.File

/** JPEG no larger than [WearArtworkPolicy.MAX_EDGE_PX] on the long edge. */
object WearArtworkEncoder {
    fun jpegFromAudio(file: File, maxEdge: Int = WearArtworkPolicy.MAX_EDGE_PX): ByteArray? {
        if (!file.isFile || !file.canRead()) return null
        val bytes = embeddedPicture(file) ?: folderCover(file) ?: return null
        return jpeg(bytes, maxEdge)
    }

    fun jpeg(bytes: ByteArray, maxEdge: Int = WearArtworkPolicy.MAX_EDGE_PX): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val decoded = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = EmbeddedArtworkKeys.sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ) ?: return null
        val (width, height) = WearArtworkPolicy.fittedSize(decoded.width, decoded.height, maxEdge)
        if (width <= 0 || height <= 0) {
            decoded.recycle()
            return null
        }
        val scaled = if (width == decoded.width && height == decoded.height) {
            decoded
        } else {
            Bitmap.createScaledBitmap(decoded, width, height, true)
        }
        val encoded = compress(scaled)
        if (scaled !== decoded) decoded.recycle()
        scaled.recycle()
        return encoded?.takeIf { WearArtworkPolicy.fitsDataItem(it.size) }
    }

    private fun compress(bitmap: Bitmap): ByteArray? {
        val qualities = intArrayOf(75, 55, 40)
        var last: ByteArray? = null
        for (quality in qualities) {
            val out = ByteArrayOutputStream()
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) continue
            val bytes = out.toByteArray()
            if (bytes.isEmpty()) continue
            last = bytes
            if (WearArtworkPolicy.fitsDataItem(bytes.size)) return bytes
        }
        return last?.takeIf { WearArtworkPolicy.fitsDataItem(it.size) }
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
}

package com.aethelsoft.grooveplayer.data.playback

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.aethelsoft.grooveplayer.domain.playback.PLAYBACK_STREAM_SCHEME
import com.aethelsoft.grooveplayer.domain.playback.PlaybackStreamTickets
import com.aethelsoft.grooveplayer.domain.playback.playbackStreamSongId
import com.aethelsoft.grooveplayer.domain.playback.streamCacheKey
import com.aethelsoft.grooveplayer.domain.playback.streamCacheLimitBytes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InterruptedIOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Disk cache for premium `groove-playback://` streams.
 *
 * The cache key is the playback URI (plus content hash), not the signed URL, so a
 * refreshed URL still reads bytes already stored. Local files are not copied here.
 * The cap follows free space so the cache cannot fill the volume.
 *
 * One [SimpleCache] per directory. This singleton owns that instance.
 */
@OptIn(UnstableApi::class)
@Singleton
class StreamPlaybackCache @Inject constructor(
    @ApplicationContext context: Context,
    resolver: PlaybackStreamResolver,
    tickets: PlaybackStreamTickets,
) {
    private val diskCache: Cache?
    private val cloudFactory: CacheDataSource.Factory?
    val playbackDataSourceFactory: DataSource.Factory

    @Volatile
    private var activeWriter: CacheWriter? = null

    init {
        val http = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
        val resolving = ResolvingDataSource.Factory(http) { spec ->
            resolver.resolve(spec)
        }
        val limit = streamCacheLimitBytes(context.cacheDir.usableSpace)
        val cache = openCache(context, limit)
        diskCache = cache
        cloudFactory = if (cache == null) {
            null
        } else {
            CacheDataSource.Factory()
                .setCache(cache)
                .setCacheKeyFactory(streamKeyFactory(tickets))
                .setUpstreamDataSourceFactory(resolving)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        }
        val cloud: DataSource.Factory = cloudFactory ?: resolving
        val local = DefaultDataSource.Factory(context, http)
        playbackDataSourceFactory = DataSource.Factory {
            PlaybackRoutingDataSource(local.createDataSource(), cloud.createDataSource())
        }
        if (cache == null) {
            android.util.Log.i(TAG, "Cloud stream disk cache off (limit=$limit)")
        } else {
            android.util.Log.i(TAG, "Cloud stream disk cache ${limit / (1024 * 1024)} MB")
        }
    }

    /**
     * Pull at most [maxBytes] of [streamUri] into the cache.
     * [stillActive] is checked while bytes arrive so a queue change can stop the download.
     * Failures are logged and ignored: playback still opens the track itself.
     */
    fun prefetchPrefix(streamUri: String, maxBytes: Long, stillActive: () -> Boolean = { true }) {
        val factory = cloudFactory ?: return
        val cache = diskCache ?: return
        if (!stillActive() || maxBytes <= 0L) return
        if (prefixSatisfied(cache, factory, streamUri, maxBytes)) return
        val dataSource = factory.createDataSource()
        val spec = DataSpec.Builder()
            .setUri(Uri.parse(streamUri))
            .setPosition(0)
            .setLength(maxBytes)
            .build()
        val writer = CacheWriter(dataSource, spec, null) { _, _, _ ->
            if (!stillActive()) activeWriter?.cancel()
        }
        activeWriter = writer
        try {
            if (!stillActive()) {
                writer.cancel()
                return
            }
            writer.cache()
        } catch (_: InterruptedIOException) {
            // Queue moved on. The prefix already stored stays usable.
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Next-track prefetch stopped: ${e.message}")
        } finally {
            if (activeWriter === writer) activeWriter = null
        }
    }

    fun cancelPrefetch() {
        activeWriter?.cancel()
    }

    private fun prefixSatisfied(
        cache: Cache,
        factory: CacheDataSource.Factory,
        uri: String,
        maxBytes: Long,
    ): Boolean {
        val key = factory.cacheKeyFactory.buildCacheKey(
            DataSpec.Builder().setUri(Uri.parse(uri)).build(),
        )
        if (cache.getCachedBytes(key, 0, maxBytes) >= maxBytes) return true
        val length = ContentMetadata.getContentLength(cache.getContentMetadata(key))
        return length > 0L &&
            length != C.LENGTH_UNSET.toLong() &&
            cache.getCachedBytes(key, 0, length) >= length
    }

    private fun openCache(context: Context, limitBytes: Long): Cache? {
        if (limitBytes <= 0L) return null
        val dir = File(context.cacheDir, CACHE_DIR)
        return try {
            dir.mkdirs()
            SimpleCache(
                dir,
                LeastRecentlyUsedCacheEvictor(limitBytes),
                StandaloneDatabaseProvider(context),
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Cloud stream cache unavailable", e)
            null
        }
    }

    private fun streamKeyFactory(tickets: PlaybackStreamTickets): CacheKeyFactory {
        return CacheKeyFactory { spec ->
            val uri = spec.uri.toString()
            val hash = playbackStreamSongId(uri)?.let { tickets.get(it)?.contentHash }
            streamCacheKey(uri, hash)
        }
    }

    private companion object {
        const val TAG = "StreamPlaybackCache"
        const val CACHE_DIR = "exo-stream"
    }
}

/**
 * Cloud streams go through the disk cache. Local content and file URIs do not,
 * so a song that is already on the device is not copied into the cache.
 */
@OptIn(UnstableApi::class)
private class PlaybackRoutingDataSource(
    private val local: DataSource,
    private val cloud: DataSource,
) : DataSource {
    private var active: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        local.addTransferListener(transferListener)
        cloud.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val chosen = if (dataSpec.uri.scheme == PLAYBACK_STREAM_SCHEME) cloud else local
        active = chosen
        return try {
            chosen.open(dataSpec)
        } catch (e: Exception) {
            active = null
            runCatching { chosen.close() }
            throw e
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val source = active ?: throw java.io.IOException("playback data source is not open")
        return source.read(buffer, offset, length)
    }

    override fun getUri(): Uri? = active?.uri

    override fun getResponseHeaders(): Map<String, List<String>> {
        return active?.responseHeaders ?: emptyMap()
    }

    override fun close() {
        try {
            active?.close()
        } finally {
            active = null
        }
    }
}

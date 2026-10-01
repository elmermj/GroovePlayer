package com.aethelsoft.grooveplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.aethelsoft.grooveplayer.MainActivity
import com.aethelsoft.grooveplayer.R
import com.aethelsoft.grooveplayer.domain.artwork.EmbeddedArtworkCache
import com.aethelsoft.grooveplayer.domain.artwork.EmbeddedArtworkKeys
import com.aethelsoft.grooveplayer.services.MusicPlaybackService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pushes the current player snapshot to every home-screen playback widget.
 * Playback actions go to [MusicPlaybackService]. Artwork is a bitmap already
 * in memory, a local URI, or a file already in the artwork cache.
 */
object PlaybackWidgetPublisher {
    private const val TAG = "PlaybackWidget"
    private const val ARTWORK_EDGE = 128
    private const val MAX_ARTWORK_BYTES = 2 * 1024 * 1024
    private const val REQUEST_OPEN = 1
    private const val REQUEST_PREVIOUS = 2
    private const val REQUEST_PLAY_PAUSE = 3
    private const val REQUEST_NEXT = 4

    private val artworkEpoch = AtomicInteger(0)
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
            Log.w(TAG, "Widget artwork load failed", error)
        },
    )

    @Volatile
    private var latest: PlaybackWidgetState = PlaybackWidgetState.EMPTY

    @Volatile
    private var currentBitmap: Bitmap? = null

    fun publish(context: Context, state: PlaybackWidgetState) {
        val app = context.applicationContext
        val keepArt = state.hasTrack &&
            state.trackId != null &&
            state.trackId == latest.trackId &&
            state.artworkUrl == latest.artworkUrl &&
            currentBitmap != null
        val epoch = if (keepArt) artworkEpoch.get() else {
            currentBitmap = null
            artworkEpoch.incrementAndGet()
        }
        latest = state
        try {
            if (!hasWidgets(app)) return
            render(app, state, if (keepArt) currentBitmap else null)
        } catch (error: Exception) {
            Log.w(TAG, "Widget update failed", error)
            return
        }
        if (keepArt || !state.hasTrack) return
        scope.launch {
            val bitmap = decode(app, state) ?: return@launch
            withContext(Dispatchers.Main.immediate) {
                if (epoch != artworkEpoch.get() || latest.trackId != state.trackId) {
                    bitmap.recycle()
                    return@withContext
                }
                currentBitmap = bitmap
                try {
                    if (hasWidgets(app)) render(app, latest, bitmap)
                } catch (error: Exception) {
                    Log.w(TAG, "Widget artwork update failed", error)
                }
            }
        }
    }

    fun refresh(context: Context) {
        publish(context, latest)
    }

    /**
     * Artwork the playback service already decoded for the notification.
     * Ignored when it belongs to a track that is no longer current.
     */
    fun offerArtwork(context: Context, trackId: String, bitmap: Bitmap) {
        if (latest.trackId != trackId) return
        val app = context.applicationContext
        val widgets = try {
            hasWidgets(app)
        } catch (error: Exception) {
            Log.w(TAG, "Widget artwork update failed", error)
            return
        }
        if (!widgets) return
        val epoch = artworkEpoch.incrementAndGet()
        scope.launch {
            val scaled = scaleDown(bitmap, ARTWORK_EDGE)
            withContext(Dispatchers.Main.immediate) {
                if (scaled == null) return@withContext
                if (epoch != artworkEpoch.get() || latest.trackId != trackId) {
                    if (scaled !== bitmap) scaled.recycle()
                    return@withContext
                }
                currentBitmap = scaled
                try {
                    if (hasWidgets(app)) render(app, latest, scaled)
                } catch (error: Exception) {
                    Log.w(TAG, "Widget artwork update failed", error)
                }
            }
        }
    }

    private fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, PlaybackWidgetProvider::class.java)
        return manager.getAppWidgetIds(component).isNotEmpty()
    }

    private fun render(context: Context, state: PlaybackWidgetState, artwork: Bitmap?) {
        val views = RemoteViews(context.packageName, R.layout.widget_playback)
        val open = openAppIntent(context)
        views.setOnClickPendingIntent(R.id.widget_root, open)
        views.setOnClickPendingIntent(R.id.widget_artwork, open)
        views.setOnClickPendingIntent(R.id.widget_text, open)
        views.setOnClickPendingIntent(R.id.widget_title, open)
        views.setOnClickPendingIntent(R.id.widget_artist, open)

        if (state.hasTrack) {
            views.setTextViewText(R.id.widget_title, state.title)
            if (state.artist.isNotEmpty()) {
                views.setViewVisibility(R.id.widget_artist, View.VISIBLE)
                views.setTextViewText(R.id.widget_artist, state.artist)
            } else {
                views.setViewVisibility(R.id.widget_artist, View.GONE)
            }
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            )
            views.setContentDescription(R.id.widget_previous, context.getString(R.string.widget_previous))
            views.setContentDescription(
                R.id.widget_play_pause,
                context.getString(if (state.isPlaying) R.string.widget_pause else R.string.widget_play),
            )
            views.setContentDescription(R.id.widget_next, context.getString(R.string.widget_next))
            views.setOnClickPendingIntent(
                R.id.widget_previous,
                serviceIntent(context, MusicPlaybackService.ACTION_PREVIOUS, REQUEST_PREVIOUS),
            )
            views.setOnClickPendingIntent(
                R.id.widget_play_pause,
                serviceIntent(context, MusicPlaybackService.ACTION_PLAY_PAUSE, REQUEST_PLAY_PAUSE),
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                serviceIntent(context, MusicPlaybackService.ACTION_NEXT, REQUEST_NEXT),
            )
        } else {
            views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_nothing_playing))
            views.setViewVisibility(R.id.widget_artist, View.GONE)
            views.setImageViewResource(R.id.widget_play_pause, R.drawable.ic_play)
            val openLabel = context.getString(R.string.widget_open_app)
            views.setContentDescription(R.id.widget_previous, openLabel)
            views.setContentDescription(R.id.widget_play_pause, openLabel)
            views.setContentDescription(R.id.widget_next, openLabel)
            views.setOnClickPendingIntent(R.id.widget_previous, open)
            views.setOnClickPendingIntent(R.id.widget_play_pause, open)
            views.setOnClickPendingIntent(R.id.widget_next, open)
        }

        views.setContentDescription(R.id.widget_artwork, context.getString(R.string.widget_artwork))
        if (artwork != null) {
            views.setImageViewBitmap(R.id.widget_artwork, artwork)
        } else {
            views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_widget_note)
        }

        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, PlaybackWidgetProvider::class.java)
        manager.updateAppWidget(component, views)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun serviceIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MusicPlaybackService::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, requestCode, intent, flags)
        } else {
            PendingIntent.getService(context, requestCode, intent, flags)
        }
    }

    private fun decode(context: Context, state: PlaybackWidgetState): Bitmap? {
        if (!state.hasTrack) return null
        return try {
            when (val source = widgetArtworkSource(artworkCacheDir(context), state.artworkUrl)) {
                WidgetArtworkSource.None -> null
                is WidgetArtworkSource.LocalUri -> decodeUri(context, source.value)
                is WidgetArtworkSource.CacheFile -> decodeFile(source.file)
            }
        } catch (error: Exception) {
            Log.w(TAG, "Local artwork decode failed", error)
            null
        }
    }

    private fun artworkCacheDir(context: Context): File =
        File(context.cacheDir, EmbeddedArtworkCache.DIRECTORY)

    private fun decodeUri(context: Context, value: String): Bitmap? {
        return context.contentResolver.openInputStream(Uri.parse(value))?.use { stream ->
            decodeBytes(stream.readBytes())
        }
    }

    private fun decodeFile(file: File): Bitmap? {
        if (!file.isFile || file.length() <= 0L || file.length() > MAX_ARTWORK_BYTES) return null
        return decodeBytes(file.readBytes())
    }

    private fun decodeBytes(bytes: ByteArray): Bitmap? {
        if (bytes.isEmpty() || bytes.size > MAX_ARTWORK_BYTES) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = EmbeddedArtworkKeys.sampleSize(
                bounds.outWidth,
                bounds.outHeight,
                ARTWORK_EDGE,
            )
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val scaled = scaleDown(decoded, ARTWORK_EDGE) ?: return null
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private fun scaleDown(bitmap: Bitmap, edge: Int): Bitmap? {
        return try {
            if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
            val longest = maxOf(bitmap.width, bitmap.height)
            val source = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return null
            } else {
                bitmap
            }
            if (longest <= edge) return source
            val factor = edge.toFloat() / longest.toFloat()
            val width = (source.width * factor).toInt().coerceAtLeast(1)
            val height = (source.height * factor).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(source, width, height, true)
            if (source !== bitmap && scaled !== source) source.recycle()
            scaled
        } catch (error: Exception) {
            Log.w(TAG, "Artwork scale failed", error)
            null
        }
    }
}

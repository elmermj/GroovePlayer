package com.aethelsoft.grooveplayer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import coil3.toBitmap
import com.aethelsoft.grooveplayer.MainActivity
import com.aethelsoft.grooveplayer.R
import com.aethelsoft.grooveplayer.data.player.ExoPlayerManager
import com.aethelsoft.grooveplayer.domain.model.Song
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Enhanced Music Playback Service with rich notifications.
 * Features:
 * - Artwork background with black fallback
 * - Collapsible notification with custom layouts
 * - Seek slider (automatic on Android 13+, custom for older versions)
 * - Player controls (play/pause, next, previous)
 * - Compatible with Android 7.0+ (API 24+)
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MusicPlaybackService : Service() {

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManager

    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSession: MediaSession
    private lateinit var imageLoader: ImageLoader
    private lateinit var dummyPlayer: Player

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentSong: Song? = null
    private var isPlaying = false
    private var currentPosition = 0L
    private var duration = 0L
    private var artworkBitmap: Bitmap? = null
    private var dominantColor: Int = Color.BLACK
    private var lastNotificationUpdate = 0L
    private val NOTIFICATION_UPDATE_INTERVAL = 1000L // Update at most once per second

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        android.util.Log.d(TAG, "MusicPlaybackService onCreate()")

        createNotificationChannel()

        mediaSession = MediaSession.Builder(this, exoPlayerManager.livePlayer).build()

        // Initialize image loader
        imageLoader = ImageLoader.Builder(this)
            .crossfade(true)
            .build()

        // Start as foreground service immediately
        val initialNotification = buildEmptyNotification()
        startForeground(NOTIFICATION_ID, initialNotification)
        android.util.Log.d(TAG, "Started foreground service with notification")

        // Observe player state
        observePlayerState()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observePlayerState() {
        serviceScope.launch {
            combine(
                exoPlayerManager.observeCurrentSong(),
                exoPlayerManager.observeIsPlaying(),
                exoPlayerManager.observePosition(),
                exoPlayerManager.observeDuration()
            ) { song, playing, position, dur ->
                PlayerState(song, playing, position, dur)
            }.collect { state ->
                val songChanged = state.song?.id != currentSong?.id
                val playingStateChanged = state.isPlaying != isPlaying
                val now = System.currentTimeMillis()
                val shouldUpdate = songChanged || playingStateChanged || (now - lastNotificationUpdate) > NOTIFICATION_UPDATE_INTERVAL

                currentSong = state.song
                isPlaying = state.isPlaying
                currentPosition = state.position
                duration = state.duration

                // Load artwork if song changed
                if (state.song != null && songChanged) {
                    loadArtwork(state.song)
                }

                // Update notification only when necessary
                if (shouldUpdate) {
                    lastNotificationUpdate = now
                    updateNotification()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun loadArtwork(song: Song) {
        withContext(Dispatchers.IO) {
            try {
                val deviceWidth = resources.displayMetrics.widthPixels
                val artworkUrl = song.artworkUrl
                if (!artworkUrl.isNullOrEmpty()) {
                    val request = ImageRequest.Builder(this@MusicPlaybackService)
                        .data(artworkUrl)
                        .size(Size(deviceWidth, deviceWidth))
                        .build()

                    val result = imageLoader.execute(request)
                    val drawable = result.image

                    if (drawable != null) {
                        var bitmap = (drawable as? BitmapDrawable)?.bitmap
                            ?: drawable.toBitmap(deviceWidth, deviceWidth)

                        // Convert HARDWARE bitmaps to mutable format for color extraction and notification display
                        // HARDWARE bitmaps cannot be accessed directly and cause crashes
                        if (bitmap.config == Bitmap.Config.HARDWARE || !bitmap.isMutable) {
                            try {
                                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                            } catch (e: Exception) {
                                android.util.Log.e(TAG, "Failed to convert bitmap from HARDWARE format", e)
                                // Fallback: create new bitmap from drawable with explicit config
                                bitmap = drawable.toBitmap(deviceWidth, deviceWidth)
                                if (bitmap.config == Bitmap.Config.HARDWARE) {
                                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                }
                            }
                        }
                        
                        artworkBitmap = bitmap
                        
                        // Extract dominant color for background
                        artworkBitmap?.let { bmp ->
                            extractDominantColor(bmp)
                        }
                    } else {
                        setDefaultArtwork()
                    }
                } else {
                    setDefaultArtwork()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to load artwork", e)
                setDefaultArtwork()
            }
        }
    }

    private fun setDefaultArtwork() {
        // Create a simple default artwork with music icon
        artworkBitmap = createDefaultArtworkBitmap()
        dominantColor = Color.BLACK
    }

    private fun createDefaultArtworkBitmap(): Bitmap {
        val size = resources.displayMetrics.widthPixels
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        
        // Dark gradient background
        val paint = Paint().apply {
            color = "#1a1a1a".toColorInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        return bitmap
    }

    private fun extractDominantColor(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            dominantColor = palette?.getDarkMutedColor(Color.BLACK) 
                ?: palette?.getDominantColor(Color.BLACK)
                ?: Color.BLACK
            
            // Update notification with new color
            updateNotification()
        }
    }

    private fun updateNotification() {
        try {
            val notification = buildNotification()
            val hasSong = currentSong != null
            android.util.Log.d(TAG, "Updating notification: hasSong=$hasSong, isPlaying=$isPlaying, title=${currentSong?.title}")
            
            // Always update foreground notification for foreground services
            startForeground(NOTIFICATION_ID, notification)
            android.util.Log.d(TAG, "Foreground notification started")
            
            // Also notify for visibility (in case foreground fails silently)
            notificationManager.notify(NOTIFICATION_ID, notification)
            android.util.Log.d(TAG, "NotificationManager.notify() called")
            
            // Check if notifications are enabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
                android.util.Log.w(TAG, "⚠️ NOTIFICATIONS ARE DISABLED! Please enable in Settings → Apps → GroovePlayer → Notifications")
                
                if (!areNotificationsEnabled) {
                    // Log a warning but continue - foreground service notifications might still work
                    android.util.Log.w(TAG, "Notifications disabled, but continuing with foreground service notification")
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
                    android.util.Log.d(TAG, "Notification channel: importance=${channel?.importance}, enabled=${channel?.importance != NotificationManager.IMPORTANCE_NONE}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to update notification", e)
            e.printStackTrace()
        }
    }

    private fun buildNotification(): Notification {
        val song = currentSong ?: return buildEmptyNotification()
        
        // Create intent to open app when notification is clicked


        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create actions
        val previousIntent = createActionIntent(ACTION_PREVIOUS)
        val playPauseIntent = createActionIntent(ACTION_PLAY_PAUSE)
        val nextIntent = createActionIntent(ACTION_NEXT)
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_transparent)
            .setLargeIcon(artworkBitmap)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText(song.album ?: "")
            .setContentIntent(contentIntent)
            .setDeleteIntent(createActionIntent(ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOnlyAlertOnce(true) // Only make sound/vibrate once
            .setAutoCancel(false) // Don't auto-cancel media notifications
            
        // Add actions
        builder.addAction(
            R.drawable.ic_skip_previous,
            "Previous",
            previousIntent
        )
        builder.addAction(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            if (isPlaying) "Pause" else "Play",
            playPauseIntent
        )
        builder.addAction(
            R.drawable.ic_skip_next,
            "Next",
            nextIntent
        )

        // Use Media Style - simplified without requiring MediaSession token
        // The notification works with or without the session token for our use case
        builder.setStyle(
            MediaStyleNotificationHelper.MediaStyle(mediaSession)
        )
        
        // Use custom layouts - but only if notifications are enabled
        // Custom RemoteViews may not work if notifications are disabled
        val areNotificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
        
        if (areNotificationsEnabled) {
            val collapsedView = createCollapsedView(song)
            val expandedView = createExpandedView(song)
            
            builder.setCustomContentView(collapsedView)
            builder.setCustomBigContentView(expandedView)
        }
        // If notifications are disabled, use default notification layout
        
        return builder.build()
    }

    private fun createCollapsedView(song: Song): RemoteViews {
        val view = RemoteViews(packageName, R.layout.notification_collapsed)
        
        view.setTextViewText(R.id.notification_title, song.title)
        view.setTextViewText(R.id.notification_artist, song.artist)
        
        // Set artwork as background
        if (artworkBitmap != null) {
            view.setImageViewBitmap(R.id.notification_artwork_background, artworkBitmap)
        } else {
            // Fallback to black background if no artwork
            view.setInt(R.id.notification_artwork_background, "setBackgroundColor", Color.BLACK)
        }
        
        // Set play/pause button
        view.setImageViewResource(
            R.id.notification_play_pause,
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        
        // Set click listeners
        view.setOnClickPendingIntent(R.id.notification_play_pause, createActionIntent(ACTION_PLAY_PAUSE))
        view.setOnClickPendingIntent(R.id.notification_next, createActionIntent(ACTION_NEXT))
        
        return view
    }

    private fun createExpandedView(song: Song): RemoteViews {
        val view = RemoteViews(packageName, R.layout.notification_expanded)
        
        view.setTextViewText(R.id.notification_title, song.title)
        view.setTextViewText(R.id.notification_artist, song.artist)
        view.setTextViewText(R.id.notification_album, song.album ?: "")
        
        // Set artwork as background
        if (artworkBitmap != null) {
            view.setImageViewBitmap(R.id.notification_artwork_background, artworkBitmap)
        } else {
            // Fallback to black background if no artwork
            view.setInt(R.id.notification_artwork_background, "setBackgroundColor", Color.BLACK)
        }
        
        // Set play/pause button
        view.setImageViewResource(
            R.id.notification_play_pause,
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        
        // Set progress bar
        if (duration > 0) {
            val progress = ((currentPosition.toFloat() / duration.toFloat()) * 100).toInt()
            view.setProgressBar(R.id.notification_progress, 100, progress, false)
            
            // Set time text
            view.setTextViewText(R.id.notification_current_time, formatTime(currentPosition))
            view.setTextViewText(R.id.notification_total_time, formatTime(duration))
        }
        
        // Set click listeners
        view.setOnClickPendingIntent(R.id.notification_previous, createActionIntent(ACTION_PREVIOUS))
        view.setOnClickPendingIntent(R.id.notification_play_pause, createActionIntent(ACTION_PLAY_PAUSE))
        view.setOnClickPendingIntent(R.id.notification_next, createActionIntent(ACTION_NEXT))
        
        return view
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = ms / (1000 * 60 * 60)
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    private fun buildEmptyNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("GroovePlayer")
            .setContentText("Preparing playback...")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .build()
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d(TAG, "onStartCommand: action=${intent?.action}")
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                serviceScope.launch {
                    if (isPlaying) {
                        exoPlayerManager.pause()
                    } else {
                        exoPlayerManager.play()
                    }
                }
            }
            ACTION_NEXT -> {
                serviceScope.launch {
                    exoPlayerManager.next()
                }
            }
            ACTION_PREVIOUS -> {
                serviceScope.launch {
                    exoPlayerManager.previous()
                }
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                // Service is already foreground, just update notification
                updateNotification()
            }
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_DEFAULT // DEFAULT ensures notification is visible
            ).apply {
                description = "Shows currently playing music"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(false)
                enableLights(false)
                setSound(null, null) // No sound for music notifications
            }
            notificationManager.createNotificationChannel(channel)
            
            // Force channel importance on Android 8.0+ (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val createdChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (createdChannel != null && createdChannel.importance < NotificationManager.IMPORTANCE_DEFAULT) {
                    // Channel exists but has low importance, recreate it
                    notificationManager.deleteNotificationChannel(CHANNEL_ID)
                    notificationManager.createNotificationChannel(channel)
                }
            }
        }
    }

    private data class PlayerState(
        val song: Song?,
        val isPlaying: Boolean,
        val position: Long,
        val duration: Long
    )

    companion object {
        private const val TAG = "MusicPlaybackService"
        private const val CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_PLAY_PAUSE = "com.aethelsoft.grooveplayer.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.aethelsoft.grooveplayer.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.aethelsoft.grooveplayer.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.aethelsoft.grooveplayer.ACTION_STOP"
    }
}

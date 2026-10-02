package com.aethelworks.grooveplayer.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.aethelworks.grooveplayer.MainActivity
import com.aethelworks.grooveplayer.R
import com.aethelworks.grooveplayer.data.auto.AndroidAutoCatalog
import com.aethelworks.grooveplayer.data.player.ExoPlayerManager
import com.aethelworks.grooveplayer.domain.auto.AndroidAutoBrowse
import com.aethelworks.grooveplayer.domain.auto.AutoBrowseItem
import com.aethelworks.grooveplayer.domain.auto.AutoPlayRequest
import com.aethelworks.grooveplayer.domain.repository.PlayerRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Android Auto browse and playback.
 *
 * The tree comes from [AndroidAutoCatalog] (private library, playlists, history, likes).
 * Play, add, and resume call [PlayerRepository], so a missing local file follows the
 * same local-first / Premium stream rule as the phone. This service does not scan MediaStore.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class GrooveMediaLibraryService : MediaLibraryService() {

    @Inject
    lateinit var catalog: AndroidAutoCatalog

    @Inject
    lateinit var playerRepository: PlayerRepository

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var librarySession: MediaLibrarySession? = null
    private var inForeground = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val openApp = PendingIntent.getActivity(
            this,
            REQUEST_OPEN_APP,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        librarySession = MediaLibrarySession.Builder(this, LibrarySessionPlayer(exoPlayerManager.livePlayer), Callback())
            .setId(SESSION_ID)
            .setSessionActivity(openApp)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = librarySession

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        // MusicPlaybackService owns the rich playback notification. This service only
        // stays in the foreground when Android requires it for the Auto session.
        if (!startInForegroundRequired) {
            if (inForeground) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                inForeground = false
            }
            return
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_transparent)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.android_auto_playback))
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        inForeground = true
    }

    override fun onDestroy() {
        scope.cancel()
        librarySession?.release()
        librarySession = null
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.android_auto_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setShowBadge(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun <T> future(block: suspend () -> T): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        scope.launch {
            try {
                future.set(block())
            } catch (cancelled: CancellationException) {
                future.setException(cancelled)
            } catch (error: Exception) {
                future.setException(error)
            }
        }
        return future
    }

    private suspend fun play(request: AutoPlayRequest?) {
        if (request == null || request.songs.isEmpty()) return
        playerRepository.setQueue(
            songs = request.songs,
            startIndex = request.startIndex,
            isEndlessQueue = false,
            autoPlay = true,
        )
    }

    private fun AutoBrowseItem.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(browsable)
            .setIsPlayable(playable)
            .setMediaType(mediaType())
        subtitle?.let { metadata.setSubtitle(it) }
        artist?.let { metadata.setArtist(it) }
        album?.let { metadata.setAlbumTitle(it) }
        if (durationMs > 0L) metadata.setDurationMs(durationMs)
        artworkUrl?.takeIf { it.isNotBlank() }?.let { metadata.setArtworkUri(Uri.parse(it)) }
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(metadata.build())
            .build()
    }

    private fun AutoBrowseItem.mediaType(): Int {
        return when {
            !browsable && playable -> MediaMetadata.MEDIA_TYPE_MUSIC
            id == "playlists" -> MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS
            id == "artists" || id == "favorites/artists" -> MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS
            id == "albums" || id == "favorites/albums" -> MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS
            id.startsWith("playlist/") -> MediaMetadata.MEDIA_TYPE_PLAYLIST
            id.startsWith("artist/") || id.startsWith("favorites/artist/") -> MediaMetadata.MEDIA_TYPE_ARTIST
            id.startsWith("album/") || id.startsWith("favorites/album/") -> MediaMetadata.MEDIA_TYPE_ALBUM
            else -> MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
        }
    }

    private inner class Callback : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return future {
                try {
                    LibraryResult.ofItem(
                        AndroidAutoBrowse.item(AndroidAutoBrowse.ROOT, catalog.snapshot())!!.toMediaItem(),
                        params,
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_IO, params)
                }
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return future {
                try {
                    val item = AndroidAutoBrowse.item(mediaId, catalog.snapshot())
                    if (item == null) {
                        LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                    } else {
                        LibraryResult.ofItem(item.toMediaItem(), null)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_IO)
                }
            }
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return future {
                try {
                    val children = AndroidAutoBrowse.children(parentId, catalog.snapshot())
                    if (children == null) {
                        LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE, params)
                    } else {
                        val items = AndroidAutoBrowse.page(children, page, pageSize).map { it.toMediaItem() }
                        LibraryResult.ofItemList(items, params)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_IO, params)
                }
            }
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            return future {
                try {
                    val count = AndroidAutoBrowse.search(query, catalog.snapshot()).size
                    librarySession?.notifySearchResultChanged(browser, query, count, params)
                    LibraryResult.ofVoid(params)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_IO, params)
                }
            }
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return future {
                try {
                    val items = AndroidAutoBrowse.page(
                        AndroidAutoBrowse.search(query, catalog.snapshot()),
                        page,
                        pageSize,
                    ).map { it.toMediaItem() }
                    LibraryResult.ofItemList(items, params)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_IO, params)
                }
            }
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaItemsWithStartPosition> {
            return future {
                val snapshot = catalog.snapshot()
                val request = when {
                    mediaItems.size == 1 -> AndroidAutoBrowse.playRequest(mediaItems[0].mediaId, snapshot)
                    mediaItems.size > 1 -> {
                        val songs = mediaItems.mapNotNull { item ->
                            AndroidAutoBrowse.playRequest(item.mediaId, snapshot)?.let { request ->
                                request.songs.getOrNull(request.startIndex)
                            }
                        }
                        val start = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
                        if (songs.isEmpty()) null else AutoPlayRequest(songs, start)
                    }
                    else -> null
                }
                play(request)
                MediaItemsWithStartPosition(emptyList(), C.INDEX_UNSET, C.TIME_UNSET)
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            return future {
                val snapshot = catalog.snapshot()
                for (item in mediaItems) {
                    val request = AndroidAutoBrowse.playRequest(item.mediaId, snapshot) ?: continue
                    val song = request.songs.getOrNull(request.startIndex) ?: continue
                    playerRepository.playNext(song)
                }
                emptyList()
            }
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaItemsWithStartPosition> {
            return future {
                val snapshot = catalog.snapshot()
                val request = AndroidAutoBrowse.resumeRequest(snapshot)
                    ?: throw UnsupportedOperationException("Nothing to resume")
                if (isForPlayback) {
                    play(request)
                    MediaItemsWithStartPosition(emptyList(), C.INDEX_UNSET, C.TIME_UNSET)
                } else {
                    val song = request.songs[request.startIndex]
                    val item = MediaItem.Builder()
                        .setMediaId(song.id)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(song.title)
                                .setArtist(song.artist)
                                .setIsBrowsable(false)
                                .setIsPlayable(true)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                .build(),
                        )
                        .build()
                    MediaItemsWithStartPosition(listOf(item), 0, C.TIME_UNSET)
                }
            }
        }
    }

    private companion object {
        const val SESSION_ID = "groove-auto"
        const val CHANNEL_ID = "android_auto_playback"
        const val NOTIFICATION_ID = 1002
        const val REQUEST_OPEN_APP = 11
    }
}

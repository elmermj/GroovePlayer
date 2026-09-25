package com.aethelsoft.grooveplayer.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aethelsoft.grooveplayer.domain.auth.ColdStartPresentation
import com.aethelsoft.grooveplayer.presentation.home.HomeScreen
import com.aethelsoft.grooveplayer.presentation.library.albums.AlbumDetailScreen
import com.aethelsoft.grooveplayer.presentation.library.artists.ArtistDetailScreen
import com.aethelsoft.grooveplayer.presentation.library.favorites.FavoriteAlbumsScreen
import com.aethelsoft.grooveplayer.presentation.library.favorites.FavoriteArtistsScreen
import com.aethelsoft.grooveplayer.presentation.library.favorites.FavoriteTracksScreen
import com.aethelsoft.grooveplayer.presentation.library.genres.GenreTracksScreen
import com.aethelsoft.grooveplayer.presentation.library.genres.GenresScreen
import com.aethelsoft.grooveplayer.presentation.library.playlists.AddPlaylistTracksScreen
import com.aethelsoft.grooveplayer.presentation.library.playlists.PlaylistDetailScreen
import com.aethelsoft.grooveplayer.presentation.library.playlists.PlaylistsScreen
import com.aethelsoft.grooveplayer.presentation.library.mostplayed.MostPlayedScreen
import com.aethelsoft.grooveplayer.presentation.library.recentlyplayed.RecentlyPlayedScreen
import com.aethelsoft.grooveplayer.presentation.library.songs.SongsScreen
import com.aethelsoft.grooveplayer.presentation.player.FullPlayerScreen
import com.aethelsoft.grooveplayer.presentation.profile.ProfileScreen
import com.aethelsoft.grooveplayer.presentation.ui_customisation.UiCustomisationScreen
import com.aethelsoft.grooveplayer.presentation.backup.BackupScreen
import com.aethelsoft.grooveplayer.presentation.backup.RestoreApplyScreen
import com.aethelsoft.grooveplayer.presentation.search.SearchScreen
import com.aethelsoft.grooveplayer.presentation.share.ReceiveApprovalScreen
import com.aethelsoft.grooveplayer.presentation.transfer.DeviceDiscoveryScreen
import com.aethelsoft.grooveplayer.presentation.transfer.TransferProgressScreen
import com.aethelsoft.grooveplayer.presentation.transfer.TransferStatusScreen
import com.aethelsoft.grooveplayer.presentation.share.ShareConfirmationScreen
import com.aethelsoft.grooveplayer.presentation.share.ShareOptionsScreen
import com.aethelsoft.grooveplayer.presentation.share.ShareViaNfcScreen
import com.aethelsoft.grooveplayer.presentation.share.ShareViaNearbyScreen

/**
 * Main navigation host for the app.
 * PlayerViewModel is accessed via CompositionLocal in each screen instead of parameter passing.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = AppRoutes.HOME,
    onBackupRestoreVisible: (Boolean) -> Unit = {},
    onManualRestoreOpened: () -> Unit = {},
) {
    // The first entrance used to slide the start destination in from the right,
    // on top of a black splash. If that transition never finished, the window
    // stayed black while taps still hit the activity. Later navigations still slide.
    val coldStartEntrance = remember { java.util.concurrent.atomic.AtomicBoolean(true) }
    SideEffect { coldStartEntrance.set(false) }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            if (ColdStartPresentation.playEntrance(coldStartEntrance.get())) {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            } else {
                EnterTransition.None
            }
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(durationMillis = 300)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(durationMillis = 300)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 300)
            )
        }
    ) {
        composable(
            route = AppRoutes.HOME,
            enterTransition = {
                // Cold start must appear in place. A later visit still slides in from the right.
                if (ColdStartPresentation.playEntrance(coldStartEntrance.get())) {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    )
                } else {
                    EnterTransition.None
                }
            },
            exitTransition = {
                // Slide out to left
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popEnterTransition = {
                // Slide in from left when going back
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popExitTransition = {
                // Slide out to right when going back (iOS style)
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            }
        ) {
            HomeScreen(
                onNavigateToSongs = { navController.navigate(AppRoutes.SONGS) },
                onNavigateToGenres = { navController.navigate(AppRoutes.GENRES) },
                onNavigateToRecentlyPlayed = { navController.navigate(AppRoutes.RECENTLY_PLAYED) },
                onNavigateToMostPlayed = { navController.navigate(AppRoutes.MOST_PLAYED) },
                onNavigateToFavoriteTracks = { navController.navigate(AppRoutes.FAVORITE_TRACKS) },
                onNavigateToFavoriteArtists = { navController.navigate(AppRoutes.FAVORITE_ARTISTS) },
                onNavigateToFavoriteAlbums = { navController.navigate(AppRoutes.FAVORITE_ALBUMS) },
                onNavigateToPlaylists = { navController.navigate(AppRoutes.PLAYLISTS) },
                onNavigateToSearch = { query ->
                    navController.navigate(AppRoutes.searchRoute(query))
                },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(AppRoutes.albumDetailRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(AppRoutes.artistDetailRoute(artistId))
                },
            )
        }
        composable(
            route = AppRoutes.SONGS,
            enterTransition = {
                // Slide in from right (iOS style)
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            exitTransition = {
                // Slide out to left
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popEnterTransition = {
                // Slide in from left when going back
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popExitTransition = {
                // Slide out to right when going back (iOS style)
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            }
        ) {
            SongsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = AppRoutes.RECENTLY_PLAYED,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            }
        ) {
            RecentlyPlayedScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = AppRoutes.MOST_PLAYED) {
            MostPlayedScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = AppRoutes.PLAYLISTS) {
            PlaylistsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenPlaylist = { playlistId ->
                    navController.navigate(AppRoutes.playlistDetailRoute(playlistId))
                },
            )
        }
        composable(
            route = AppRoutes.PLAYLIST_DETAIL,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType },
            ),
        ) { entry ->
            val playlistId = entry.arguments?.getLong("playlistId") ?: 0L
            PlaylistDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddTracks = {
                    navController.navigate(AppRoutes.playlistAddTracksRoute(playlistId))
                },
            )
        }
        composable(
            route = AppRoutes.PLAYLIST_ADD_TRACKS,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType },
            ),
        ) {
            AddPlaylistTracksScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(route = AppRoutes.GENRES) {
            GenresScreen(
                onNavigateBack = { navController.popBackStack() },
                onGenreClick = { genreName ->
                    navController.navigate(AppRoutes.genreTracksRoute(genreName))
                },
            )
        }
        composable(
            route = AppRoutes.GENRE_TRACKS,
            arguments = listOf(
                navArgument("genreName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            ),
        ) {
            GenreTracksScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = AppRoutes.FAVORITE_TRACKS,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            }
        ) {
            FavoriteTracksScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = AppRoutes.FAVORITE_ARTISTS,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            }
        ) {
            FavoriteArtistsScreen(
                onNavigateBack = { navController.popBackStack() },
                onArtistClick = { artistId ->
                    navController.navigate(AppRoutes.artistDetailRoute(artistId))
                }
            )
        }
        composable(
            route = AppRoutes.FAVORITE_ALBUMS,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            }
        ) {
            FavoriteAlbumsScreen(
                onNavigateBack = { navController.popBackStack() },
                onAlbumClick = { albumId ->
                    navController.navigate(AppRoutes.albumDetailRoute(albumId))
                }
            )
        }
        composable(
            route = AppRoutes.ALBUM_DETAIL,
            arguments = listOf(
                navArgument("albumId") { type = NavType.StringType }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            }
        ) { backStackEntry ->
            val rawAlbumId = backStackEntry.arguments?.getString("albumId") ?: ""
            val albumId = android.net.Uri.decode(rawAlbumId)
            AlbumDetailScreen(
                albumId = albumId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = AppRoutes.ARTIST_DETAIL,
            arguments = listOf(
                navArgument("artistId") { type = NavType.StringType }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            }
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
            ArtistDetailScreen(
                artistId = artistId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = AppRoutes.SEARCH,
            arguments = listOf(
                navArgument("query") { type = NavType.StringType; defaultValue = "" }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            }
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            SearchScreen(
                query = query,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(AppRoutes.albumDetailRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(AppRoutes.artistDetailRoute(artistId))
                },
            )
        }
        composable(
            route = AppRoutes.FULL_PLAYER,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(durationMillis = 250, delayMillis = 0)
                ) + slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 350, delayMillis = 0)
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(durationMillis = 250, delayMillis = 150)
                ) + slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 350)
                )
            },
            popEnterTransition = {
                fadeIn(
                    animationSpec = tween(durationMillis = 250, delayMillis = 0)
                ) + slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 350, delayMillis = 0)
                )
            },
            popExitTransition = {
                fadeOut(
                    animationSpec = tween(durationMillis = 250, delayMillis = 150)
                ) + slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 350)
                )
            }
        ) {
            FullPlayerScreen(
                onClose = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = AppRoutes.PROFILE,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            }
        ) {
            ProfileScreen(
                onNavigateToSearch = { query ->
                    navController.navigate(AppRoutes.searchRoute(query))
                },
                onNavigateToShare = {
                    navController.navigate(AppRoutes.SHARE_OPTIONS)
                },
                onNavigateToUiCustomisation = {
                    navController.navigate(AppRoutes.UI_CUSTOMISATION)
                },
                onNavigateToBackup = {
                    navController.navigate(AppRoutes.BACKUP)
                },
            )
        }
        composable(route = AppRoutes.UI_CUSTOMISATION) {
            UiCustomisationScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(route = AppRoutes.BACKUP) {
            DisposableEffect(Unit) {
                onBackupRestoreVisible(true)
                onDispose { onBackupRestoreVisible(false) }
            }
            BackupScreen(
                onNavigateBack = { navController.popBackStack() },
                onRestoreLibrary = {
                    onManualRestoreOpened()
                    navController.navigate(AppRoutes.restoreApplyRoute(startDownload = true))
                },
            )
        }
        composable(
            route = AppRoutes.RESTORE_APPLY,
            arguments = listOf(
                navArgument("start") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            DisposableEffect(Unit) {
                onBackupRestoreVisible(true)
                onDispose { onBackupRestoreVisible(false) }
            }
            val startDownload = entry.arguments?.getBoolean("start") ?: false
            RestoreApplyScreen(
                startDownload = startDownload,
                onFinished = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(route = AppRoutes.SHARE_OPTIONS) {
            ShareOptionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onShareViaNfc = { navController.navigate(AppRoutes.shareConfirmationRoute("nfc")) },
                onShareViaNearby = { navController.navigate(AppRoutes.shareConfirmationRoute("nearby")) },
                onShareViaNearbyP2P = { navController.navigate(AppRoutes.nearbyDiscoveryRoute(isSender = true)) },
                onReceiveViaNfc = { navController.navigate(AppRoutes.SHARE_VIA_NFC) },
                onReceiveViaNearby = { navController.navigate(AppRoutes.SHARE_VIA_NEARBY) },
                onReceiveViaNearbyP2P = { navController.navigate(AppRoutes.nearbyDiscoveryRoute(isSender = false)) },
                onNavigateToTransferStatus = { navController.navigate(AppRoutes.TRANSFER_STATUS) }
            )
        }
        composable(
            route = AppRoutes.SHARE_CONFIRMATION,
            arguments = listOf(navArgument("shareMethod") { type = NavType.StringType })
        ) { backStackEntry ->
            val shareMethod = backStackEntry.arguments?.getString("shareMethod") ?: "nfc"
            ShareConfirmationScreen(
                shareMethod = shareMethod,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = AppRoutes.SHARE_VIA_NFC) {
            ShareViaNfcScreen(
                onNavigateBack = { navController.popBackStack() },
                onOfferReceived = {
                    navController.navigate(AppRoutes.RECEIVE_APPROVAL) {
                        popUpTo(AppRoutes.SHARE_VIA_NFC) { inclusive = true }
                    }
                }
            )
        }
        composable(route = AppRoutes.SHARE_VIA_NEARBY) {
            ShareViaNearbyScreen(
                onNavigateBack = { navController.popBackStack() },
                onOfferReceived = {
                    navController.navigate(AppRoutes.RECEIVE_APPROVAL) {
                        popUpTo(AppRoutes.SHARE_VIA_NEARBY) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "${AppRoutes.NEARBY_DISCOVERY}?isSender={isSender}",
            arguments = listOf(navArgument("isSender") { type = NavType.BoolType; defaultValue = true })
        ) { backStackEntry ->
            val isSender = backStackEntry.arguments?.getBoolean("isSender") ?: true
            DeviceDiscoveryScreen(
                isSender = isSender,
                onNavigateBack = { navController.popBackStack() },
                onDeviceSelected = { _, _ -> },
                onNavigateToTransferProgress = {
                    navController.navigate(AppRoutes.TRANSFER_PROGRESS) {
                        popUpTo(AppRoutes.NEARBY_DISCOVERY) { inclusive = true }
                    }
                }
            )
        }
        composable(route = AppRoutes.TRANSFER_PROGRESS) {
            TransferProgressScreen(
                onNavigateBack = {
                    com.aethelsoft.grooveplayer.presentation.share.ShareIntentHolder.clear()
                    navController.popBackStack()
                }
            )
        }
        composable(route = AppRoutes.RECEIVE_APPROVAL) {
            ReceiveApprovalScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = AppRoutes.TRANSFER_STATUS) {
            TransferStatusScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
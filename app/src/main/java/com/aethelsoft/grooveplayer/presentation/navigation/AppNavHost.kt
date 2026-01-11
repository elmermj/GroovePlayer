package com.aethelsoft.grooveplayer.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aethelsoft.grooveplayer.presentation.home.HomeScreen
import com.aethelsoft.grooveplayer.presentation.library.favorites.FavoriteAlbumsScreen
import com.aethelsoft.grooveplayer.presentation.library.favorites.FavoriteArtistsScreen
import com.aethelsoft.grooveplayer.presentation.library.favorites.FavoriteTracksScreen
import com.aethelsoft.grooveplayer.presentation.library.recentlyplayed.RecentlyPlayedScreen
import com.aethelsoft.grooveplayer.presentation.library.songs.SongsScreen
import com.aethelsoft.grooveplayer.presentation.player.FullPlayerScreen

/**
 * Main navigation host for the app.
 * PlayerViewModel is accessed via CompositionLocal in each screen instead of parameter passing.
 */
@Composable
fun AppNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME,
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
        composable(
            route = AppRoutes.HOME,
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
            HomeScreen(
                onNavigateToSongs = { navController.navigate(AppRoutes.SONGS) },
                onNavigateToRecentlyPlayed = { navController.navigate(AppRoutes.RECENTLY_PLAYED) },
                onNavigateToFavoriteTracks = { navController.navigate(AppRoutes.FAVORITE_TRACKS) },
                onNavigateToFavoriteArtists = { navController.navigate(AppRoutes.FAVORITE_ARTISTS) },
                onNavigateToFavoriteAlbums = { navController.navigate(AppRoutes.FAVORITE_ALBUMS) }
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
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateBack = { navController.popBackStack() }
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
    }
}
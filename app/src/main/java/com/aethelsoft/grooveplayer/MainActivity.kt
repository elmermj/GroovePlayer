package com.aethelsoft.grooveplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.LocalPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.navigation.AppNavHost
import com.aethelsoft.grooveplayer.presentation.navigation.AppRoutes
import com.aethelsoft.grooveplayer.presentation.player.ui.MiniPlayerBar
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel
import com.aethelsoft.grooveplayer.utils.theme.ui.GroovePlayerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * This application will be a cross-platform app to gain as many users as possible.
 * Normally, I would prefer Flutter to do this. But since this application will require top
 * performance, hence I choose Native Kotlin Jetpack Compose. iOS will be built in Swift (SwiftUI).
 * The web, MacOS, and Windows, will be built in Flutter since I don't have time to write and
 * maintain 5 different code bases. It's an irony, but sometimes I have to make shortcuts.
 *
 * The main challenge is designing the UI. Once the design is done and all the features necessary
 * implemented, I would be a cake walk doing this. The design process is without Figma, everything
 * is done on top of my head.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GroovePlayerTheme {
                // Provide PlayerViewModel to entire app via CompositionLocal
                CompositionLocalProvider(LocalPlayerViewModel provides playerViewModel) {
                    GroovePlayerAppMain()
                }
            }
        }
    }
}

@Composable
fun GroovePlayerAppMain() {
    // Access activity-scoped PlayerViewModel from CompositionLocal
    val playerViewModel = LocalPlayerViewModel.current!!
    val navController = rememberNavController()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isFullScreenPlayerOpened by playerViewModel.isFullScreenPlayerOpened.collectAsState()
    var pendingNavigation by remember { mutableStateOf<String?>(null) }
    var isNavigating by remember { mutableStateOf(false) }

    // Keep full-screen player state in sync with the current route so that
    // pressing the system back button from FullPlayerScreen correctly restores the mini player.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        val isOnFullPlayer = currentRoute == AppRoutes.FULL_PLAYER
        if (isFullScreenPlayerOpened != isOnFullPlayer) {
            playerViewModel.setFullScreenPlayerOpen(isOnFullPlayer)
        }
    }

    // Reset navigation flags when full screen player closes
    LaunchedEffect(isFullScreenPlayerOpened) {
        if (!isFullScreenPlayerOpened) {
            // Reset flags when screen is closed to allow reopening
            isNavigating = false
            pendingNavigation = null
        }
    }

    // Handle delayed navigation after mini player closes
    LaunchedEffect(pendingNavigation) {
        val route = pendingNavigation ?: return@LaunchedEffect
        if (isNavigating) return@LaunchedEffect
        
        isNavigating = true
        // Wait for mini player exit animation to complete (~400ms)
        delay(350)
        navController.navigate(route)
        pendingNavigation = null
        // Reset navigation flag after a short delay to allow navigation to complete
        delay(100)
        isNavigating = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(
                    start = innerPadding.calculateLeftPadding(LayoutDirection.Ltr),
                    end = innerPadding.calculateRightPadding(LayoutDirection.Rtl),
                    top = 0.dp,
                    bottom = 0.dp
                )
        ) {
            AppNavHost(navController = navController)
            if (currentSong != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimatedVisibility(
                        visible = !isFullScreenPlayerOpened,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = 250, delayMillis = 350)
                        ) + slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(durationMillis = 350, delayMillis = 350)
                        ),
                        exit = fadeOut(
                            animationSpec = tween(durationMillis = 200)
                        ) + slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(durationMillis = 300, delayMillis = 100)
                        )
                    ) {
                        MiniPlayerBar(
                            onMiniPlayerClicked = {
                                // Prevent multiple rapid clicks from triggering multiple navigations
                                if (isNavigating || pendingNavigation != null) return@MiniPlayerBar
                                
                                if (isFullScreenPlayerOpened) {
                                    playerViewModel.setFullScreenPlayerOpen(false)
                                    navController.popBackStack()
                                } else {
                                    // Set state first to trigger mini player exit, then navigate after delay
                                    playerViewModel.setFullScreenPlayerOpen(true)
                                    pendingNavigation = AppRoutes.FULL_PLAYER
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


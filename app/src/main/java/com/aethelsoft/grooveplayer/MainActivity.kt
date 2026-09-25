package com.aethelsoft.grooveplayer

import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aethelsoft.grooveplayer.presentation.common.AppThemeViewModel
import com.aethelsoft.grooveplayer.presentation.common.LocalBluetoothViewModel
import com.aethelsoft.grooveplayer.presentation.common.LocalBottomBarSecondaryContent
import com.aethelsoft.grooveplayer.presentation.common.LocalNavigation
import com.aethelsoft.grooveplayer.presentation.common.LocalPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.common.NavigationActions
import com.aethelsoft.grooveplayer.presentation.share.ShareIntentHolder
import com.aethelsoft.grooveplayer.presentation.backup.LoginRestorePromptDialog
import com.aethelsoft.grooveplayer.presentation.backup.LoginRestorePromptViewModel
import com.aethelsoft.grooveplayer.presentation.backup.RestoreLaunchViewModel
import com.aethelsoft.grooveplayer.presentation.navigation.AppNavHost
import com.aethelsoft.grooveplayer.presentation.navigation.AppRoutes
import com.aethelsoft.grooveplayer.presentation.player.BluetoothViewModel
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel
import com.aethelsoft.grooveplayer.data.share.NfcShareDiscovery
import com.aethelsoft.grooveplayer.presentation.share.ShareNfcReceiver
import com.aethelsoft.grooveplayer.presentation.player.ui.MiniPlayerBar
import com.aethelsoft.grooveplayer.utils.LocalAdaptiveWindowInfo
import com.aethelsoft.grooveplayer.utils.rememberAdaptiveWindowInfo
import com.aethelsoft.grooveplayer.utils.requestedOrientationFor
import com.aethelsoft.grooveplayer.utils.rememberNotificationPermissionState
import com.aethelsoft.grooveplayer.utils.rememberRecordAudioPermissionState
import com.aethelsoft.grooveplayer.utils.theme.ui.GroovePlayerTheme
import com.aethelsoft.grooveplayer.presentation.ads.AdsConsentHelper
import com.aethelsoft.grooveplayer.presentation.ads.AdsViewModel
import com.aethelsoft.grooveplayer.presentation.ads.StartupInterstitialHelper
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

private sealed interface BottomBarState {
    data object None : BottomBarState
    data object Confirmation : BottomBarState
    data object MiniPlayer : BottomBarState

    companion object {
        fun resolve(
            currentRoute: String?,
            hasSecondaryContent: Boolean,
            hasCurrentSong: Boolean,
            showMiniPlayerOnStart: Boolean,
            hasUserStartedPlayback: Boolean,
            isFullScreenPlayerOpened: Boolean,
        ): BottomBarState = when {
            // Screens with their own bottom bar or full-screen flows - MainActivity shows nothing
            currentRoute?.startsWith("share_confirmation") == true -> None
            currentRoute?.startsWith("nearby_discovery") == true -> None
            currentRoute == AppRoutes.TRANSFER_PROGRESS -> None
            currentRoute?.startsWith("restore_apply") == true -> None
            // Screens that set secondary content (e.g. SongsScreen selection mode)
            hasSecondaryContent -> Confirmation
            // Show mini player when playing and not in full-screen
            hasCurrentSong && (showMiniPlayerOnStart || hasUserStartedPlayback) && !isFullScreenPlayerOpened -> MiniPlayer
            else -> None
        }
    }
}

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
    private val bluetoothViewModel: BluetoothViewModel by viewModels()
    private val appThemeViewModel: AppThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // UMP consent (EEA/UK) before MobileAds / showing ads
        AdsConsentHelper.requestConsentThenInitAds(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        handleNfcIntent(intent)

        setContent {
            val grooveStyle by appThemeViewModel.style.collectAsState()
            GroovePlayerTheme(style = grooveStyle) {
                val adaptiveWindowInfo = rememberAdaptiveWindowInfo()

                // Compact windows are portrait-only. Crossing into either tablet bucket removes
                // the app-level lock; configChanges keeps this Activity/NavController alive.
                LaunchedEffect(adaptiveWindowInfo.deviceType) {
                    val target = requestedOrientationFor(adaptiveWindowInfo.deviceType)
                    if (requestedOrientation != target) requestedOrientation = target
                }

                // One live window snapshot and shared activity-scoped player are used by all routes.
                CompositionLocalProvider(
                    LocalAdaptiveWindowInfo provides adaptiveWindowInfo,
                    LocalPlayerViewModel provides playerViewModel,
                    LocalBluetoothViewModel provides bluetoothViewModel
                ) {
                    GroovePlayerAppMain()
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: android.content.Intent?) {
        if (intent == null) return
        if (android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED != intent.action &&
            android.nfc.NfcAdapter.ACTION_TAG_DISCOVERED != intent.action
        ) return
        val nfc = NfcShareDiscovery(this)
        val info = nfc.readSessionFromIntent(intent)
        info?.let { ShareNfcReceiver.tryEmit(it) }
    }
}

@Composable
fun GroovePlayerAppMain() {
    // Access activity-scoped PlayerViewModel from CompositionLocal
    val playerViewModel = LocalPlayerViewModel.current!!
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        playerViewModel.premiumStreamRequired.collect {
            val result = snackbarHostState.showSnackbar(
                message = "Cloud streaming is part of Premium.",
                actionLabel = "Upgrade",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                navController.navigate(AppRoutes.PROFILE)
            }
        }
    }
    LaunchedEffect(Unit) {
        playerViewModel.streamRefreshFailure.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }
    val adsViewModel: AdsViewModel = hiltViewModel()
    val activity = LocalActivity.current
    LaunchedEffect(Unit) {
        // Delay so first frame / permissions settle; never blocks Sign-In.
        // Then wait until privilege is confirmed. The post-restore restart used to
        // fire this while /v1/me was still in flight and the missing user looked Free.
        delay(1800)
        val act = activity ?: return@LaunchedEffect
        if (!adsViewModel.awaitCanShowStartupAd()) return@LaunchedEffect
        StartupInterstitialHelper.maybeShow(act, adsViewModel)
    }
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isFullScreenPlayerOpened by playerViewModel.isFullScreenPlayerOpened.collectAsState()
    val showMiniPlayerOnStart by playerViewModel.showMiniPlayerOnStart.collectAsState()
    val notificationsEnabled by playerViewModel.notificationsEnabled.collectAsState()
    var hasUserStartedPlayback by remember { mutableStateOf(false) }
    var pendingNavigation by remember { mutableStateOf<String?>(null) }
    var isNavigating by remember { mutableStateOf(false) }
    
    // Request notification permission on app start (Android 13+)
    val (hasNotificationPermission, requestNotificationPermission) = rememberNotificationPermissionState()
    // Request RECORD_AUDIO permission for the visualizer
    val (hasRecordAudioPermission, requestRecordAudioPermission) = rememberRecordAudioPermissionState()
    
    // Request notification permission on first launch when the user preference allows it
    LaunchedEffect(Unit) {
        if (notificationsEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission
        ) {
            // Small delay to let UI settle, then request permission
            delay(500)
            requestNotificationPermission()
        }

        // Ask for RECORD_AUDIO shortly after launch so the visualizer can use
        // real waveform data instead of the fallback template.
        if (!hasRecordAudioPermission) {
            delay(800)
            requestRecordAudioPermission()
        }

        // Restore last played song and position
        delay(1000) // Wait for app to initialize
        playerViewModel.restoreLastPlayedSong()
    }

    // Re-request when the user re-enables notifications from profile
    LaunchedEffect(notificationsEnabled) {
        if (notificationsEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission
        ) {
            requestNotificationPermission()
        }
    }

    // Also request when playback starts if permission not granted and preference allows it
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    LaunchedEffect(isPlaying) {
        if (isPlaying) hasUserStartedPlayback = true
        if (notificationsEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            isPlaying &&
            !hasNotificationPermission
        ) {
            requestNotificationPermission()
        }
        // Also request RECORD_AUDIO when playback starts if not yet granted,
        // so users who skipped the first prompt still get real visualization.
        if (isPlaying && !hasRecordAudioPermission) {
            requestRecordAudioPermission()
        }
    }

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
    
    // Provide navigation actions to entire app
    val navigationActions = NavigationActions(
        currentRoute = currentRoute,
        goBack = { navController.popBackStack() },
        openFullPlayer = {
            if (!isNavigating && pendingNavigation == null) {
                playerViewModel.setFullScreenPlayerOpen(true)
                pendingNavigation = AppRoutes.FULL_PLAYER
            }
        },
        closeFullPlayer = {
            playerViewModel.setFullScreenPlayerOpen(false)
            navController.popBackStack()
        },
        openProfile = {
            navController.navigate(AppRoutes.PROFILE)
        },
        openShare = {
            navController.navigate(AppRoutes.SHARE_OPTIONS)
        },
        openShareWithSongs = { songs ->
            ShareIntentHolder.setSongs(songs)
            navController.navigate(AppRoutes.SHARE_OPTIONS)
        },
        openShareViaNfcWithSongs = { songs ->
            ShareIntentHolder.setSongs(songs)
            navController.navigate(AppRoutes.shareConfirmationRoute("nfc"))
        },
        openShareViaNearbyWithSongs = { songs ->
            ShareIntentHolder.setSongs(songs)
            navController.navigate(AppRoutes.nearbyDiscoveryRoute(isSender = true))
        },
        openUiStyling = {
            navController.navigate(AppRoutes.UI_CUSTOMISATION)
        },
        openBackup = {
            navController.navigate(AppRoutes.BACKUP)
        },
    )

    val secondaryBottomContent = remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    // Clear confirmation/options bar when navigating away from screens that use it.
    // Screens set content via SideEffect/LaunchedEffect and clear via DisposableEffect on leave.
    LaunchedEffect(currentRoute) {
        val usesConfirmationBar = currentRoute == AppRoutes.SONGS ||
            currentRoute?.startsWith("share_confirmation") == true
        if (!usesConfirmationBar) {
            secondaryBottomContent.value = null
        }
    }

    CompositionLocalProvider(
        LocalNavigation provides navigationActions,
        LocalBottomBarSecondaryContent provides secondaryBottomContent
    ) {
        // Handle system back: pop nav stack, or exit app when at start destination.
        // Screen-level BackHandlers (drawer, search) are composed inside destinations and take priority.
        val activity = LocalActivity.current as? ComponentActivity
        BackHandler {
            if (!navController.popBackStack()) {
                activity?.finish()
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = GrooveTheme.colors.canvas,
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                val restoreLaunch: RestoreLaunchViewModel = hiltViewModel()
                val loginRestorePrompt: LoginRestorePromptViewModel = hiltViewModel()
                val showLoginRestorePrompt by loginRestorePrompt.visible.collectAsState()
                AppNavHost(
                    navController = navController,
                    startDestination = if (restoreLaunch.startOnApplyScreen) {
                        AppRoutes.restoreApplyRoute(startDownload = restoreLaunch.resumeDownload)
                    } else {
                        AppRoutes.HOME
                    },
                    onBackupRestoreVisible = loginRestorePrompt::onBackupRestoreVisible,
                    onManualRestoreOpened = loginRestorePrompt::onManualRestoreOpened,
                )
                if (showLoginRestorePrompt) {
                    LoginRestorePromptDialog(
                        onKeepCurrent = loginRestorePrompt::keepCurrentData,
                        onRestore = {
                            if (loginRestorePrompt.confirmRestore()) {
                                navController.navigate(
                                    AppRoutes.restoreApplyRoute(startDownload = true),
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        },
                    )
                }
                val bottomBarState = BottomBarState.resolve(
                    currentRoute = currentRoute,
                    hasSecondaryContent = secondaryBottomContent.value != null,
                    hasCurrentSong = currentSong != null,
                    showMiniPlayerOnStart = showMiniPlayerOnStart,
                    hasUserStartedPlayback = hasUserStartedPlayback,
                    isFullScreenPlayerOpened = isFullScreenPlayerOpened,
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimatedContent(
                        targetState = bottomBarState,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(250, delayMillis = 100)) + slideInVertically(
                                initialOffsetY = { fullHeight -> fullHeight },
                                animationSpec = tween(350, delayMillis = 100)
                            )) togetherWith (fadeOut(animationSpec = tween(200)) + slideOutVertically(
                                targetOffsetY = { fullHeight -> fullHeight },
                                animationSpec = tween(300, delayMillis = 100)
                            ))
                        },
                        label = "BottomBar"
                    ) { state ->
                        when (state) {
                            BottomBarState.Confirmation -> Box(modifier = Modifier.fillMaxWidth()) {
                                secondaryBottomContent.value?.invoke()
                            }
                            BottomBarState.MiniPlayer -> MiniPlayerBar(
                                onMiniPlayerClicked = {
                                    if (isNavigating || pendingNavigation != null) return@MiniPlayerBar
                                    playerViewModel.setFullScreenPlayerOpen(true)
                                    pendingNavigation = AppRoutes.FULL_PLAYER
                                }
                            )
                            BottomBarState.None -> { /* empty, allows exit transition from previous state */ }
                        }
                    }
                }
            }
        }
    }
}

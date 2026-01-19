package com.aethelsoft.grooveplayer.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.common.UiState
import com.aethelsoft.grooveplayer.presentation.home.layouts.LargeTabletHomeLayout
import com.aethelsoft.grooveplayer.presentation.home.layouts.PhoneHomeLayout
import com.aethelsoft.grooveplayer.presentation.home.layouts.TabletHomeLayout
import com.aethelsoft.grooveplayer.presentation.home.ui.PermissionRequiredComponent
import com.aethelsoft.grooveplayer.utils.APP_BAR_HEIGHT
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.rememberAudioPermissionState
import com.aethelsoft.grooveplayer.utils.rememberDeviceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSongs: () -> Unit,
    onNavigateToRecentlyPlayed: () -> Unit,
    onNavigateToFavoriteTracks: () -> Unit,
    onNavigateToFavoriteArtists: () -> Unit,
    onNavigateToFavoriteAlbums: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val (hasPermission, requestPermission) = rememberAudioPermissionState()
    val uiState by viewModel.uiState.collectAsState()
    val deviceType = rememberDeviceType()

    val listState = rememberLazyListState()

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.refresh()
    }

    /* ---------- APP BAR SCROLL ALPHA ---------- */
    val appBarAlpha by remember {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            if (index > 0) 1f else (offset / 200f).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0E))
            .padding()
    ) {

        /* ---------- CONTENT ---------- */
        if (!hasPermission) {
            PermissionRequiredComponent(requestPermission)
        } else {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error: ${state.message}")
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = viewModel::refresh) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    when (deviceType) {
                        DeviceType.PHONE -> {
                            PhoneHomeLayout(
                                state = state,
                                viewModel = viewModel,
                                onNavigateToSongs = onNavigateToSongs,
                                onNavigateToRecentlyPlayed = onNavigateToRecentlyPlayed,
                                onNavigateToFavoriteTracks = onNavigateToFavoriteTracks,
                                onNavigateToFavoriteArtists = onNavigateToFavoriteArtists,
                                onNavigateToFavoriteAlbums = onNavigateToFavoriteAlbums
                            )
                        }

                        DeviceType.TABLET -> {
                            TabletHomeLayout(
                                state = state,
                                viewModel = viewModel,
                                onNavigateToSongs = onNavigateToSongs,
                                onNavigateToRecentlyPlayed = onNavigateToRecentlyPlayed,
                                onNavigateToFavoriteTracks = onNavigateToFavoriteTracks,
                                onNavigateToFavoriteArtists = onNavigateToFavoriteArtists,
                                onNavigateToFavoriteAlbums = onNavigateToFavoriteAlbums
                            )
                        }

                        DeviceType.LARGE_TABLET -> {
                            LargeTabletHomeLayout(
                                state = state,
                                viewModel = viewModel,
                                onNavigateToSongs = onNavigateToSongs,
                                onNavigateToRecentlyPlayed = onNavigateToRecentlyPlayed,
                                onNavigateToFavoriteTracks = onNavigateToFavoriteTracks,
                                onNavigateToFavoriteArtists = onNavigateToFavoriteArtists,
                                onNavigateToFavoriteAlbums = onNavigateToFavoriteAlbums
                            )
                        }
                    }
                }
            }
        }

        /* ---------- APP BAR OVERLAY ---------- */
        XAppBar(
            title = "Your Library",
            appBarAlpha = appBarAlpha,
            deviceType = deviceType
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun XAppBar(
    title: String,
    modifier: Modifier = Modifier,
    appBarAlpha: Float,
    deviceType: DeviceType
) {
    val horizontalPadding = when (deviceType) {
        DeviceType.PHONE -> 16.dp
        DeviceType.TABLET -> 24.dp
        DeviceType.LARGE_TABLET -> M_PADDING
    }
    val density = LocalDensity.current
    val xContentWindowInsets = contentWindowInsets
    val safeInsets = remember(contentWindowInsets) { MutableWindowInsets(xContentWindowInsets) }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(APP_BAR_HEIGHT + WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.95f * 1),
                        Color.Black.copy(alpha = 0.6f * 1),
                        Color.Transparent
                    )
                )
            )
            .padding(
                top = 0.dp,
                start = horizontalPadding,
                end = horizontalPadding,
                bottom = 0.dp
            )
    ) {
        Column {
            Spacer(
                modifier = Modifier.height(safeInsets.getTop(density).dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}


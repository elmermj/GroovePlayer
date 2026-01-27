package com.aethelsoft.grooveplayer.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.aethelsoft.grooveplayer.utils.theme.icons.XSearch
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.common.UiState
import com.aethelsoft.grooveplayer.presentation.home.layouts.LargeTabletHomeLayout
import com.aethelsoft.grooveplayer.presentation.home.layouts.PhoneHomeLayout
import com.aethelsoft.grooveplayer.presentation.home.layouts.TabletHomeLayout
import com.aethelsoft.grooveplayer.presentation.home.ui.PermissionRequiredComponent
import com.aethelsoft.grooveplayer.presentation.home.ui.SearchBarComponent
import com.aethelsoft.grooveplayer.presentation.search.SearchBarViewModel
import com.aethelsoft.grooveplayer.domain.model.SearchSuggestion
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import com.aethelsoft.grooveplayer.utils.APP_BAR_HEIGHT
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.rememberAudioPermissionState
import com.aethelsoft.grooveplayer.utils.rememberDeviceType
import com.aethelsoft.grooveplayer.utils.theme.icons.XAlbum
import com.aethelsoft.grooveplayer.utils.theme.icons.XArtist
import com.aethelsoft.grooveplayer.utils.theme.icons.XMusic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSongs: () -> Unit,
    onNavigateToRecentlyPlayed: () -> Unit,
    onNavigateToFavoriteTracks: () -> Unit,
    onNavigateToFavoriteArtists: () -> Unit,
    onNavigateToFavoriteAlbums: () -> Unit,
    onNavigateToSearch: (String) -> Unit,
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

    var isSearchExpanded by remember { mutableStateOf(false) }
    var requestDismissSearchKey by remember { mutableStateOf(0) }
    var searchTextFieldBounds by remember { mutableStateOf<Rect?>(null) }
    val focusManager = LocalFocusManager.current
    val searchBarViewModel: SearchBarViewModel = hiltViewModel()
    val suggestions by searchBarViewModel.suggestions.collectAsState()
    val playerViewModel = rememberPlayerViewModel()
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0E))
            .padding()
    ) {
        if (isSearchExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(997f)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus()
                        requestDismissSearchKey++
                    }
            )
        }

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
            deviceType = deviceType,
            onNavigateToSearch = onNavigateToSearch,
            isSearchExpanded = isSearchExpanded,
            onSearchExpandedChange = { isSearchExpanded = it },
            requestDismissSearchKey = requestDismissSearchKey,
            onTextFieldPosition = { searchTextFieldBounds = it },
            searchBarViewModel = searchBarViewModel
        )
        
        // Search suggestions dropdown - rendered at HomeScreen level to avoid layout constraints
        if (isSearchExpanded && suggestions.isNotEmpty() && searchTextFieldBounds != null) {
            val density = LocalDensity.current
            val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
            val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
            val maxDropdownHeight = screenHeight * 0.4f // 40% of screen height
            
            // For phone layout, use full screen width minus padding
            // For tablet/large tablet, use fixed 360.dp width positioned relative to search bar
            val horizontalPadding = when (deviceType) {
                DeviceType.PHONE -> 16.dp
                DeviceType.TABLET -> 24.dp
                DeviceType.LARGE_TABLET -> M_PADDING
            }
            
            val dropdownWidth = if (deviceType == DeviceType.PHONE) {
                screenWidth - (horizontalPadding * 2)
            } else {
                360.dp
            }
            
            val dropdownOffsetX = if (deviceType == DeviceType.PHONE) {
                // For phone, align to left edge with padding
                horizontalPadding
            } else {
                // For tablet/large tablet, position relative to search bar
                with(density) { searchTextFieldBounds!!.right.toDp() - 360.dp }
            }
            
            androidx.compose.material3.Card(
                modifier = Modifier
                    .offset(
                        x = dropdownOffsetX,
                        y = with(density) { searchTextFieldBounds!!.bottom.toDp() + 8.dp }
                    )
                    .width(dropdownWidth)
                    .heightIn(max = maxDropdownHeight)
                    .zIndex(1000f), // Ensure it's on top
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxDropdownHeight), // Ensure LazyColumn respects max height for scrolling
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(suggestions) { suggestion ->
                        SearchSuggestionItem(
                            suggestion = suggestion,
                            onClick = {
                                isSearchExpanded = false
                                focusManager.clearFocus()
                                when (suggestion) {
                                    is SearchSuggestion.QuerySuggestion -> {
                                        // Navigate to search screen with the query
                                        onNavigateToSearch(suggestion.displayTitle)
                                    }
                                    is SearchSuggestion.SongSuggestion -> {
                                        // Play the song
                                        scope.launch {
                                            try {
                                                // Save the click to search history
                                                searchBarViewModel.saveSongClick(
                                                    suggestion.songId,
                                                    suggestion.displayTitle,
                                                    suggestion.displaySubtitle ?: "",
                                                    suggestion.artworkUrl
                                                )
                                                // Get all songs and find the clicked song
                                                val allSongs = searchBarViewModel.getAllSongs()
                                                val songIndex = allSongs.indexOfFirst { it.id == suggestion.songId }
                                                if (songIndex >= 0) {
                                                    playerViewModel.setQueue(allSongs, songIndex)
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("HomeScreen", "Error playing song: ${e.message}", e)
                                            }
                                        }
                                    }
                                    is SearchSuggestion.ArtistSuggestion -> {
                                        // TODO: Navigate to artist
                                    }
                                    is SearchSuggestion.AlbumSuggestion -> {
                                        // TODO: Navigate to album
                                    }
                                }
                            },
                            onGetArtwork = { artistName ->
                                searchBarViewModel.getArtistArtwork(artistName)
                            },
                            onGetAlbumArtwork = { albumName, artistName ->
                                searchBarViewModel.getAlbumArtwork(albumName, artistName)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestionItem(
    suggestion: SearchSuggestion,
    onClick: () -> Unit,
    onGetArtwork: suspend (String) -> String?,
    onGetAlbumArtwork: suspend (String, String) -> String?
) {
    var artworkUrl by remember { mutableStateOf<String?>(suggestion.artworkUrl) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(suggestion) {
        when (suggestion) {
            is SearchSuggestion.ArtistSuggestion -> {
                if (artworkUrl == null) {
                    artworkUrl = onGetArtwork(suggestion.artistName)
                }
            }
            is SearchSuggestion.AlbumSuggestion -> {
                if (artworkUrl == null) {
                    artworkUrl = onGetAlbumArtwork(suggestion.albumName, suggestion.artistName)
                }
            }
            else -> {}
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Artwork or placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    getPlaceholderIcon(suggestion = suggestion),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        
        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.displayTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            if (suggestion.displaySubtitle != null) {
                Text(
                    text = suggestion.displaySubtitle!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun getPlaceholderIcon(
    suggestion: SearchSuggestion
): ImageVector {
    return when (suggestion) {
        is SearchSuggestion.AlbumSuggestion -> {
            XAlbum
        }
        is SearchSuggestion.ArtistSuggestion -> {
            XArtist
        }
        is SearchSuggestion.QuerySuggestion -> {
            XSearch
        }
        is SearchSuggestion.SongSuggestion -> {
            XMusic
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun XAppBar(
    title: String,
    modifier: Modifier = Modifier,
    appBarAlpha: Float,
    deviceType: DeviceType,
    onNavigateToSearch: (String) -> Unit = {},
    isSearchExpanded: Boolean = false,
    onSearchExpandedChange: (Boolean) -> Unit = {},
    requestDismissSearchKey: Int = 0,
    onTextFieldPosition: ((Rect) -> Unit)? = null,
    searchBarViewModel: SearchBarViewModel
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // For phone layout, show title only when search is not expanded
                // For tablet/large tablet, always show title
                if (deviceType == DeviceType.PHONE) {
                    AnimatedVisibility(
                        visible = !isSearchExpanded,
                        enter = fadeIn(
                            animationSpec = androidx.compose.animation.core.tween(200)
                        ),
                        exit = fadeOut(
                            animationSpec = androidx.compose.animation.core.tween(200)
                        )
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
                SearchBarComponent(
                    onSearch = onNavigateToSearch,
                    modifier = if (deviceType == DeviceType.PHONE) {
                        if (isSearchExpanded) {
                            Modifier.weight(1f)
                        } else {
                            Modifier
                        }
                    } else {
                        Modifier.weight(1f)
                    },
                    onExpandedChange = onSearchExpandedChange,
                    onRequestDismiss = remember(requestDismissSearchKey) { { } },
                    onTextFieldPosition = onTextFieldPosition,
                    deviceType = deviceType,
                    isSearchExpanded = isSearchExpanded,
                    viewModel = searchBarViewModel
                )
            }
        }
    }
}


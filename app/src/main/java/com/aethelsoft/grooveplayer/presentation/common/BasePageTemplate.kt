package com.aethelsoft.grooveplayer.presentation.common

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Brush
import coil3.compose.AsyncImage
import com.aethelsoft.grooveplayer.domain.model.SearchSuggestion
import com.aethelsoft.grooveplayer.presentation.home.ui.PermissionRequiredComponent
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileDrawer
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel
import com.aethelsoft.grooveplayer.presentation.search.SearchBarViewModel
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.rememberAudioPermissionState
import com.aethelsoft.grooveplayer.utils.rememberDeviceType
import com.aethelsoft.grooveplayer.utils.theme.icons.XAlbum
import com.aethelsoft.grooveplayer.utils.theme.icons.XArtist
import com.aethelsoft.grooveplayer.utils.theme.icons.XMusic
import com.aethelsoft.grooveplayer.utils.theme.icons.XSearch
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasePageTemplate(
    phoneLayout: @Composable () -> Unit,
    tabletLayout: @Composable () -> Unit,
    largeTabletLayout: @Composable () -> Unit,
    onNavigateToSearch: (String) -> Unit,
    baseBackgroundColor: Color = Color.Black,
    uiError: (@Composable (String) -> Unit)? = null,
    uiLoading: (@Composable (String) -> Unit)? = null,
    isSearchEnabled: Boolean,
    pageTitle: String,
    viewModel: BaseViewModel = hiltViewModel(),
    /**
     * If this value false, this page template will not provide any kind of AppBar.
     * You will have to provide your own AppBar (recommended: GradientAppBar)
     *
     * If this value is true, this page template will use XAppBar
     */
    useSearchBar: Boolean,
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
    var isProfileDrawerOpen by remember { mutableStateOf(false) }
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

        // Intercept native back button when search is expanded so that
        // the first back press closes search instead of navigating away.
        BackHandler(enabled = isSearchExpanded) {
            isSearchExpanded = false
            focusManager.clearFocus()
            requestDismissSearchKey++
        }

        /* ---------- CONTENT ---------- */
        if (!hasPermission) {
            PermissionRequiredComponent(requestPermission)
        } else {
            when (val state = uiState) {
                is UiState.Loading -> {
                    if(uiLoading != null) uiLoading("Loading...")
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    if(uiError != null) uiError(state.message)
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
                            phoneLayout()
                        }

                        DeviceType.TABLET -> {
                            tabletLayout()
                        }

                        DeviceType.LARGE_TABLET -> {
                            largeTabletLayout()
                        }
                    }
                }

                is UiState.Idle -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(baseBackgroundColor),
                    )
                }
            }
        }

        /**
         * Use XAppBar when useSearchBar is true. If it's false, then you must create your own AppBar
         * Recommended AppBar when useSearchBar is false => GradientAppBar
         *
         **/
        if (useSearchBar) {
            val navigation = rememberNavigationActions()
            Column {
                Box(
                    modifier = Modifier
                        .height(
                            WindowInsets.statusBars.asPaddingValues()
                                .calculateTopPadding() + (M_PADDING)
                        )
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black,
                                    Color.Black.copy(alpha = 0.85f * 1),
                                )
                            )
                        )
                )
                XAppBar(
                    title = pageTitle,
                    appBarAlpha = appBarAlpha,
                    deviceType = deviceType,
                    onNavigateToSearch = onNavigateToSearch,
                    isSearchExpanded = isSearchExpanded,
                    onSearchExpandedChange = { isSearchExpanded = it },
                    requestDismissSearchKey = requestDismissSearchKey,
                    onTextFieldPosition = { searchTextFieldBounds = it },
                    searchBarViewModel = searchBarViewModel,
                    onProfileDrawerOpen = { isProfileDrawerOpen = true },
                )
            }

            // Profile drawer overlay for tablet/large tablet - rendered at root Box level
            // so it can fill the screen and overlay everything (not constrained by Column layout)
            if (deviceType != DeviceType.PHONE) {
                BackHandler(enabled = isProfileDrawerOpen) {
                    isProfileDrawerOpen = false
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(999f)
                ) {
                    ProfileDrawer(
                        isOpen = isProfileDrawerOpen,
                        onClose = { isProfileDrawerOpen = false },
                        onNavigateToShare = {
                            isProfileDrawerOpen = false
                            navigation.openShare()
                        },
                        deviceType = deviceType,
                    )
                }
            }
        } else {
            Column() {
                Box(
                    modifier = Modifier
                        .height(
                            WindowInsets.statusBars.asPaddingValues()
                                .calculateTopPadding() + (M_PADDING)
                        )
                        .width(if (deviceType == DeviceType.TABLET) 360.dp else 420.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black,
                                    Color.Black.copy(alpha = 0.85f * 1),
                                )
                            )
                        )
                )
                GradientAppBar(
                    title = pageTitle,
                    deviceType = deviceType,
                )
            }
        }

        // Search suggestions dropdown - rendered at HomeScreen level to avoid layout constraints
        SearchSuggestionDropDown(
            isActive = isSearchExpanded && suggestions.isNotEmpty() && searchTextFieldBounds != null,
            suggestions = suggestions,
            searchBarViewModel = searchBarViewModel,
            deviceType = deviceType,
            searchTextFieldBounds = searchTextFieldBounds,
            onSuggestionClicked = { suggestion ->
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
                                Log.e("HomeScreen", "Error playing song: ${e.message}", e)
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
            }
        )
    }
}

@Composable
private fun SearchSuggestionDropDown(
    isActive: Boolean,
    suggestions: List<SearchSuggestion> = emptyList(),
    searchBarViewModel: SearchBarViewModel = hiltViewModel(),
    deviceType: DeviceType = rememberDeviceType(),
    searchTextFieldBounds: Rect? = null,
    onSuggestionClicked: (SearchSuggestion) -> Unit = {},
){
    if(isActive){
        val density = LocalDensity.current
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
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
        Card(
            modifier = Modifier
                .offset(
                    x = dropdownOffsetX,
                    y = with(density) { searchTextFieldBounds!!.bottom.toDp() + 8.dp }
                )
                .width(dropdownWidth)
                .heightIn(max = maxDropdownHeight)
                .zIndex(1000f), // Ensure it's on top
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDropdownHeight), // Ensure LazyColumn respects max height for scrolling
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(suggestions) { suggestion ->
                    SearchSuggestionItem(
                        suggestion = suggestion,
                        onClick = {
                            onSuggestionClicked(suggestion)
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
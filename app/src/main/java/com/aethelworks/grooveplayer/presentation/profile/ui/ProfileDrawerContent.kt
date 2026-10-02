package com.aethelworks.grooveplayer.presentation.profile.ui

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelworks.grooveplayer.presentation.common.GradientAppBar
import com.aethelworks.grooveplayer.presentation.common.grooveOverlayTopScrim
import com.aethelworks.grooveplayer.presentation.common.rememberSearchBarViewModel
import com.aethelworks.grooveplayer.presentation.common.topBarContentInset
import com.aethelworks.grooveplayer.presentation.profile.ProfileViewModel
import com.aethelworks.grooveplayer.presentation.profile.RecentUpdatesViewModel
import com.aethelworks.grooveplayer.presentation.profile.layouts.LargeTabletProfileLayout
import com.aethelworks.grooveplayer.presentation.profile.layouts.TabletProfileLayout
import com.aethelworks.grooveplayer.presentation.search.SearchBarViewModel
import com.aethelworks.grooveplayer.utils.DeviceType
import com.aethelworks.grooveplayer.utils.M_PADDING
import kotlinx.coroutines.delay


/**
 * Profile content to be used inside the tablet / large-tablet drawer.
 * The ViewModel is only created when this composable enters the composition.
 */
@Composable
fun ProfileDrawerContent(
    deviceType: DeviceType,
    isOpen: Boolean,
    onNavigateToShare: () -> Unit = {},
    onNavigateToUiStyling: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onClose: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    recentUpdatesViewModel: RecentUpdatesViewModel = hiltViewModel(),
) {
    // Closing the drawer confirms draft excluded-folder changes.
    // (Drawer ViewModel is retained on the host route, so onCleared alone is not enough.)
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.commitPendingExcludedFolders()
        }
    }

    val listState = rememberLazyListState()
    val appBarAlpha by remember {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            if (index > 0) 1f else (offset / 200f).coerceIn(0f, 1f)
        }
    }

    val searchViewModel: SearchBarViewModel = rememberSearchBarViewModel()
    var showRecentUpdates by remember { mutableStateOf(false) }
    val openRecentUpdates = { showRecentUpdates = true }

    BackHandler(enabled = showRecentUpdates) {
        showRecentUpdates = false
    }
    LaunchedEffect(isOpen) {
        if (!isOpen) {
            delay(320)
            showRecentUpdates = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .grooveOverlayTopScrim(),
        ) {
            if (showRecentUpdates) {
                RecentUpdatesBody(
                    updates = recentUpdatesViewModel.updates,
                    contentPadding = PaddingValues(
                        start = M_PADDING,
                        end = M_PADDING,
                        top = topBarContentInset() + M_PADDING,
                    ),
                )
            } else if (deviceType == DeviceType.LARGE_TABLET) {
                LargeTabletProfileLayout(
                    viewModel,
                    onNavigateToShare,
                    onNavigateToUiStyling,
                    onNavigateToBackup,
                    onOpenRecentUpdates = openRecentUpdates,
                )
            } else {
                TabletProfileLayout(
                    viewModel,
                    onNavigateToShare,
                    onNavigateToUiStyling,
                    onNavigateToBackup,
                    onOpenRecentUpdates = openRecentUpdates,
                )
            }
        }
        // Transparent control row; veil is drawn on the content box above.
        GradientAppBar(
            title = if (showRecentUpdates) recentUpdatesViewModel.updates.title else stringResource(R.string.settings_profile),
            deviceType = deviceType,
            modifier = Modifier,
            centerTitle = !showRecentUpdates,
            onBackClick = {
                if (showRecentUpdates) showRecentUpdates = false else onClose()
            },
        )
    }
}

package com.aethelsoft.grooveplayer.presentation.profile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.common.GradientAppBar
import com.aethelsoft.grooveplayer.presentation.common.rememberSearchBarViewModel
import com.aethelsoft.grooveplayer.presentation.profile.ProfileViewModel
import com.aethelsoft.grooveplayer.presentation.profile.layouts.LargeTabletProfileLayout
import com.aethelsoft.grooveplayer.presentation.profile.layouts.TabletProfileLayout
import com.aethelsoft.grooveplayer.presentation.search.SearchBarViewModel
import com.aethelsoft.grooveplayer.utils.DeviceType


/**
 * Profile content to be used inside the tablet / large-tablet drawer.
 * The ViewModel is only created when this composable enters the composition.
 */
@Composable
fun ProfileDrawerContent(
    deviceType: DeviceType,
    onNavigateToShare: () -> Unit = {},
    onNavigateToUiStyling: () -> Unit = {},
    onClose: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
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

    Box {
        if (deviceType == DeviceType.LARGE_TABLET) {
            LargeTabletProfileLayout(viewModel, onNavigateToShare, onNavigateToUiStyling)
        } else {
            TabletProfileLayout(viewModel, onNavigateToShare, onNavigateToUiStyling)
        }
        // GradientAppBar owns statusBarsPadding + APP_BAR_HEIGHT (same as Home / Profile route).
        GradientAppBar(
            title = "Profile",
            deviceType = deviceType,
            modifier = Modifier,
            onBackClick = onClose,
        )
    }
}

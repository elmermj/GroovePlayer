package com.aethelsoft.grooveplayer.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.common.BasePageTemplate
import com.aethelsoft.grooveplayer.presentation.profile.layouts.LargeTabletProfileLayout
import com.aethelsoft.grooveplayer.presentation.profile.layouts.PhoneProfileLayout
import com.aethelsoft.grooveplayer.presentation.profile.layouts.TabletProfileLayout

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToSearch: (String) -> Unit,
    onNavigateToShare: () -> Unit = {},
    onNavigateToUiStyling: () -> Unit = {},
){

    /**
     * Due to tablet and large tablet layouts use drawer,
     * the BasePageTemplate.tabletLayout and BasePageTemplate.largeTabletLayout
     * for this screen (ProfileScreen) will never be called.
     */

    // Exiting Profile confirms draft excluded-folder changes.
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.commitPendingExcludedFolders()
        }
    }

    BasePageTemplate(
        phoneLayout = { PhoneProfileLayout(viewModel, onNavigateToShare, onNavigateToUiStyling) },
        tabletLayout = { TabletProfileLayout(viewModel, onNavigateToShare, onNavigateToUiStyling) },
        largeTabletLayout = { LargeTabletProfileLayout(viewModel, onNavigateToShare, onNavigateToUiStyling) },
        onNavigateToSearch = onNavigateToSearch,
        viewModel = viewModel,
        isSearchEnabled = false,
        pageTitle = "Profile",
        useSearchBar = false,
    )

}

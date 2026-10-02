package com.aethelworks.grooveplayer.presentation.profile

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelworks.grooveplayer.presentation.common.BasePageTemplate
import com.aethelworks.grooveplayer.presentation.profile.layouts.LargeTabletProfileLayout
import com.aethelworks.grooveplayer.presentation.profile.layouts.PhoneProfileLayout
import com.aethelworks.grooveplayer.presentation.profile.layouts.TabletProfileLayout

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToSearch: (String) -> Unit,
    onNavigateToShare: () -> Unit = {},
    onNavigateToUiCustomisation: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToRecentUpdates: () -> Unit = {},
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
        phoneLayout = {
            PhoneProfileLayout(
                viewModel,
                onNavigateToShare,
                onNavigateToUiCustomisation,
                onNavigateToBackup,
                onOpenRecentUpdates = onNavigateToRecentUpdates,
            )
        },
        tabletLayout = { TabletProfileLayout(viewModel, onNavigateToShare, onNavigateToUiCustomisation, onNavigateToBackup) },
        largeTabletLayout = { LargeTabletProfileLayout(viewModel, onNavigateToShare, onNavigateToUiCustomisation, onNavigateToBackup) },
        onNavigateToSearch = onNavigateToSearch,
        viewModel = viewModel,
        isSearchEnabled = false,
        pageTitle = stringResource(R.string.settings_profile),
        useSearchBar = false,
    )

}

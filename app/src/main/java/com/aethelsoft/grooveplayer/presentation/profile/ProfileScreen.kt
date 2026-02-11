package com.aethelsoft.grooveplayer.presentation.profile

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.common.BasePageTemplate
import com.aethelsoft.grooveplayer.presentation.common.GradientAppBar
import com.aethelsoft.grooveplayer.presentation.profile.layouts.LargeTabletProfileLayout
import com.aethelsoft.grooveplayer.presentation.profile.layouts.PhoneProfileLayout
import com.aethelsoft.grooveplayer.presentation.profile.layouts.TabletProfileLayout
import com.aethelsoft.grooveplayer.utils.rememberDeviceType

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToSearch: (String) -> Unit
){

    /**
     * Due to tablet and large tablet layouts use drawer,
     * the BasePageTemplate.tabletLayout and BasePageTemplate.largeTabletLayout
     * for this screen (ProfileScreen) will never be called.
     */
    BasePageTemplate(
        phoneLayout = { PhoneProfileLayout(viewModel) },
        tabletLayout = { TabletProfileLayout(viewModel) },
        largeTabletLayout = { LargeTabletProfileLayout(viewModel) },
        onNavigateToSearch = onNavigateToSearch,
        viewModel = viewModel,
        isSearchEnabled = false,
        pageTitle = "Profile",
        useSearchBar = false,
    )

}

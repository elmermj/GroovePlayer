package com.aethelsoft.grooveplayer.presentation.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.presentation.home.ui.SearchBarComponent
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileDrawer
import com.aethelsoft.grooveplayer.presentation.search.SearchBarViewModel
import com.aethelsoft.grooveplayer.utils.APP_BAR_HEIGHT
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.icons.XUser

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
    searchBarViewModel: SearchBarViewModel,
    isSearchEnabled: Boolean,
) {
    val horizontalPadding = when (deviceType) {
        DeviceType.PHONE -> S_PADDING
        DeviceType.TABLET -> S_PADDING
        DeviceType.LARGE_TABLET -> M_PADDING
    }
    val density = LocalDensity.current
    val xContentWindowInsets = contentWindowInsets
    val safeInsets = remember(contentWindowInsets) { MutableWindowInsets(xContentWindowInsets) }

    val navigation = rememberNavigationActions()
    var isProfileDrawerOpen by remember { mutableStateOf(false) }

    // For phone we navigate to a dedicated profile route.
    // For tablet / large tablet we open a drawer that shows the profile layout.
    val onProfileClick: () -> Unit = when (deviceType) {
        DeviceType.PHONE -> { { navigation.openProfile() } }
        DeviceType.TABLET, DeviceType.LARGE_TABLET -> { { isProfileDrawerOpen = true } }
    }

    if (isSearchEnabled) {
        SearchEnabledLayout(
            horizontalPadding = horizontalPadding,
            onNavigateToSearch = onNavigateToSearch,
            isSearchExpanded = isSearchExpanded,
            onSearchExpandedChange = onSearchExpandedChange,
            requestDismissSearchKey = requestDismissSearchKey,
            onTextFieldPosition = onTextFieldPosition,
            searchBarViewModel = searchBarViewModel,
            deviceType = deviceType,
            safeInsets = safeInsets,
            density = density,
            title = title,
            onProfileClick = onProfileClick,
        )
    } else {
        SearchDisabledLayout(
            horizontalPadding = horizontalPadding,
            title = title
        )
    }

    // Close the profile drawer on native back when it's open (tablet / large tablet).
    if (deviceType != DeviceType.PHONE) {
        BackHandler(enabled = isProfileDrawerOpen) {
            isProfileDrawerOpen = false
        }
    }

    if (deviceType != DeviceType.PHONE) {
        ProfileDrawer(
            isOpen = isProfileDrawerOpen,
            onClose = { isProfileDrawerOpen = false },
            deviceType = deviceType,
        )
    }

}

@Composable
private fun SearchDisabledLayout(
    horizontalPadding: Dp,
    title: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                APP_BAR_HEIGHT + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            )
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
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchEnabledLayout(
    horizontalPadding: Dp,
    onNavigateToSearch: (String) -> Unit = {},
    isSearchExpanded: Boolean = false,
    onSearchExpandedChange: (Boolean) -> Unit = {},
    requestDismissSearchKey: Int = 0,
    onTextFieldPosition: ((Rect) -> Unit)? = null,
    searchBarViewModel: SearchBarViewModel,
    deviceType: DeviceType,
    safeInsets: MutableWindowInsets,
    density: Density,
    title: String,
    onProfileClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                APP_BAR_HEIGHT + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // For phone layout, show title only when search is not expanded
                // For tablet/large tablet, always show title
                if (deviceType == DeviceType.PHONE) {
                    AnimatedVisibility(
                        visible = !isSearchExpanded,
                        enter = fadeIn(
                            animationSpec = tween(200)
                        ),
                        exit = fadeOut(
                            animationSpec = tween(200)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            IconButton(
                                onClick = onProfileClick
                            ) {
                                Icon(
                                    XUser,
                                    contentDescription = "Profile",
                                    tint = Color.White,
                                )
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }

                    }
                } else {
                    IconButton(
                        onClick = onProfileClick
                    ) {
                        Icon(
                            XUser,
                            contentDescription = "Profile",
                            tint = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.size(S_PADDING))
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

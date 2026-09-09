package com.aethelsoft.grooveplayer.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.aethelsoft.grooveplayer.presentation.home.ui.SearchBarComponent
import com.aethelsoft.grooveplayer.presentation.search.SearchBarViewModel
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.icons.XUser
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

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
    /** Called when profile icon is tapped on tablet/large tablet to open the drawer. */
    onProfileDrawerOpen: () -> Unit = {},
) {
    val navigation = rememberNavigationActions()

    // For phone we navigate to a dedicated profile route.
    // For tablet / large tablet we open a drawer (state handled by BasePageTemplate).
    val onProfileClick: () -> Unit = when (deviceType) {
        DeviceType.PHONE -> { { navigation.openProfile() } }
        DeviceType.TABLET, DeviceType.LARGE_TABLET -> onProfileDrawerOpen
    }

    Box(
        modifier = modifier.grooveTopBarContainer(deviceType),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // For phone layout, show title only when search is not expanded
            // For tablet/large tablet, always show title
            if (deviceType == DeviceType.PHONE) {
                AnimatedVisibility(
                    visible = !isSearchExpanded,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onProfileClick) {
                            Icon(
                                XUser,
                                contentDescription = "Profile",
                                tint = GrooveTheme.colors.onSurface,
                            )
                        }
                        Spacer(modifier = Modifier.size(S_PADDING))
                        Text(
                            text = title,
                            style = GrooveTheme.typography.pageTitle.toTextStyle(),
                            color = GrooveTheme.colors.onSurface,
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            XUser,
                            contentDescription = "Profile",
                            tint = GrooveTheme.colors.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.size(S_PADDING))
                    Text(
                        text = title,
                        style = GrooveTheme.typography.pageTitle.toTextStyle(),
                        color = GrooveTheme.colors.onSurface,
                    )
                }
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

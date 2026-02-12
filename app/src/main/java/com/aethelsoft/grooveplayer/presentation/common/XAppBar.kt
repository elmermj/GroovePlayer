package com.aethelsoft.grooveplayer.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.presentation.home.ui.SearchBarComponent
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

    val horizontalPadding = when (deviceType) {
        DeviceType.PHONE -> S_PADDING
        DeviceType.TABLET -> S_PADDING
        DeviceType.LARGE_TABLET -> M_PADDING
    }


    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(
                APP_BAR_HEIGHT + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.85f * 1),
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
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
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
                            onClick = onProfileClick,
                        ) {
                            Icon(
                                XUser,
                                contentDescription = "Profile",
                                tint = Color.White,
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.W700,
                            color = Color.White,
                        )
                    }

                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

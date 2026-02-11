package com.aethelsoft.grooveplayer.presentation.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.common.GradientAppBar
import com.aethelsoft.grooveplayer.presentation.common.XAppBar
import com.aethelsoft.grooveplayer.presentation.common.rememberSearchBarViewModel
import com.aethelsoft.grooveplayer.presentation.profile.ProfileViewModel
import com.aethelsoft.grooveplayer.presentation.profile.layouts.LargeTabletProfileLayout
import com.aethelsoft.grooveplayer.presentation.profile.layouts.PhoneProfileLayout
import com.aethelsoft.grooveplayer.presentation.profile.layouts.TabletProfileLayout
import com.aethelsoft.grooveplayer.presentation.search.SearchBarViewModel
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.M_PADDING


/**
 * Profile content to be used inside the tablet / large-tablet drawer.
 * The ViewModel is only created when this composable enters the composition.
 */
@Composable
fun ProfileDrawerContent(
    deviceType: DeviceType,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    // TODO: Wire layouts to viewModel when profile state is implemented.
    // Only for Large Tablets and Tablets layouts

    val listState = rememberLazyListState()
    val appBarAlpha by remember {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            if (index > 0) 1f else (offset / 200f).coerceIn(0f, 1f)
        }
    }

    val searchViewModel: SearchBarViewModel = rememberSearchBarViewModel()


    Box() {
        if(deviceType == DeviceType.LARGE_TABLET){
            LargeTabletProfileLayout(viewModel)
        } else {
            TabletProfileLayout(viewModel)
        }
        Column() {
            Box(
                modifier = Modifier
                    .height(
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + (M_PADDING * 2)
                    )
                    .width(if (deviceType == DeviceType.TABLET) 360.dp else 420.dp)
                    .background(Color.Black)
            )
            GradientAppBar(
                title = "Profile",
                deviceType = deviceType,
                modifier = Modifier
            )
        }
    }

}

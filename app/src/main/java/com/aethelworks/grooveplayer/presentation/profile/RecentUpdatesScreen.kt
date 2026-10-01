package com.aethelworks.grooveplayer.presentation.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelworks.grooveplayer.presentation.common.GrooveScreen
import com.aethelworks.grooveplayer.presentation.common.topBarContentInset
import com.aethelworks.grooveplayer.presentation.profile.ui.RecentUpdatesBody
import com.aethelworks.grooveplayer.utils.M_PADDING

@Composable
fun RecentUpdatesScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecentUpdatesViewModel = hiltViewModel(),
) {
    GrooveScreen(
        title = viewModel.updates.title,
        onBackClick = onNavigateBack,
        contentPadding = PaddingValues(0.dp),
    ) {
        RecentUpdatesBody(
            updates = viewModel.updates,
            contentPadding = PaddingValues(
                start = M_PADDING,
                end = M_PADDING,
                top = topBarContentInset() + M_PADDING,
            ),
        )
    }
}

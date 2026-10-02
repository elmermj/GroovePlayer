package com.aethelworks.grooveplayer.presentation.share

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelworks.grooveplayer.presentation.common.FlatCentered
import com.aethelworks.grooveplayer.presentation.common.FlatChevron
import com.aethelworks.grooveplayer.presentation.common.FlatLeadingIcon
import com.aethelworks.grooveplayer.presentation.common.FlatRow
import com.aethelworks.grooveplayer.presentation.common.GrooveScreen
import com.aethelworks.grooveplayer.presentation.common.OverlineLabel
import com.aethelworks.grooveplayer.presentation.common.flatSubtitleStyle
import com.aethelworks.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelworks.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelworks.grooveplayer.presentation.common.topBarContentInset
import com.aethelworks.grooveplayer.presentation.transfer.DeviceCapabilityRow
import com.aethelworks.grooveplayer.utils.theme.icons.XListMusic
import com.aethelworks.grooveplayer.utils.theme.icons.XShareMusic
import com.aethelworks.grooveplayer.utils.theme.icons.XSmartphone
import kotlinx.coroutines.launch

@Composable
fun ShareOptionsScreen(
    onNavigateBack: () -> Unit,
    onShareViaNfc: () -> Unit,
    onShareViaNearby: () -> Unit,
    onShareViaNearbyP2P: (() -> Unit)? = null,
    onReceiveViaNfc: () -> Unit,
    onReceiveViaNearby: () -> Unit,
    onReceiveViaNearbyP2P: (() -> Unit)? = null,
    onNavigateToTransferStatus: (() -> Unit)? = null,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val shareRequirements = rememberShareRequirementsStatus()
    val deviceCapability by viewModel.deviceCapability.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadSongsToShare()
    }

    GrooveScreen(
        title = "Share & Receive",
        onBackClick = onNavigateBack,
        centerTitle = true,
        flatBackdrop = true,
        contentPadding = PaddingValues(
            bottom = 12.dp + grooveBottomContentInset(includeMiniPlayer = rememberClearMiniPlayer()),
        ),
    ) {
        FlatCentered(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = topBarContentInset()),
            ) {
                ShareRequirementsBanner(status = shareRequirements)

                DeviceCapabilityRow(capability = deviceCapability)

                OverlineLabel("Share or receive media")

                FlatRow(
                    title = "Receive from nearby device (P2P)",
                    leading = { FlatLeadingIcon(XSmartphone) },
                    trailing = { FlatChevron() },
                    onClick = {
                        viewModel.clearSongsToReceive()
                        onReceiveViaNearbyP2P?.invoke() ?: onReceiveViaNearby()
                    },
                    showDivider = true,
                )
                FlatRow(
                    title = "Share music with nearby device (P2P)",
                    leading = { FlatLeadingIcon(XShareMusic) },
                    trailing = { FlatChevron() },
                    onClick = {
                        scope.launch {
                            viewModel.prepareToShareAsSender()
                            onShareViaNearbyP2P?.invoke() ?: onShareViaNearby()
                        }
                    },
                )
                Text(
                    text = "Works without WiFi. Bluetooth or WiFi Direct.",
                    style = flatSubtitleStyle(),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )

                onNavigateToTransferStatus?.let { navigate ->
                    OverlineLabel("Transfers")
                    FlatRow(
                        title = "Transfer Status",
                        subtitle = "View active transfers and history",
                        leading = { FlatLeadingIcon(XListMusic) },
                        trailing = { FlatChevron() },
                        onClick = navigate,
                        showDivider = true,
                    )
                }
            }
        }
    }
}

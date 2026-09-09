package com.aethelsoft.grooveplayer.presentation.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.common.GrooveCardSubtitle
import com.aethelsoft.grooveplayer.presentation.common.GrooveCardTitle
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.GrooveSectionTitle
import com.aethelsoft.grooveplayer.presentation.common.GrooveSurfaceCard
import com.aethelsoft.grooveplayer.presentation.common.GrooveTinySpacer
import com.aethelsoft.grooveplayer.presentation.transfer.DeviceCapabilityCard
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(M_PADDING),
        ) {
            ShareRequirementsBanner(status = shareRequirements)

            DeviceCapabilityCard(capability = deviceCapability)

            GrooveSectionTitle("Share or receive media")

            GrooveSurfaceCard(
                onClick = {
                    viewModel.clearSongsToReceive()
                    onReceiveViaNearbyP2P?.invoke() ?: onReceiveViaNearby()
                }
            ) {
                GrooveCardTitle("Receive from nearby device (P2P)")
                GrooveTinySpacer()
                GrooveCardSubtitle("Works without WiFi. Bluetooth or WiFi Direct.")
            }

            GrooveSurfaceCard(
                onClick = {
                    scope.launch {
                        viewModel.prepareToShareAsSender()
                        onShareViaNearbyP2P?.invoke() ?: onShareViaNearby()
                    }
                }
            ) {
                GrooveCardTitle("Share music with nearby device (P2P)")
                GrooveTinySpacer()
                GrooveCardSubtitle("Works without WiFi. Bluetooth or WiFi Direct.")
            }

            onNavigateToTransferStatus?.let { navigate ->
                Spacer(modifier = Modifier.height(S_PADDING))
                GrooveSectionTitle("Transfers")
                GrooveSurfaceCard(onClick = navigate) {
                    GrooveCardTitle("Transfer Status")
                    GrooveTinySpacer()
                    GrooveCardSubtitle("View active transfers and history")
                }
            }
        }
    }
}

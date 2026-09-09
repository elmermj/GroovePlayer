package com.aethelsoft.grooveplayer.presentation.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.data.share.ShareProtocol
import com.aethelsoft.grooveplayer.domain.model.ShareSessionInfo
import com.aethelsoft.grooveplayer.presentation.common.GrooveCardSubtitle
import com.aethelsoft.grooveplayer.presentation.common.GrooveCardTitle
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.GrooveSurfaceCard
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.getLocalIpAddress
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ShareViaNearbyScreen(
    onNavigateBack: () -> Unit,
    onOfferReceived: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val songs by viewModel.songsToShare.collectAsState()
    val isSender = songs.isNotEmpty()

    LaunchedEffect(Unit) {
        viewModel.loadSongsToShare()
    }

    LaunchedEffect(isSender, songs) {
        if (isSender && songs.isNotEmpty()) {
            val host = withContext(Dispatchers.IO) {
                getLocalIpAddress() ?: "127.0.0.1"
            }
            val sessionInfo = ShareSessionInfo(
                host = host,
                port = ShareProtocol.DEFAULT_PORT,
                sessionToken = ShareProtocol.generateSessionToken(),
                deviceName = android.os.Build.MODEL
            )
            viewModel.startSender(sessionInfo)
        }
    }

    GrooveScreen(
        title = "Share with nearby",
        onBackClick = onNavigateBack,
    ) {
        if (isSender) {
            WaitingState(
                title = "Waiting for receiver…",
                subtitle = "Make sure both devices are on the same Wi‑Fi network",
            )
        } else {
            NearbyDeviceList(
                viewModel = viewModel,
                onDeviceSelected = { info ->
                    viewModel.connectAndReceiveOffer(info)
                    onOfferReceived()
                }
            )
        }
    }
}

@Composable
private fun WaitingState(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(S_PADDING))
        GrooveMutedText(text = subtitle)
        Spacer(modifier = Modifier.height(M_PADDING * 2))
        CircularProgressIndicator(color = SoftWhite)
    }
}

@Composable
private fun NearbyDeviceList(
    viewModel: ShareViewModel,
    onDeviceSelected: (ShareSessionInfo) -> Unit
) {
    val devices by viewModel.discoveredDevices.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startDeviceDiscovery()
    }

    if (devices.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = SoftWhite)
                Spacer(modifier = Modifier.height(S_PADDING))
                GrooveMutedText("Searching for nearby devices…")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(S_PADDING),
        ) {
            items(devices) { info ->
                GrooveSurfaceCard(onClick = { onDeviceSelected(info) }) {
                    GrooveCardTitle(info.deviceName)
                    GrooveCardSubtitle("${info.host}:${info.port}")
                }
            }
        }
    }
}

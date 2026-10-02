package com.aethelworks.grooveplayer.presentation.share

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelworks.grooveplayer.data.share.ShareProtocol
import com.aethelworks.grooveplayer.domain.model.ShareSessionInfo
import com.aethelworks.grooveplayer.presentation.common.FlatCentered
import com.aethelworks.grooveplayer.presentation.common.FlatChevron
import com.aethelworks.grooveplayer.presentation.common.FlatLeadingIcon
import com.aethelworks.grooveplayer.presentation.common.FlatRow
import com.aethelworks.grooveplayer.presentation.common.GrooveScreen
import com.aethelworks.grooveplayer.presentation.common.PulsingRing
import com.aethelworks.grooveplayer.presentation.common.flatBarTitleStyle
import com.aethelworks.grooveplayer.presentation.common.flatSubtitleStyle
import com.aethelworks.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelworks.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelworks.grooveplayer.presentation.common.topBarContentInset
import com.aethelworks.grooveplayer.utils.M_PADDING
import com.aethelworks.grooveplayer.utils.S_PADDING
import com.aethelworks.grooveplayer.utils.getLocalIpAddress
import com.aethelworks.grooveplayer.utils.theme.icons.XSmartphone
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
        centerTitle = true,
        flatBackdrop = true,
        contentPadding = PaddingValues(
            bottom = 12.dp + grooveBottomContentInset(includeMiniPlayer = rememberClearMiniPlayer()),
        ),
    ) {
        FlatCentered(Modifier.fillMaxSize()) {
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
}

@Composable
private fun WaitingState(
    title: String,
    subtitle: String,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            PulsingRing()
            Spacer(modifier = Modifier.height(M_PADDING))
            Text(
                text = title,
                style = flatBarTitleStyle(),
                textAlign = TextAlign.Center,
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(S_PADDING))
                Text(
                    text = subtitle,
                    style = flatSubtitleStyle(),
                    textAlign = TextAlign.Center,
                )
            }
        }
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
        WaitingState(
            title = "Searching for nearby devices…",
            subtitle = "",
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topBarContentInset()),
        ) {
            items(devices) { info ->
                FlatRow(
                    title = info.deviceName,
                    subtitle = "${info.host}:${info.port}",
                    leading = { FlatLeadingIcon(XSmartphone) },
                    trailing = { FlatChevron() },
                    onClick = { onDeviceSelected(info) },
                    showDivider = true,
                )
            }
        }
    }
}

package com.aethelsoft.grooveplayer.presentation.share

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.data.share.ShareProtocol
import com.aethelsoft.grooveplayer.domain.model.ShareSessionInfo
import com.aethelsoft.grooveplayer.utils.getLocalIpAddress
import com.aethelsoft.grooveplayer.utils.theme.icons.XBack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
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
            // NSD registration is done in startSender via NsdShareDiscovery - we'd need to inject it
            viewModel.startSender(sessionInfo)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Share with nearby device") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(XBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
            if (isSender) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(
                        text = "Waiting for receiver...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Make sure both devices are on the same Wi‑Fi network",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularProgressIndicator(color = Color.White)
                }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Searching for nearby devices...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(devices) { info ->
                Card(
                    onClick = { onDeviceSelected(info) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = info.deviceName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "${info.host}:${info.port}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}


package com.aethelsoft.grooveplayer.presentation.share

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.data.share.NfcShareDiscovery
import com.aethelsoft.grooveplayer.data.share.ShareProtocol
import com.aethelsoft.grooveplayer.domain.model.ShareSessionInfo
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.GrooveActionButton
import com.aethelsoft.grooveplayer.presentation.common.GrooveCardSubtitle
import com.aethelsoft.grooveplayer.presentation.common.GrooveCardTitle
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.GrooveSurfaceCard
import com.aethelsoft.grooveplayer.presentation.common.MediaArtwork
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelsoft.grooveplayer.utils.L_PADDING
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING
import com.aethelsoft.grooveplayer.utils.getLocalIpAddress
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftBlack
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareConfirmationScreen(
    shareMethod: String,
    onNavigateBack: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val songs by viewModel.songsToShare.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val isNfc = shareMethod == "nfc"

    LaunchedEffect(Unit) {
        viewModel.loadSongsToShare()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GrooveScreen(
            title = "Confirm Share",
            onBackClick = onNavigateBack,
            contentPadding = PaddingValues(
                start = L_PADDING,
                end = L_PADDING,
                bottom = 96.dp,
            ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(XS_PADDING),
                contentPadding = PaddingValues(vertical = M_PADDING),
            ) {
                items(songs) { song ->
                    ShareConfirmationSongItem(song = song)
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black,
                        )
                    )
                )
                .padding(
                    start = M_PADDING,
                    end = M_PADDING,
                    top = S_PADDING,
                    bottom = S_PADDING + WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding(),
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(M_PADDING),
            ) {
                GrooveActionButton(
                    label = "Cancel",
                    onClick = onNavigateBack,
                    isPrimary = false,
                    modifier = Modifier.weight(1f),
                )
                GrooveActionButton(
                    label = "Confirm",
                    onClick = {
                        if (songs.isNotEmpty()) showBottomSheet = true
                    },
                    isPrimary = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showBottomSheet) {
        if (isNfc) {
            NfcListeningBottomSheet(
                onDismiss = { showBottomSheet = false },
                viewModel = viewModel
            )
        } else {
            NearbyDevicesBottomSheet(
                onDismiss = { showBottomSheet = false },
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun ShareConfirmationSongItem(song: Song) {
    GrooveSurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MediaArtwork(
                url = song.artworkUrl,
                kind = MediaArtworkKind.SONG,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                cornerRadius = XS_PADDING,
            )
            Spacer(modifier = Modifier.size(S_PADDING))
            Column(modifier = Modifier.weight(1f)) {
                GrooveCardTitle(song.title)
                GrooveCardSubtitle(song.artist)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NfcListeningBottomSheet(
    onDismiss: () -> Unit,
    viewModel: ShareViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val songs by viewModel.songsToShare.collectAsState()
    val nfcDiscovery = remember { activity?.let { NfcShareDiscovery(it) } }
    val isBeamSupported = nfcDiscovery?.isBeamSupported == true

    DisposableEffect(Unit) {
        nfcDiscovery?.enableForegroundDispatch()
        onDispose {
            nfcDiscovery?.disableForegroundDispatch()
        }
    }

    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            val host = withContext(Dispatchers.IO) {
                getLocalIpAddress() ?: "127.0.0.1"
            }
            val sessionInfo = ShareSessionInfo(
                host = host,
                port = ShareProtocol.DEFAULT_PORT,
                sessionToken = ShareProtocol.generateSessionToken(),
                deviceName = android.os.Build.MODEL
            )
            if (isBeamSupported && nfcDiscovery != null) {
                nfcDiscovery.setPushMessage(sessionInfo)
            }
            viewModel.startSender(sessionInfo)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.cancelTransfer()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = SoftBlack,
    ) {
        ShareSheetBody(
            title = "Listening via NFC",
            body = "The app is listening for NFC. Tap the other device to begin transferring.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NearbyDevicesBottomSheet(
    onDismiss: () -> Unit,
    viewModel: ShareViewModel
) {
    val songs by viewModel.songsToShare.collectAsState()

    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.cancelTransfer()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = SoftBlack,
    ) {
        ShareSheetBody(
            title = "Waiting for receiver",
            body = "The other device can find you in their \"Share with nearby\" list. They will receive a notification to approve the transfer.",
        )
    }
}

@Composable
private fun ShareSheetBody(
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(L_PADDING)
            .padding(bottom = M_PADDING * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = SoftWhite)
        Spacer(modifier = Modifier.height(M_PADDING))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(S_PADDING))
        GrooveMutedText(text = body)
    }
}

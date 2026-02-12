package com.aethelsoft.grooveplayer.presentation.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.utils.theme.icons.XBack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareOptionsScreen(
    onNavigateBack: () -> Unit,
    onShareViaNfc: () -> Unit,
    onShareViaNearby: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val shareRequirements = rememberShareRequirementsStatus()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadSongsToShare()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share Music") },
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
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShareRequirementsBanner(status = shareRequirements)

            Text(
                text = "Share music",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            ShareOptionCard(
                title = "Share music via Tap (NFC)",
                subtitle = "Hold phones back to back to send",
                onClick = {
                    scope.launch {
                        viewModel.prepareToShareAsSender()
                        onShareViaNfc()
                    }
                }
            )
            ShareOptionCard(
                title = "Share music with nearby device",
                subtitle = "Send to devices on the same network",
                onClick = {
                    scope.launch {
                        viewModel.prepareToShareAsSender()
                        onShareViaNearby()
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Receive music",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            ShareOptionCard(
                title = "Receive via Tap (NFC)",
                subtitle = "Tap sender's phone to receive",
                onClick = {
                    viewModel.clearSongsToReceive()
                    onShareViaNfc()
                }
            )
            ShareOptionCard(
                title = "Receive from nearby device",
                subtitle = "Find nearby devices on the same network",
                onClick = {
                    viewModel.clearSongsToReceive()
                    onShareViaNearby()
                }
            )
        }
    }
}

@Composable
private fun ShareOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

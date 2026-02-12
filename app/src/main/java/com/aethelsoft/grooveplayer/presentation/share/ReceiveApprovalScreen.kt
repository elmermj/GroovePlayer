package com.aethelsoft.grooveplayer.presentation.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.domain.model.ShareableItem
import com.aethelsoft.grooveplayer.utils.theme.icons.XBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveApprovalScreen(
    onNavigateBack: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val items by viewModel.offerItems.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Receive Music") },
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
        Text(
            text = "Select songs to receive",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                ReceiveItemRow(
                    item = item,
                    isSelected = item.id in selectedIds,
                    onToggle = { viewModel.toggleSelection(item.id) }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { viewModel.rejectOffer(); onNavigateBack() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Decline")
            }
            Button(
                onClick = {
                    viewModel.approveAndReceive()
                },
                modifier = Modifier.weight(1f),
                enabled = selectedIds.isNotEmpty()
            ) {
                Text("Accept (${selectedIds.size})")
            }
        }
    }
}

@Composable
private fun ReceiveItemRow(
    item: ShareableItem,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Text(
                text = "${item.artist}${item.album?.let { " • $it" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

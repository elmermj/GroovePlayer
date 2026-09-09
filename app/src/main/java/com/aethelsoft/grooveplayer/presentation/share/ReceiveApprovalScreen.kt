package com.aethelsoft.grooveplayer.presentation.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.domain.model.ShareableItem
import com.aethelsoft.grooveplayer.presentation.common.GrooveActionRow
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.GrooveSurfaceCard
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

@Composable
fun ReceiveApprovalScreen(
    onNavigateBack: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val items by viewModel.offerItems.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    GrooveScreen(
        title = "Receive Music",
        onBackClick = onNavigateBack,
    ) {
        GrooveMutedText(
            text = "Select songs to receive",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = S_PADDING),
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(S_PADDING),
        ) {
            items(items) { item ->
                ReceiveItemRow(
                    item = item,
                    isSelected = item.id in selectedIds,
                    onToggle = { viewModel.toggleSelection(item.id) }
                )
            }
        }
        GrooveActionRow(
            primaryLabel = "Accept (${selectedIds.size})",
            onPrimary = { viewModel.approveAndReceive() },
            secondaryLabel = "Decline",
            onSecondary = {
                viewModel.rejectOffer()
                onNavigateBack()
            },
            primaryEnabled = selectedIds.isNotEmpty(),
            modifier = Modifier.padding(top = M_PADDING),
        )
    }
}

@Composable
private fun ReceiveItemRow(
    item: ShareableItem,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    GrooveSurfaceCard(onClick = onToggle) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SoftWhite,
                    uncheckedColor = SoftWhite.copy(alpha = 0.5f),
                    checkmarkColor = Color.Black,
                )
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                )
                GrooveMutedText(
                    text = "${item.artist}${item.album?.let { " • $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

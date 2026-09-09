package com.aethelsoft.grooveplayer.presentation.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.domain.model.transfer.Transfer
import com.aethelsoft.grooveplayer.domain.model.transfer.TransferStatus
import com.aethelsoft.grooveplayer.presentation.common.GrooveActionButton
import com.aethelsoft.grooveplayer.presentation.common.GrooveCardTitle
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.GrooveSectionTitle
import com.aethelsoft.grooveplayer.presentation.common.GrooveSurfaceCard
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftBlack
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransferStatusScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransferStatusViewModel = hiltViewModel(),
) {
    val activeTransfers by viewModel.activeTransfers.collectAsState()
    val transferHistory by viewModel.transferHistory.collectAsState()

    GrooveScreen(
        title = "Transfer Status",
        onBackClick = onNavigateBack,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(M_PADDING),
        ) {
            if (activeTransfers.isNotEmpty()) {
                item { GrooveSectionTitle("Active Transfers") }
                items(activeTransfers) { transfer ->
                    ActiveTransferCard(
                        transfer = transfer,
                        onPause = { viewModel.pauseTransfer(transfer.id) },
                        onResume = { viewModel.resumeTransfer(transfer.id) },
                        onCancel = { viewModel.cancelTransfer(transfer.id) },
                    )
                }
            }
            item { GrooveSectionTitle("Transfer History") }
            items(transferHistory) { transfer ->
                TransferHistoryCard(transfer = transfer)
            }
        }
    }
}

@Composable
private fun ActiveTransferCard(
    transfer: Transfer,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    GrooveSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GrooveCardTitle(transfer.deviceName)
            StatusBadge(status = transfer.overallStatus)
        }
        Spacer(modifier = Modifier.height(S_PADDING))
        LinearProgressIndicator(
            progress = { transfer.progressPercent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(XS_PADDING),
            color = SoftWhite,
            trackColor = SoftBlack,
        )
        Spacer(modifier = Modifier.height(S_PADDING))
        transfer.files.forEach { file ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = XS_PADDING / 2),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GrooveMutedText(
                    text = file.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                GrooveMutedText(
                    text = "${file.progressPercent}%",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(modifier = Modifier.height(S_PADDING))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(S_PADDING),
        ) {
            when (transfer.overallStatus) {
                TransferStatus.TRANSFERRING, TransferStatus.CONNECTING, TransferStatus.RETRYING -> {
                    GrooveActionButton(
                        label = "Pause",
                        onClick = onPause,
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                    GrooveActionButton(
                        label = "Cancel",
                        onClick = onCancel,
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                }
                TransferStatus.PAUSED -> {
                    GrooveActionButton(
                        label = "Resume",
                        onClick = onResume,
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                    )
                    GrooveActionButton(
                        label = "Cancel",
                        onClick = onCancel,
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                }
                else -> {
                    GrooveActionButton(
                        label = "Cancel",
                        onClick = onCancel,
                        isPrimary = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferHistoryCard(transfer: Transfer) {
    var expanded by remember { mutableStateOf(false) }
    GrooveSurfaceCard(onClick = { expanded = !expanded }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GrooveCardTitle(transfer.deviceName)
            StatusBadge(status = transfer.overallStatus)
        }
        Spacer(modifier = Modifier.height(S_PADDING))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GrooveMutedText(
                text = formatBytes(transfer.totalBytes),
                style = MaterialTheme.typography.bodySmall,
            )
            GrooveMutedText(
                text = formatDate(transfer.endTime ?: transfer.startTime),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (expanded && transfer.files.isNotEmpty()) {
            Spacer(modifier = Modifier.height(S_PADDING))
            transfer.files.forEach { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = XS_PADDING / 2),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GrooveMutedText(
                        text = file.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    StatusBadge(status = file.status, small = true)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: TransferStatus, small: Boolean = false) {
    val label = when (status) {
        TransferStatus.PENDING -> "Pending"
        TransferStatus.CONNECTING -> "Connecting"
        TransferStatus.TRANSFERRING -> "Transferring"
        TransferStatus.PAUSED -> "Paused"
        TransferStatus.COMPLETED -> "Completed"
        TransferStatus.FAILED -> "Failed"
        TransferStatus.CANCELLED -> "Cancelled"
        TransferStatus.CHECKSUM_VALIDATING -> "Validating"
        TransferStatus.RETRYING -> "Retrying"
    }
    val tone = when (status) {
        TransferStatus.COMPLETED -> SoftWhite
        TransferStatus.FAILED, TransferStatus.CANCELLED -> SoftWhite.copy(alpha = 0.45f)
        else -> SoftWhite.copy(alpha = 0.75f)
    }
    Text(
        text = label,
        style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
        color = tone,
        modifier = Modifier
            .clip(RoundedCornerShape(XS_PADDING / 2))
            .padding(horizontal = XS_PADDING, vertical = 4.dp),
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
    return "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

private fun formatDate(date: Date): String {
    return SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(date)
}

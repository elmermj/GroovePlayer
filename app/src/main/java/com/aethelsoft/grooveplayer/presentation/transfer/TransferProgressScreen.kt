package com.aethelsoft.grooveplayer.presentation.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.domain.model.transfer.TransferFile
import com.aethelsoft.grooveplayer.domain.model.transfer.TransferStatus
import com.aethelsoft.grooveplayer.presentation.common.GrooveActionButton
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.GrooveSectionTitle
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftBlack
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

/** Sections for file transfer progress: In progress, In queue, Success, Failed */
private enum class FileSection {
    IN_PROGRESS,
    IN_QUEUE,
    SUCCESS,
    FAILED,
}

private fun TransferFile.section(): FileSection = when (status) {
    TransferStatus.TRANSFERRING,
    TransferStatus.CONNECTING,
    TransferStatus.RETRYING,
    TransferStatus.CHECKSUM_VALIDATING -> FileSection.IN_PROGRESS
    TransferStatus.PENDING,
    TransferStatus.PAUSED -> FileSection.IN_QUEUE
    TransferStatus.COMPLETED -> FileSection.SUCCESS
    TransferStatus.FAILED,
    TransferStatus.CANCELLED -> FileSection.FAILED
}

private fun groupFilesBySection(files: List<TransferFile>): Map<FileSection, List<TransferFile>> =
    files.groupBy { it.section() }

@Composable
fun TransferProgressScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransferProgressViewModel = hiltViewModel(),
) {
    val activeTransfers by viewModel.activeTransfers.collectAsState()
    var watchedTransferId by remember { mutableStateOf<Long?>(null) }
    var navigatedAway by remember { mutableStateOf(false) }

    // Latch onto the transfer being shown; when it leaves the active set
    // (completed / failed / cancelled), leave this screen automatically.
    LaunchedEffect(activeTransfers) {
        val current = activeTransfers.firstOrNull()
        val watched = watchedTransferId
        if (watched == null) {
            if (current != null) watchedTransferId = current.id
        } else if (!navigatedAway && activeTransfers.none { it.id == watched }) {
            navigatedAway = true
            onNavigateBack()
        }
    }

    GrooveScreen(
        title = "Transfer in progress",
        onBackClick = onNavigateBack,
    ) {
        if (activeTransfers.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GrooveMutedText(
                    text = "No active transfers",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            val transfer = activeTransfers.first()
            val groupedFiles = groupFilesBySection(transfer.files)

            Text(
                text = transfer.deviceName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(M_PADDING))
            LinearProgressIndicator(
                progress = { transfer.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(XS_PADDING),
                color = SoftWhite,
                trackColor = SoftBlack,
            )
            Spacer(modifier = Modifier.height(S_PADDING))
            GrooveMutedText(
                text = "${transfer.progressPercent}% • ${formatBytes(transfer.transferredBytes)} / ${formatBytes(transfer.totalBytes)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(M_PADDING))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                FileSectionContent(
                    title = "In progress",
                    files = groupedFiles[FileSection.IN_PROGRESS].orEmpty(),
                )
                FileSectionContent(
                    title = "In queue",
                    files = groupedFiles[FileSection.IN_QUEUE].orEmpty(),
                )
                FileSectionContent(
                    title = "Success",
                    files = groupedFiles[FileSection.SUCCESS].orEmpty(),
                )
                FileSectionContent(
                    title = "Failed",
                    files = groupedFiles[FileSection.FAILED].orEmpty(),
                )

                if (transfer.files.isEmpty() && transfer.totalBytes > 0) {
                    GrooveMutedText(
                        text = "Receiving ${formatBytes(transfer.totalBytes)}…",
                        modifier = Modifier.padding(top = S_PADDING),
                    )
                }
            }

            Spacer(modifier = Modifier.height(M_PADDING))
            GrooveActionButton(
                label = "Cancel",
                onClick = {
                    // Guard so the auto-back effect can't pop a second screen.
                    navigatedAway = true
                    viewModel.cancelTransfer(transfer.id)
                    onNavigateBack()
                },
                isPrimary = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FileSectionContent(
    title: String,
    files: List<TransferFile>,
) {
    if (files.isEmpty()) return
    GrooveSectionTitle(
        text = title,
        modifier = Modifier.padding(top = M_PADDING, bottom = S_PADDING),
    )
    files.forEach { file ->
        FileRow(file = file)
    }
}

@Composable
private fun FileRow(file: TransferFile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = XS_PADDING / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = file.fileName,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = when (file.status) {
                TransferStatus.TRANSFERRING,
                TransferStatus.CONNECTING,
                TransferStatus.RETRYING,
                TransferStatus.CHECKSUM_VALIDATING,
                TransferStatus.PENDING,
                TransferStatus.PAUSED -> "${file.progressPercent}%"
                TransferStatus.COMPLETED -> formatBytes(file.fileSize)
                TransferStatus.FAILED,
                TransferStatus.CANCELLED -> file.status.name.lowercase()
            },
            style = MaterialTheme.typography.bodySmall,
            color = SoftWhite.copy(alpha = 0.65f),
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
    return "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

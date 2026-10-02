package com.aethelworks.grooveplayer.presentation.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelworks.grooveplayer.R
import com.aethelworks.grooveplayer.domain.model.transfer.Transfer
import com.aethelworks.grooveplayer.domain.model.transfer.TransferStatus
import com.aethelworks.grooveplayer.presentation.common.FlatCentered
import com.aethelworks.grooveplayer.presentation.common.FlatProgress
import com.aethelworks.grooveplayer.presentation.common.FlatRow
import com.aethelworks.grooveplayer.presentation.common.GrooveScreen
import com.aethelworks.grooveplayer.presentation.common.OverlineLabel
import com.aethelworks.grooveplayer.presentation.common.flatValueStyle
import com.aethelworks.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelworks.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelworks.grooveplayer.presentation.common.topBarContentInset
import com.aethelworks.grooveplayer.utils.theme.icons.XMusic
import com.aethelworks.grooveplayer.utils.theme.ui.PoppinsFontFamily
import com.aethelworks.grooveplayer.utils.theme.ui.SoftWhite

@Composable
fun TransferStatusScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransferStatusViewModel = hiltViewModel(),
) {
    val activeTransfers by viewModel.activeTransfers.collectAsState()
    val transferHistory by viewModel.transferHistory.collectAsState()
    var clearedCompleted by rememberSaveable { mutableStateOf(false) }

    val waiting = activeTransfers.filter { it.overallStatus == TransferStatus.PENDING }
    val active = activeTransfers.filter { it.overallStatus != TransferStatus.PENDING }
    val history = if (clearedCompleted) {
        transferHistory.filter { it.overallStatus != TransferStatus.COMPLETED }
    } else {
        transferHistory
    }
    val hasCompleted = transferHistory.any { it.overallStatus == TransferStatus.COMPLETED }
    val empty = waiting.isEmpty() && active.isEmpty() && history.isEmpty()
    val clearLabel = stringResource(R.string.cd_transfer_clear_completed)

    GrooveScreen(
        title = "Transfer Status",
        onBackClick = onNavigateBack,
        centerTitle = true,
        flatBackdrop = true,
        contentPadding = PaddingValues(
            bottom = 12.dp + grooveBottomContentInset(includeMiniPlayer = rememberClearMiniPlayer()),
        ),
        actions = {
            if (hasCompleted && !clearedCompleted) {
                TextButton(
                    onClick = { clearedCompleted = true },
                    modifier = Modifier.semantics { contentDescription = clearLabel },
                    colors = ButtonDefaults.textButtonColors(contentColor = SoftWhite),
                ) {
                    Text(
                        text = stringResource(R.string.transfer_clear_completed),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
            }
        },
    ) {
        FlatCentered(Modifier.fillMaxSize()) {
            if (empty) {
                EmptyTransfers()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = topBarContentInset()),
                ) {
                    if (active.isNotEmpty()) {
                        item { OverlineLabel(stringResource(R.string.transfer_section_active)) }
                        items(active, key = { "active-${it.id}" }) { transfer ->
                            TransferBlock(
                                transfer = transfer,
                                showActions = true,
                                onPause = { viewModel.pauseTransfer(transfer.id) },
                                onResume = { viewModel.resumeTransfer(transfer.id) },
                                onCancel = { viewModel.cancelTransfer(transfer.id) },
                            )
                        }
                    }
                    if (waiting.isNotEmpty()) {
                        item { OverlineLabel(stringResource(R.string.transfer_waiting)) }
                        items(waiting, key = { "waiting-${it.id}" }) { transfer ->
                            TransferBlock(
                                transfer = transfer,
                                showActions = true,
                                onPause = { viewModel.pauseTransfer(transfer.id) },
                                onResume = { viewModel.resumeTransfer(transfer.id) },
                                onCancel = { viewModel.cancelTransfer(transfer.id) },
                            )
                        }
                    }
                    if (history.isNotEmpty()) {
                        item { OverlineLabel(stringResource(R.string.transfer_section_completed)) }
                        items(history, key = { "history-${it.id}" }) { transfer ->
                            TransferBlock(
                                transfer = transfer,
                                showActions = false,
                                onPause = {},
                                onResume = {},
                                onCancel = {},
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTransfers() {
    val description = stringResource(R.string.cd_transfer_empty)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = XMusic,
                contentDescription = null,
                tint = SoftWhite.copy(alpha = 0.80f),
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = stringResource(R.string.transfer_empty),
                color = SoftWhite.copy(alpha = 0.55f),
                fontFamily = PoppinsFontFamily,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clearAndSetSemantics {},
            )
        }
    }
}

@Composable
private fun TransferBlock(
    transfer: Transfer,
    showActions: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    if (transfer.files.isEmpty()) {
        TransferLine(
            title = transfer.deviceName,
            status = transfer.overallStatus,
            percent = transfer.progressPercent,
        )
    } else {
        transfer.files.forEach { file ->
            TransferLine(
                title = file.fileName,
                status = file.status,
                percent = file.progressPercent,
            )
        }
    }
    if (showActions) {
        TransferActions(
            status = transfer.overallStatus,
            onPause = onPause,
            onResume = onResume,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun TransferLine(
    title: String,
    status: TransferStatus,
    percent: Int,
) {
    val waiting = percent == 0 &&
        status != TransferStatus.COMPLETED &&
        status != TransferStatus.FAILED &&
        status != TransferStatus.CANCELLED
    val waitingDescription = stringResource(R.string.cd_transfer_waiting)
    Column(modifier = Modifier.fillMaxWidth()) {
        FlatRow(
            title = title,
            subtitle = statusLabel(status),
            titleMaxLines = 1,
            minHeight = 64.dp,
            leading = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SoftWhite.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = XMusic,
                        contentDescription = stringResource(R.string.cd_no_artwork),
                        tint = SoftWhite.copy(alpha = 0.80f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
            trailing = {
                Text(
                    text = if (waiting) stringResource(R.string.transfer_waiting) else "$percent%",
                    style = flatValueStyle(),
                    modifier = if (waiting) {
                        Modifier.semantics { contentDescription = waitingDescription }
                    } else {
                        Modifier
                    },
                )
            },
            showDivider = false,
        )
        FlatProgress(
            progress = percent / 100f,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun TransferActions(
    status: TransferStatus,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        when (status) {
            TransferStatus.TRANSFERRING, TransferStatus.CONNECTING, TransferStatus.RETRYING -> {
                FlatTextAction("Pause", onPause)
                FlatTextAction("Cancel", onCancel)
            }
            TransferStatus.PAUSED -> {
                FlatTextAction("Resume", onResume)
                FlatTextAction("Cancel", onCancel)
            }
            else -> FlatTextAction("Cancel", onCancel)
        }
    }
}

@Composable
private fun FlatTextAction(
    label: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = SoftWhite),
    ) {
        Text(
            text = label,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 390, name = "Transfer rows")
@Composable
private fun TransferLinePreview() {
    Column(Modifier.background(Color.Black)) {
        OverlineLabel("Active")
        TransferLine(
            title = "Midnight Drive",
            status = TransferStatus.TRANSFERRING,
            percent = 42,
        )
        OverlineLabel("Waiting")
        TransferLine(
            title = "Queued track",
            status = TransferStatus.PENDING,
            percent = 0,
        )
    }
}

private fun statusLabel(status: TransferStatus): String = when (status) {
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

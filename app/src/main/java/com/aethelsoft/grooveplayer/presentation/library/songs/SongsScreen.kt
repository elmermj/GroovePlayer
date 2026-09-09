package com.aethelsoft.grooveplayer.presentation.library.songs

import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.LocalBottomBarSecondaryContent
import com.aethelsoft.grooveplayer.presentation.common.rememberNavigationActions
import com.aethelsoft.grooveplayer.presentation.library.songs.layouts.LargeTabletSongsLayout
import com.aethelsoft.grooveplayer.presentation.library.songs.layouts.PhoneSongsLayout
import com.aethelsoft.grooveplayer.presentation.library.songs.layouts.TabletSongsLayout
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.rememberAudioPermissionState
import com.aethelsoft.grooveplayer.utils.rememberDeviceType
import com.aethelsoft.grooveplayer.utils.theme.icons.XClose
import com.aethelsoft.grooveplayer.utils.theme.icons.XNFC
import com.aethelsoft.grooveplayer.utils.theme.icons.XWifiSync
import com.aethelsoft.grooveplayer.utils.theme.ui.HighlightPrimary
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

private val SelectionBottomBarHeight = 140.dp

@Composable
fun SongsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SongsViewModel = hiltViewModel()
) {

    val (hasPermission, requestPermission) = rememberAudioPermissionState()
    val songsPagingItems: LazyPagingItems<Song> = viewModel.songsPagingFlow.collectAsLazyPagingItems()
    val deviceType = rememberDeviceType()
    val selectedSongForEdit by viewModel.selectedSongForEdit.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val navigation = rememberNavigationActions()

    val selectedSongs = remember(selectedIds, songsPagingItems.itemSnapshotList) {
        viewModel.getSelectedSongs(songsPagingItems.itemSnapshotList.filterNotNull())
    }

    val secondaryContentState = LocalBottomBarSecondaryContent.current
    SideEffect {
        secondaryContentState.value = if (isSelectionMode) {
            {
                SelectionBottomBar(
                    selectedCount = selectedIds.size,
                    onTapToShare = {
                        if (selectedIds.isNotEmpty()) {
                            navigation.openShareViaNfcWithSongs(selectedSongs)
                            viewModel.clearSelectionAndExit()
                        }
                    },
                    onShareWithNearby = {
                        if (selectedIds.isNotEmpty()) {
                            navigation.openShareViaNearbyWithSongs(selectedSongs)
                            viewModel.clearSelectionAndExit()
                        }
                    },
                    onCancel = { viewModel.exitSelectionMode() }
                )
            }
        } else null
    }
    // Ensure we revert to MiniPlayerBar immediately when selection mode ends
    LaunchedEffect(isSelectionMode) {
        if (!isSelectionMode) {
            secondaryContentState.value = null
        }
    }
    DisposableEffect(Unit) {
        onDispose { secondaryContentState.value = null }
    }

    val title = when {
        isSelectionMode && selectedIds.isNotEmpty() -> "${selectedIds.size} selected"
        isSelectionMode -> "Select songs"
        else -> "All Songs"
    }

    GrooveScreen(
        title = title,
        onBackClick = {
            if (isSelectionMode) {
                viewModel.exitSelectionMode()
            } else {
                onNavigateBack()
            }
        },
        contentPadding = PaddingValues.Zero,
        actions = {
            if (!isSelectionMode) {
                TextButton(onClick = { viewModel.enterSelectionMode() }) {
                    Text(text = "Select", color = SoftWhite)
                }
            }
        },
    ) {
        val layoutPadding = PaddingValues.Zero
        val horizontalPadding = M_PADDING
        if (!hasPermission) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Permission Required",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = S_PADDING),
                    )
                    GrooveMutedText(
                        text = "We need access to your music files",
                        modifier = Modifier.padding(bottom = M_PADDING),
                    )
                    Button(
                        onClick = requestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftWhite,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        } else {
            when (deviceType) {
                DeviceType.PHONE -> {
                    PhoneSongsLayout(
                        songsPagingItems = songsPagingItems,
                        paddingValues = layoutPadding,
                        horizontalPadding = horizontalPadding,
                        onEditSong = { viewModel.setSelectedSongForEdit(it) },
                        onLongPress = { viewModel.enterSelectionModeWithSong(it) },
                        isSelectionMode = isSelectionMode,
                        selectedIds = selectedIds,
                        onToggleSelection = { viewModel.toggleSelection(it) },
                        bottomPaddingForSelectionBar = SelectionBottomBarHeight
                    )
                }
                DeviceType.TABLET -> {
                    TabletSongsLayout(
                        songsPagingItems = songsPagingItems,
                        paddingValues = layoutPadding,
                        horizontalPadding = horizontalPadding,
                        onEditSong = { viewModel.setSelectedSongForEdit(it) },
                        onLongPress = { viewModel.enterSelectionModeWithSong(it) },
                        isSelectionMode = isSelectionMode,
                        selectedIds = selectedIds,
                        onToggleSelection = { viewModel.toggleSelection(it) },
                        bottomPaddingForSelectionBar = SelectionBottomBarHeight
                    )
                }
                DeviceType.LARGE_TABLET -> {
                    LargeTabletSongsLayout(
                        songsPagingItems = songsPagingItems,
                        paddingValues = layoutPadding,
                        horizontalPadding = horizontalPadding,
                        onEditSong = { viewModel.setSelectedSongForEdit(it) },
                        onLongPress = { viewModel.enterSelectionModeWithSong(it) },
                        isSelectionMode = isSelectionMode,
                        selectedIds = selectedIds,
                        onToggleSelection = { viewModel.toggleSelection(it) },
                        bottomPaddingForSelectionBar = SelectionBottomBarHeight
                    )
                }
            }
        }
    }

    // Edit metadata dialog
    selectedSongForEdit?.let { song ->
        EditSongMetadataDialog(
            song = song,
            onDismiss = { viewModel.setSelectedSongForEdit(null) },
            onSave = { updatedSong ->
                // TODO: Update song in list
                viewModel.setSelectedSongForEdit(null)
            }
        )
    }
}

@Composable
private fun SelectionBottomBar(
    selectedCount: Int,
    onTapToShare: () -> Unit,
    onShareWithNearby: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.3f * 1),
                        Color.Black.copy(alpha = 0.5f * 1),
                        Color.Black.copy(alpha = 0.65f * 1),
                        Color.Black.copy(alpha = 0.8f * 1),
                        Color.Black.copy(alpha = 0.9f * 1),
                        Color.Black.copy(alpha = 1f),
                    )
                )
            )
            .padding(
                top = S_PADDING,
                bottom = S_PADDING + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                start = M_PADDING,
                end = M_PADDING
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            SelectionBarOption(
                icon = XNFC,
                label = "Tap to share",
                onClick = onTapToShare,
                enabled = selectedCount > 0,
                modifier = Modifier.weight(1f)
            )
            SelectionBarOption(
                icon = XWifiSync,
                label = "Share with nearby",
                onClick = onShareWithNearby,
                enabled = selectedCount > 0,
                modifier = Modifier.weight(1f)
            )
            SelectionBarOption(
                icon = XClose,
                label = "Cancel",
                onClick = onCancel,
                enabled = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SelectionBarOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HighlightPrimary)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = M_PADDING, vertical = S_PADDING/2),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) SoftWhite else SoftWhite.copy(alpha = 0.5f),
            modifier = Modifier.size(M_PADDING)
        )
        Spacer(
            modifier = Modifier
                .height(4.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) SoftWhite else SoftWhite.copy(alpha = 0.5f),
            modifier = Modifier.basicMarquee(
                iterations = Int.MAX_VALUE,
                spacing = MarqueeSpacing(
                    spacing = 4.dp
                )
            )
        )
    }
}
package com.aethelsoft.grooveplayer.presentation.profile.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.domain.model.VisualizationMode
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.player.ui.CustomSlider
import com.aethelsoft.grooveplayer.presentation.player.ui.EqualizerControlsComponent
import com.aethelsoft.grooveplayer.presentation.profile.ProfileViewModel
import com.aethelsoft.grooveplayer.presentation.profile.ui.ActionType
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileSectionComponent
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileSettingRow
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileSettingsButton
import com.aethelsoft.grooveplayer.domain.model.FolderSizeEntry
import com.aethelsoft.grooveplayer.utils.APP_BAR_HEIGHT
import com.aethelsoft.grooveplayer.utils.StorageFormatUtils
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

@Composable
fun LargeTabletProfileLayout(
    viewModel: ProfileViewModel,
    onNavigateToShare: () -> Unit = {}
){

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = M_PADDING)
    ) {
        item {
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(420.dp)
                    .background(Color.Black)
                    .height(
                        APP_BAR_HEIGHT + WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + M_PADDING
                    )
            )
        }
        item {
            /** xdev
             * Account settings
             *
             * - User account (free, basic, premium) with basic and premium requires Google OAuth login
             * - Reset account
             */
            ProfileSectionComponent(
                sectionTitle = "Account",
            ) {
                ProfileSettingRow(
                    title = "Share Music",
                    subtitle = "Share via Tap (NFC) or nearby device",
                    actionType = ActionType.EXPANDABLE,
                    onClick = onNavigateToShare
                )
                Spacer(Modifier.height(S_PADDING))
                AccountSection(viewModel = viewModel)
            }
        }

        item {
            /** xdev
             * Playback settings
             *
             * - Repeat, shuffle, fade modes.
             * - Turn on/off MiniPlayerBar at start.
             * - Default mode for visualization.
             * - Equalizer preset and settings.
             */
            ProfileSectionComponent(
                sectionTitle = "Playback",
            ) {
                val activeRowId by viewModel.activeRowId.collectAsState()

                RepeatModeRow(
                    viewModel = viewModel,
                    isExpanded = activeRowId == "repeat",
                    onExpandedChange = { expanded ->
                        viewModel.setActiveRowId(if (expanded) "repeat" else null)
                    }
                )
                Spacer(Modifier.height(S_PADDING))

                ShuffleModeRow(
                    viewModel = viewModel,
                    isExpanded = activeRowId == "shuffle",
                    onExpandedChange = { expanded ->
                        viewModel.setActiveRowId(if (expanded) "shuffle" else null)
                    }
                )
                Spacer(Modifier.height(S_PADDING))

                CrossFadeModeRow(
                    viewModel = viewModel,
                    isExpanded = activeRowId == "fade",
                    onExpandedChange = { expanded ->
                        viewModel.setActiveRowId(if (expanded) "fade" else null)
                    }
                )
                Spacer(Modifier.height(S_PADDING))

                MiniPlayerOnStartRow(
                    viewModel = viewModel,
                    isExpanded = activeRowId == "mini_player",
                    onExpandedChange = { expanded ->
                        viewModel.setActiveRowId(if (expanded) "mini_player" else null)
                    }
                )
                Spacer(Modifier.height(S_PADDING))

                VisualizationModeRow(
                    viewModel = viewModel,
                    isExpanded = activeRowId == "visualization",
                    onExpandedChange = { expanded ->
                        viewModel.setActiveRowId(if (expanded) "visualization" else null)
                    }
                )
                Spacer(Modifier.height(S_PADDING))

                EqualizerRow(
                    isExpanded = activeRowId == "equalizer",
                    onExpandedChange = { expanded ->
                        viewModel.setActiveRowId(if (expanded) "equalizer" else null)
                    }
                )
            }
        }

        item {
            /** xdev
             * Storage settings
             *
             * - Excluded folders.
             * - Storage usage.
             * - A button for consolidating specific folders which contain music files into a specific folder.
             * - Clear cache.
             */
            ProfileSectionComponent(
                sectionTitle = "Storage",
            ) {
                val storageActiveRowId by viewModel.storageActiveRowId.collectAsState()
                ExcludedFoldersRow(
                    viewModel = viewModel,
                    isExpanded = storageActiveRowId == "excluded_folders",
                    onExpandedChange = { expanded ->
                        viewModel.setStorageActiveRowId(if (expanded) "excluded_folders" else null)
                    }
                )
                Spacer(Modifier.height(S_PADDING))
                StorageUsageRow(
                    viewModel = viewModel,
                    isExpanded = storageActiveRowId == "storage_usage",
                    onExpandedChange = { expanded ->
                        viewModel.setStorageActiveRowId(if (expanded) "storage_usage" else null)
                    }
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    title = "Consolidate music folders",
                    subtitle = "Move scattered music into a single location"
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    title = "Clear cache",
                    subtitle = "Remove temporary data"
                )
            }
        }

        item {
            /** xdev
             * About section
             *
             * - App version.
             * - App recent updates.
             * - App copyright.
             * - App privacy policy.
             */
            ProfileSectionComponent(
                sectionTitle = "About",
            ) {
                ProfileSettingRow(
                    title = "App version",
                    subtitle = "See current version"
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    title = "Recent updates",
                    subtitle = "What’s new in GroovePlayer"
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    title = "Copyright & licenses",
                    subtitle = "Legal information"
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    title = "Privacy policy",
                    subtitle = "How your data is handled"
                )
            }
        }
        item {
            Spacer(
                modifier = Modifier
                    .height(M_PADDING + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 240.dp)
                    .width(420.dp)
            )
        }
    }
}

@Composable
fun ExcludedFoldersRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    LaunchedEffect(isExpanded) {
        if (isExpanded) viewModel.loadFolderSuggestions()
    }
    ProfileSettingRow(
        title = "Excluded folders",
        subtitle = "Manage folders that are ignored during scanning",
        actionType = ActionType.EXPANDABLE,
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            ExcludedFoldersContent(viewModel = viewModel)
        }
    )
}

@Composable
private fun StorageUsageRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    LaunchedEffect(isExpanded) {
        if (isExpanded) viewModel.loadStorageUsage()
    }
    ProfileSettingRow(
        title = "Storage usage",
        subtitle = "View how much space music uses (included vs excluded)",
        actionType = ActionType.EXPANDABLE,
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            StorageUsageContent(viewModel = viewModel)
        }
    )
}

@Composable
private fun StorageUsageContent(viewModel: ProfileViewModel) {
    val storageUsage by viewModel.storageUsage.collectAsState()
    val isStorageLoading by viewModel.isStorageLoading.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(S_PADDING)
    ) {
        if (isStorageLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            Text(
                text = "Calculating storage…",
                style = MaterialTheme.typography.bodySmall,
                color = SoftWhite
            )
        } else {
            val data = storageUsage
            if (data == null) {
                Text(
                    text = "Could not load storage data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftWhite
                )
            } else {
                val total = data.totalBytes.coerceAtLeast(1L)
                val includedFraction = data.includedBytes.toFloat() / total

                Text(
                    text = "Total: ${StorageFormatUtils.formatBytes(data.totalBytes, total)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                LinearProgressIndicator(
                    progress = { includedFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color.White.copy(alpha = 0.9f),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Included: ${StorageFormatUtils.formatBytes(data.includedBytes, total)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftWhite
                    )
                    Text(
                        text = "Excluded: ${StorageFormatUtils.formatBytes(data.excludedBytes, total)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftWhite
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Included folders",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                StorageFolderBulletList(
                    entries = data.includedFolderDetails,
                    totalBytes = total
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Excluded folders",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                StorageFolderBulletList(
                    entries = data.excludedFolderDetails,
                    totalBytes = total
                )
            }
        }
    }
}

@Composable
private fun StorageFolderBulletList(
    entries: List<FolderSizeEntry>,
    totalBytes: Long,
) {
    if (entries.isEmpty()) {
        Text(
            text = "• None",
            style = MaterialTheme.typography.bodySmall,
            color = SoftWhite
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            entries.forEach { entry ->
                val displayName = entry.path.substringAfterLast('/', entry.path).ifEmpty { entry.path }
                Text(
                    text = "• $displayName — ${StorageFormatUtils.formatBytes(entry.bytes, totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftWhite,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ExcludedFoldersContent(viewModel: ProfileViewModel) {
    val folderSuggestions by viewModel.folderSuggestions.collectAsState()
    val excludedFolders by viewModel.excludedFolders.collectAsState()
    val suggestionsToShow = folderSuggestions.filter { it !in excludedFolders }
    val columnsCount = when {
        suggestionsToShow.isEmpty() -> 1
        suggestionsToShow.size == 1 -> 1
        suggestionsToShow.size == 2 -> 2
        else -> 3
    }
    val rows = suggestionsToShow.chunked(columnsCount)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(S_PADDING)
    ) {
        Text(
            text = "Suggestions",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
        if (rows.isEmpty()) {
            Text(
                text = "No folders with music found, or all are excluded.",
                style = MaterialTheme.typography.bodySmall,
                color = SoftWhite
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { path ->
                            Box(modifier = Modifier.weight(1f)) {
                                FolderSuggestionChip(
                                    path = path,
                                    onClick = { viewModel.excludeFolder(path) }
                                )
                            }
                        }
                        repeat(columnsCount - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Excluded",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
        if (excludedFolders.isEmpty()) {
            Text(
                text = "No excluded folders.",
                style = MaterialTheme.typography.bodySmall,
                color = SoftWhite
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                excludedFolders.forEach { path ->
                    ExcludedFolderItem(
                        path = path,
                        onInclude = { viewModel.includeFolder(path) }
                    )
                }
            }
        }
    }
}

@Composable
fun FolderSuggestionChip(
    path: String,
    onClick: () -> Unit,
) {
    val displayName = path.substringAfterLast('/', path).ifEmpty { path }
    Text(
        text = displayName,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

@Composable
private fun ExcludedFolderItem(
    path: String,
    onInclude: () -> Unit,
) {
    val displayName = path.substringAfterLast('/', path).ifEmpty { path }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                onClick = onInclude,
            )
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodySmall,
            color = SoftWhite,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AccountSection(viewModel: ProfileViewModel) {
    val profile by viewModel.userProfile.collectAsState()
    val tier = profile?.privilegeTier ?: com.aethelsoft.grooveplayer.domain.model.PrivilegeTier.FREE

    ProfileSettingRow(
        title = "Account type",
        subtitle = when (tier) {
            com.aethelsoft.grooveplayer.domain.model.PrivilegeTier.FREE -> "Free"
            com.aethelsoft.grooveplayer.domain.model.PrivilegeTier.BASIC -> "Basic"
            com.aethelsoft.grooveplayer.domain.model.PrivilegeTier.PREMIUM -> "Premium"
        }
    )
    Spacer(Modifier.height(S_PADDING))
    ProfileSettingsButton(
        onClick = {
            // Placeholder: in future replace with real Google OAuth.
            // For now, simply indicate that sign-in is not implemented.
        },
        modifier = Modifier.fillMaxWidth(),
        title = "Sign in with Google",
        isActive = false,
    )
    Spacer(Modifier.height(S_PADDING))
    ProfileSettingsButton(
        onClick = {
            // TODO: wire to a real "reset account" flow when available.
        },
        modifier = Modifier.fillMaxWidth(),
        title = "Reset account",
        isActive = false,
    )
}

@Composable
fun RepeatModeRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val playerViewModel = rememberPlayerViewModel()
    val currentRepeat by playerViewModel.repeat.collectAsState()

    ProfileSettingRow(
        actionType = ActionType.EXPANDABLE,
        title = "Repeat mode",
        subtitle = "Toggle repeat mode",
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            val modes = listOf(
                RepeatMode.OFF to "Off",
                RepeatMode.ALL to "All",
                RepeatMode.ONE to "One"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(S_PADDING)
            ) {
                modes.forEach { (mode, label) ->
                    val isActive = mode == currentRepeat
                    ProfileSettingsButton(
                        onClick = {
                            playerViewModel.setRepeat(mode)
                            onExpandedChange(false)
                        },
                        modifier = Modifier
                            .weight(1f),
                        title = label,
                        isActive = isActive
                    )
                }
            }
        }
    )
}

@Composable
fun ShuffleModeRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val playerViewModel = rememberPlayerViewModel()
    val isEnabled by playerViewModel.shuffle.collectAsState()

    ProfileSettingRow(
        actionType = ActionType.EXPANDABLE,
        title = "Shuffle mode",
        subtitle = "Toggle shuffle mode",
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(S_PADDING)
            ) {
                ProfileSettingsButton(
                    onClick = {
                        playerViewModel.setShuffle(false)
                        onExpandedChange(false)
                    },
                    modifier = Modifier.weight(1f),
                    title = "Off",
                    isActive = !isEnabled
                )
                ProfileSettingsButton(
                    onClick = {
                        playerViewModel.setShuffle(true)
                        onExpandedChange(false)
                    },
                    modifier = Modifier.weight(1f),
                    title = "On",
                    isActive = isEnabled
                )
            }
        }
    )
}

@Composable
fun CrossFadeModeRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val settings by viewModel.userSettings.collectAsState()
    val fadeSeconds = settings.fadeTimer.coerceIn(0, 10)
    var sliderValue = fadeSeconds.toFloat()

    ProfileSettingRow(
        actionType = ActionType.EXPANDABLE,
        title = "Cross-fade mode",
        subtitle = "Enable and set duration for smooth transitions between songs",
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(S_PADDING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomSlider(
                    value = sliderValue,
                    onValueChange = { value ->
                        sliderValue = value
                    },
                    onValueChangeFinished = {
                        viewModel.setFadeTimer(sliderValue.toInt())
                    },
                    valueRange = 0f..10f,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.3f)
                )
                Text(
                    text = "${sliderValue.toInt()} s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftWhite
                )
            }
        }
    )
}

@Composable
fun MiniPlayerOnStartRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val isEnabled by viewModel.isMiniPlayerOnStartEnabled.collectAsState()

    ProfileSettingRow(
        title = "Show mini player on app start",
        subtitle = "Toggle mini player visibility at launch",
        actionType = ActionType.EXPANDABLE,
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnabled) "Enabled" else "Disabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftWhite
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.setMiniPlayerOnStartEnabled(enabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.White.copy(alpha = 0.6f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }
    )
}

@Composable
fun VisualizationModeRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val settings by viewModel.userSettings.collectAsState()
    val mode = settings.visualizationMode

    ProfileSettingRow(
        actionType = ActionType.EXPANDABLE,
        title = "Default visualization mode",
        subtitle = "Choose how the visualizer looks by default",
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            val options = listOf(
                VisualizationMode.OFF to "Off",
                VisualizationMode.SIMULATED to "Simulated",
                VisualizationMode.REAL_TIME to "Real-time"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(S_PADDING)
            ) {
                options.forEach { (value, label) ->
                    val isActive = value == mode
                    ProfileSettingsButton(
                        onClick = {
                            viewModel.setVisualizationMode(value)
                            onExpandedChange(false)
                        },
                        modifier = Modifier.weight(1f, fill = false),
                        title = label,
                        isActive = isActive
                    )
                }
            }
        }
    )
}

@Composable
fun EqualizerRow(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    ProfileSettingRow(
        actionType = ActionType.EXPANDABLE,
        title = "Equalizer",
        subtitle = "Preset & advanced settings",
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            val consumeScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset = available
                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity
                    ): Velocity = available
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 1200.dp)
                    .nestedScroll(consumeScrollConnection)
            ) {
                EqualizerControlsComponent(
                    modifier = Modifier
                        .padding(0.dp)
                        .fillMaxWidth(),
                    isSimplified = true
                )
            }
        }
    )
}
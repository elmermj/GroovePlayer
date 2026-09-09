package com.aethelsoft.grooveplayer.presentation.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.FolderSizeEntry
import com.aethelsoft.grooveplayer.presentation.profile.ProfileViewModel
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.StorageFormatUtils
import com.aethelsoft.grooveplayer.utils.XS_PADDING
import com.aethelsoft.grooveplayer.utils.rememberDeviceType
import com.aethelsoft.grooveplayer.utils.theme.icons.XClearCache
import com.aethelsoft.grooveplayer.utils.theme.icons.XConsolidateFolders
import com.aethelsoft.grooveplayer.utils.theme.icons.XExcludedFolder
import com.aethelsoft.grooveplayer.utils.theme.icons.XStorageUsage
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

/**
 * Shared Storage section used by Phone, Tablet, and LargeTablet profile layouts.
 */
@Composable
fun ProfileStorageSection(viewModel: ProfileViewModel) {
    val storageActiveRowId by viewModel.storageActiveRowId.collectAsState()

    ProfileSectionComponent(sectionTitle = "Storage") {
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
        Spacer(modifier = Modifier.height(S_PADDING))
        ConsolidateFoldersRow(
            isExpanded = storageActiveRowId == "consolidate_folders",
            onExpandedChange = { expanded ->
                viewModel.setStorageActiveRowId(if (expanded) "consolidate_folders" else null)
            }
        )
        Spacer(modifier = Modifier.height(S_PADDING))
        ClearCacheRow(
            viewModel = viewModel,
            isExpanded = storageActiveRowId == "clear_cache",
            onExpandedChange = { expanded ->
                viewModel.setStorageActiveRowId(if (expanded) "clear_cache" else null)
            }
        )
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
        icon = { ProfileRowIcon(XExcludedFolder) },
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
fun StorageUsageRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    LaunchedEffect(isExpanded) {
        if (isExpanded) viewModel.loadStorageUsage()
    }
    ProfileSettingRow(
        icon = { ProfileRowIcon(XStorageUsage) },
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
fun ConsolidateFoldersRow(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    ProfileSettingRow(
        icon = { ProfileRowIcon(XConsolidateFolders) },
        title = "Consolidate music folders",
        subtitle = "Move scattered music into a single location",
        actionType = ActionType.EXPANDABLE,
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            Text(
                text = "Folder consolidation will gather music from multiple locations into one library folder. Coming in a future update.",
                style = MaterialTheme.typography.bodySmall,
                color = SoftWhite,
            )
        }
    )
}

@Composable
fun ClearCacheRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsState()
    val isClearingCache by viewModel.isClearingCache.collectAsState()

    LaunchedEffect(isExpanded) {
        if (isExpanded) viewModel.refreshCacheSize()
    }

    ProfileSettingRow(
        icon = { ProfileRowIcon(XClearCache) },
        title = "Clear cache",
        subtitle = "Remove temporary data",
        actionType = ActionType.EXPANDABLE,
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(S_PADDING),
            ) {
                Text(
                    text = if (isClearingCache) {
                        "Clearing…"
                    } else {
                        "Temporary files: ${formatCacheBytes(cacheSizeBytes)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftWhite,
                )
                ProfileSettingsButton(
                    onClick = { viewModel.clearAppCache() },
                    title = "Clear cache",
                    isActive = !isClearingCache && cacheSizeBytes > 0L,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
                    .clip(RoundedCornerShape(2.dp)),
                color = SoftWhite,
                trackColor = SoftWhite.copy(alpha = 0.2f),
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
                        .height(XS_PADDING)
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
                Spacer(modifier = Modifier.height(XS_PADDING))
                Text(
                    text = "Included folders",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                StorageFolderBulletList(
                    entries = data.includedFolderDetails,
                    totalBytes = total
                )
                Spacer(modifier = Modifier.height(4.dp))
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
    val deviceType = rememberDeviceType()
    val suggestionsToShow = folderSuggestions.filter { it !in excludedFolders }
    val maxColumns = when (deviceType) {
        DeviceType.PHONE -> 2
        DeviceType.TABLET -> 2
        DeviceType.LARGE_TABLET -> 3
    }
    val columnsCount = when {
        suggestionsToShow.isEmpty() -> 1
        suggestionsToShow.size == 1 -> 1
        suggestionsToShow.size < maxColumns -> suggestionsToShow.size
        else -> maxColumns
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
            Column(verticalArrangement = Arrangement.spacedBy(XS_PADDING)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(XS_PADDING)
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
        Spacer(modifier = Modifier.height(XS_PADDING))
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
            .clip(RoundedCornerShape(XS_PADDING))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = S_PADDING, vertical = 10.dp)
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
            .clip(RoundedCornerShape(XS_PADDING))
            .clickable(onClick = onInclude)
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = S_PADDING, vertical = XS_PADDING),
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

private fun formatCacheBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
    return StorageFormatUtils.formatBytes(bytes, bytes.coerceAtLeast(1L))
}

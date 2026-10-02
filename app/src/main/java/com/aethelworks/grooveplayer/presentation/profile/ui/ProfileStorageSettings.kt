package com.aethelworks.grooveplayer.presentation.profile.ui

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
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
import com.aethelworks.grooveplayer.domain.model.FolderSizeEntry
import com.aethelworks.grooveplayer.presentation.library.importing.LocalLibraryImport
import com.aethelworks.grooveplayer.presentation.profile.ProfileViewModel
import com.aethelworks.grooveplayer.utils.DeviceType
import com.aethelworks.grooveplayer.utils.S_PADDING
import com.aethelworks.grooveplayer.utils.StorageFormatUtils
import com.aethelworks.grooveplayer.utils.XS_PADDING
import com.aethelworks.grooveplayer.utils.rememberDeviceType
import com.aethelworks.grooveplayer.utils.theme.icons.XClearCache
import com.aethelworks.grooveplayer.utils.theme.icons.XConsolidateFolders
import com.aethelworks.grooveplayer.utils.theme.icons.XExcludedFolder
import com.aethelworks.grooveplayer.utils.theme.icons.XStorageUsage
import com.aethelworks.grooveplayer.utils.theme.ui.SoftWhite

/**
 * Shared Storage section used by Phone, Tablet, and LargeTablet profile layouts.
 */
@Composable
fun ProfileStorageSection(viewModel: ProfileViewModel) {
    val storageActiveRowId by viewModel.storageActiveRowId.collectAsState()

    ProfileSectionComponent(sectionTitle = stringResource(R.string.settings_storage)) {
        ImportFolderRow()
        StorageUsageRow(
            viewModel = viewModel,
            isExpanded = storageActiveRowId == "storage_usage",
            onExpandedChange = { expanded ->
                viewModel.setStorageActiveRowId(if (expanded) "storage_usage" else null)
            }
        )
        ConsolidateFoldersRow(
            isExpanded = storageActiveRowId == "consolidate_folders",
            onExpandedChange = { expanded ->
                viewModel.setStorageActiveRowId(if (expanded) "consolidate_folders" else null)
            }
        )
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
private fun ImportFolderRow() {
    val import = LocalLibraryImport.current
    ProfileSettingRow(
        icon = { SettingsRowIcon(XExcludedFolder) },
        title = stringResource(R.string.library_import_folder),
        subtitle = stringResource(R.string.storage_import_sub),
        actionType = ActionType.LINK,
        onClick = import.pickFolder,
    )
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
        icon = { SettingsRowIcon(XExcludedFolder) },
        title = stringResource(R.string.storage_excluded_title),
        subtitle = stringResource(R.string.storage_excluded_sub),
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
        icon = { SettingsRowIcon(XStorageUsage) },
        title = stringResource(R.string.storage_usage),
        subtitle = stringResource(R.string.storage_usage_sub),
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
        icon = { SettingsRowIcon(XConsolidateFolders) },
        title = stringResource(R.string.storage_consolidate),
        subtitle = stringResource(R.string.storage_consolidate_sub),
        actionType = ActionType.EXPANDABLE,
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            Text(
                text = stringResource(R.string.storage_consolidate_body),
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
        icon = { SettingsRowIcon(XClearCache) },
        title = stringResource(R.string.storage_clear_cache),
        subtitle = stringResource(R.string.storage_clear_cache_sub),
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
                        stringResource(R.string.storage_clearing)
                    } else {
                        stringResource(R.string.storage_temp_files, formatCacheBytes(cacheSizeBytes))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftWhite,
                )
                ProfileSettingsButton(
                    onClick = { viewModel.clearAppCache() },
                    title = stringResource(R.string.storage_clear_cache),
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
                text = stringResource(R.string.storage_calculating),
                style = MaterialTheme.typography.bodySmall,
                color = SoftWhite
            )
        } else {
            val data = storageUsage
            if (data == null) {
                Text(
                    text = stringResource(R.string.storage_load_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftWhite
                )
            } else {
                val total = data.totalBytes.coerceAtLeast(1L)
                val includedFraction = data.includedBytes.toFloat() / total

                Text(
                    text = stringResource(R.string.storage_total, StorageFormatUtils.formatBytes(data.totalBytes, total)),
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
                        text = stringResource(R.string.storage_included, StorageFormatUtils.formatBytes(data.includedBytes, total)),
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftWhite
                    )
                    Text(
                        text = stringResource(R.string.storage_excluded_bytes, StorageFormatUtils.formatBytes(data.excludedBytes, total)),
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftWhite
                    )
                }
                Spacer(modifier = Modifier.height(XS_PADDING))
                Text(
                    text = stringResource(R.string.storage_included_folders),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                StorageFolderBulletList(
                    entries = data.includedFolderDetails,
                    totalBytes = total
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.storage_excluded_folders),
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
            text = stringResource(R.string.storage_none),
            style = MaterialTheme.typography.bodySmall,
            color = SoftWhite
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            entries.forEach { entry ->
                val displayName = entry.path.substringAfterLast('/', entry.path).ifEmpty { entry.path }
                Text(
                    text = stringResource(R.string.storage_folder_line, displayName, StorageFormatUtils.formatBytes(entry.bytes, totalBytes)),
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
            text = stringResource(R.string.storage_suggestions),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.storage_no_suggestions),
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
            text = stringResource(R.string.storage_excluded_heading),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
        if (excludedFolders.isEmpty()) {
            Text(
                text = stringResource(R.string.storage_no_excluded),
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

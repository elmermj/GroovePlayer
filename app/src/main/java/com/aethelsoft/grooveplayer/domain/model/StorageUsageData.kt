package com.aethelsoft.grooveplayer.domain.model

/**
 * Storage usage breakdown for music folders: included vs excluded.
 */
data class StorageUsageData(
    val totalBytes: Long,
    val includedBytes: Long,
    val excludedBytes: Long,
    val includedFolderDetails: List<FolderSizeEntry> = emptyList(),
    val excludedFolderDetails: List<FolderSizeEntry> = emptyList()
)

data class FolderSizeEntry(
    val path: String,
    val bytes: Long
)

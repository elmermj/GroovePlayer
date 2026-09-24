package com.aethelsoft.grooveplayer.presentation.backup

import com.aethelsoft.grooveplayer.domain.model.BackupObject

enum class BackupObjectsFilter(val label: String) {
    ALL("All"),
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    LARGEST("Largest first"),
    SMALLEST("Smallest first"),
    NAME("Name A–Z"),
    ;

    fun apply(objects: List<BackupObject>): List<BackupObject> = when (this) {
        ALL -> objects
        NEWEST -> objects.sortedByDescending { it.createdAtIso.orEmpty() }
        OLDEST -> objects.sortedBy { it.createdAtIso.orEmpty() }
        LARGEST -> objects.sortedByDescending { it.sizeBytes }
        SMALLEST -> objects.sortedBy { it.sizeBytes }
        NAME -> objects.sortedBy { displayName(it).lowercase() }
    }

    companion object {
        fun displayName(obj: BackupObject): String =
            obj.logicalPath?.substringAfterLast('/')
                ?: obj.r2Key.substringAfterLast('/').ifBlank { obj.id }
    }
}

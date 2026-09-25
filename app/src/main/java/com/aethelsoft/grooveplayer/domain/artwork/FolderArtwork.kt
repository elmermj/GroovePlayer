package com.aethelsoft.grooveplayer.domain.artwork

import java.io.File

/** `cover.jpg`, then `folder.jpg`, in the same directory as the audio file. */
object FolderArtwork {
    val FILE_NAMES = listOf("cover.jpg", "folder.jpg")

    fun find(directory: File?): File? {
        if (directory == null || !directory.isDirectory) return null
        val children = directory.listFiles() ?: return null
        for (name in FILE_NAMES) {
            children.firstOrNull { file ->
                file.isFile && file.length() > 0L && file.name.equals(name, ignoreCase = true)
            }?.let { return it }
        }
        return null
    }
}

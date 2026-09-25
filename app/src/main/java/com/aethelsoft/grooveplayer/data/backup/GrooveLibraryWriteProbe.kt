package com.aethelsoft.grooveplayer.data.backup

import java.io.File
import java.io.FileOutputStream

/**
 * [java.io.File.canWrite] is not enough on scoped storage. MediaProvider-owned
 * directories report writable and then reject `*.tmp` creates with EPERM.
 */
internal object GrooveLibraryWriteProbe {
    fun canCreateFile(dir: File): Boolean {
        if (!dir.exists() && !dir.mkdirs()) return false
        if (!dir.isDirectory) return false
        val probe = File(dir, ".groove_write_${System.nanoTime()}.tmp")
        return try {
            FileOutputStream(probe).use { out ->
                out.write(byteArrayOf(0))
                out.fd.sync()
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            runCatching { probe.delete() }
        }
    }
}

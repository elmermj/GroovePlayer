package com.aethelsoft.grooveplayer.domain.backup

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/** SHA-256 of file bytes. Backup identity is this hash plus size; filenames are cosmetic. */
object ContentHash {
    const val PROGRESS_STEP_BYTES = 256 * 1024

    fun sha256(
        file: File,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val total = file.length().coerceAtLeast(0L)
        onProgress?.invoke(0L, total)
        var readTotal = 0L
        var sinceEmit = 0L
        FileInputStream(file).use { input ->
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buf)
                if (read < 0) break
                digest.update(buf, 0, read)
                readTotal += read
                sinceEmit += read
                if (onProgress != null && sinceEmit >= PROGRESS_STEP_BYTES) {
                    onProgress(readTotal, total)
                    sinceEmit = 0L
                }
            }
        }
        if (onProgress != null && sinceEmit > 0L) {
            onProgress(readTotal, total)
        }
        return digest.digest().joinToString("") { b -> "%02x".format(b) }
    }
}

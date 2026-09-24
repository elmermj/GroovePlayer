package com.aethelsoft.grooveplayer.domain.backup

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/** SHA-256 of file bytes. Backup identity is this hash plus size; filenames are cosmetic. */
object ContentHash {
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buf)
                if (read < 0) break
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { b -> "%02x".format(b) }
    }
}

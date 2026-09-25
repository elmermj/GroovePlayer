package com.aethelsoft.grooveplayer.domain.artwork

import java.io.File

/**
 * Downsampled artwork bytes, keyed by [EmbeddedArtworkKeys.cacheKey].
 * Memory is an LRU; disk keeps the same bytes so a later process does not
 * open the audio file again. A changed content hash is a different key.
 */
class EmbeddedArtworkCache(
    private val directory: File,
    private val memoryCapacity: Int = 32,
) {
    private val lock = Any()
    private val memory = LinkedHashMap<String, ByteArray>(memoryCapacity, 0.75f, true)

    fun read(key: String): ByteArray? {
        synchronized(lock) {
            memory[key]?.let { return it.copyOf() }
        }
        val file = file(key)
        if (!file.isFile || file.length() <= 0L) return null
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        if (bytes.isEmpty()) return null
        remember(key, bytes)
        return bytes.copyOf()
    }

    fun write(key: String, bytes: ByteArray) {
        if (key.isBlank() || bytes.isEmpty()) return
        val dest = file(key)
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, "${dest.name}.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(dest)) {
            dest.writeBytes(bytes)
            tmp.delete()
        }
        remember(key, bytes)
    }

    fun file(key: String): File = File(directory, EmbeddedArtworkKeys.fileName(key))

    private fun remember(key: String, bytes: ByteArray) {
        synchronized(lock) {
            memory[key] = bytes.copyOf()
            while (memory.size > memoryCapacity) {
                val eldest = memory.entries.iterator().next().key
                memory.remove(eldest)
            }
        }
    }
}

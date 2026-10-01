package com.aethelworks.grooveplayer.data.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends file data in chunks using FileChannel for large file support (2GB+).
 * Uses configurable chunk size (default 64KB) and supports resume via offset.
 */
@Singleton
class FileChunkSender @Inject constructor() {

    /**
     * SHA-256 of the file about to be sent. Uses [TransferProtocol.metadataFor],
     * which hashes through the private-library [com.aethelworks.grooveplayer.domain.backup.ContentHash].
     */
    suspend fun computeChecksum(filePath: String): String = withContext(Dispatchers.IO) {
        TransferProtocol.metadataFor(File(filePath)).checksum
    }

    /**
     * Read a chunk from file at given offset.
     * Uses FileChannel.position(offset) for large file support.
     */
    suspend fun readChunk(
        filePath: String,
        offset: Long,
        chunkSize: Int = TransferProtocol.DEFAULT_CHUNK_SIZE,
    ): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext null
        RandomAccessFile(filePath, "r").use { raf ->
            val channel: FileChannel = raf.channel
            val size = channel.size()
            if (offset >= size) return@withContext null
            val toRead = minOf(chunkSize.toLong(), size - offset).toInt()
            val buffer = ByteBuffer.allocate(toRead)
            channel.position(offset)
            channel.read(buffer)
            buffer.flip()
            val result = ByteArray(buffer.remaining())
            buffer.get(result)
            result
        }
    }

    /**
     * Get file size.
     */
    suspend fun getFileSize(filePath: String): Long = withContext(Dispatchers.IO) {
        File(filePath).length()
    }
}

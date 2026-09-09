package com.aethelsoft.grooveplayer.data.transfer

import android.content.Context
import android.util.Log
import com.aethelsoft.grooveplayer.utils.helpers.logShareNearbyP2PTag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Receives file chunks and writes using RandomAccessFile.seek(offset) for resume support.
 * Validates checksum after completion.
 */
@Singleton
class FileChunkReceiver @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val tag = logShareNearbyP2PTag(context)
    /**
     * Write chunk to file at given offset.
     * Uses RandomAccessFile.seek(offset) for partial file support.
     */
    suspend fun writeChunk(
        filePath: String,
        offset: Long,
        data: ByteArray,
    ): Unit = withContext(Dispatchers.IO) {
        Log.d(tag, "Writing ${data.size} bytes to $filePath at offset=$offset")
        File(filePath).parentFile?.mkdirs()
        RandomAccessFile(filePath, "rw").use { raf ->
            raf.seek(offset)
            raf.write(data)
        }
    }

    /**
     * Validate file checksum after transfer.
     */
    suspend fun validateChecksum(filePath: String, expectedChecksum: String): Boolean =
        withContext(Dispatchers.IO) {
            val digest = MessageDigest.getInstance("SHA-256")
            RandomAccessFile(filePath, "r").use { raf ->
                val buffer = ByteArray(TransferProtocol.DEFAULT_CHUNK_SIZE)
                var read: Int
                while (raf.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            actual.equals(expectedChecksum, ignoreCase = true)
        }

    /**
     * Get current file size (for resume - already transferred bytes).
     */
    suspend fun getCurrentSize(filePath: String): Long = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) file.length() else 0L
    }
}

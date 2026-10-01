package com.aethelworks.grooveplayer.data.transfer

import com.aethelworks.grooveplayer.domain.backup.ContentHash
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * One file announced before its chunks. [checksum] is the SHA-256 from [ContentHash].
 */
data class TransferFileMeta(
    val name: String,
    val sizeBytes: Long,
    val checksum: String,
)

/**
 * Transfer protocol constants for Nearby P2P file transfer.
 * Defines message types, chunk sizes, and retry limits.
 */
object TransferProtocol {

    /** Service ID for Nearby Connections (use app package for uniqueness) */
    const val SERVICE_ID = "com.aethelworks.grooveplayer.transfer"

    /** Default chunk size for streaming (64KB - configurable) */
    const val DEFAULT_CHUNK_SIZE = 64 * 1024

    /** Max retries per file before marking as FAILED */
    const val MAX_RETRIES_PER_FILE = 3

    /**
     * Max chunk payloads queued in Nearby without a delivery confirmation.
     * Keeps sender progress honest and prevents Play Services queue bloat
     * (sendPayload only enqueues; delivery is confirmed via onPayloadTransferUpdate).
     */
    const val MAX_IN_FLIGHT_CHUNKS = 16

    /** If no queued payload gets delivered within this window, the transfer is stalled. */
    const val SEND_STALL_TIMEOUT_MS = 60_000L

    /** Message type: sender sends file metadata to receiver */
    const val MSG_FILE_METADATA = 0x01

    /** Message type: receiver responds with offset (resume support) */
    const val MSG_OFFSET_RESPONSE = 0x02

    /** Message type: chunk of file data */
    const val MSG_CHUNK = 0x03

    /** Message type: transfer complete */
    const val MSG_COMPLETE = 0x04

    /** Message type: transfer cancelled */
    const val MSG_CANCEL = 0x05

    /** Message type: pause request */
    const val MSG_PAUSE = 0x06

    /** Message type: resume request */
    const val MSG_RESUME = 0x07

    /**
     * Sender metadata for [file]. The checksum is [ContentHash.sha256] of the bytes
     * that will be transferred, not a hash of the name.
     */
    fun metadataFor(file: File): TransferFileMeta {
        val name = file.name.ifBlank { "audio.bin" }
        return TransferFileMeta(
            name = name,
            sizeBytes = file.length(),
            checksum = ContentHash.sha256(file),
        )
    }

    /**
     * MSG_FILE_METADATA layout:
     * type, totalBytes, fileCount, then for each file: name, size, SHA-256 hex.
     */
    fun encodeFileMetadata(files: List<TransferFileMeta>): ByteArray {
        val names = files.map { it.name.toByteArray(Charsets.UTF_8) }
        val sums = files.map { it.checksum.toByteArray(Charsets.UTF_8) }
        var size = 1 + 8 + 4
        for (i in files.indices) {
            size += 2 + names[i].size + 8 + 2 + sums[i].size
        }
        val message = ByteArray(size)
        var pos = 0
        message[pos++] = MSG_FILE_METADATA.toByte()
        val totalBytes = files.sumOf { it.sizeBytes }
        ByteBuffer.wrap(message, pos, 8).order(ByteOrder.BIG_ENDIAN).putLong(totalBytes)
        pos += 8
        ByteBuffer.wrap(message, pos, 4).order(ByteOrder.BIG_ENDIAN).putInt(files.size)
        pos += 4
        for (i in files.indices) {
            val name = names[i]
            ByteBuffer.wrap(message, pos, 2).order(ByteOrder.BIG_ENDIAN).putShort(name.size.toShort())
            pos += 2
            System.arraycopy(name, 0, message, pos, name.size)
            pos += name.size
            ByteBuffer.wrap(message, pos, 8).order(ByteOrder.BIG_ENDIAN).putLong(files[i].sizeBytes)
            pos += 8
            val sum = sums[i]
            ByteBuffer.wrap(message, pos, 2).order(ByteOrder.BIG_ENDIAN).putShort(sum.size.toShort())
            pos += 2
            System.arraycopy(sum, 0, message, pos, sum.size)
            pos += sum.size
        }
        return message
    }

    /** Null when the message is truncated or a file has no SHA-256. */
    fun decodeFileMetadata(message: ByteArray): DecodedFileMetadata? {
        if (message.size < 13) return null
        if ((message[0].toInt() and 0xFF) != MSG_FILE_METADATA) return null
        val totalBytes = ByteBuffer.wrap(message, 1, 8).order(ByteOrder.BIG_ENDIAN).long
        val fileCount = ByteBuffer.wrap(message, 9, 4).order(ByteOrder.BIG_ENDIAN).int
        if (fileCount < 0 || fileCount > 10_000) return null
        var pos = 13
        val files = ArrayList<TransferFileMeta>(fileCount)
        var summed = 0L
        for (i in 0 until fileCount) {
            if (pos + 2 > message.size) return null
            val nameLen = ByteBuffer.wrap(message, pos, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
            pos += 2
            if (nameLen <= 0 || pos + nameLen + 8 + 2 > message.size) return null
            val name = message.copyOfRange(pos, pos + nameLen).toString(Charsets.UTF_8)
            pos += nameLen
            val fileSize = ByteBuffer.wrap(message, pos, 8).order(ByteOrder.BIG_ENDIAN).long
            pos += 8
            val sumLen = ByteBuffer.wrap(message, pos, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
            pos += 2
            if (sumLen != 64 || pos + sumLen > message.size) return null
            val checksum = message.copyOfRange(pos, pos + sumLen).toString(Charsets.UTF_8)
            pos += sumLen
            if (name.isBlank() || checksum.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
                return null
            }
            files += TransferFileMeta(name, fileSize, checksum)
            summed += fileSize
        }
        if (pos != message.size || summed != totalBytes) return null
        return DecodedFileMetadata(totalBytes, files)
    }
}

data class DecodedFileMetadata(
    val totalBytes: Long,
    val files: List<TransferFileMeta>,
)

package com.aethelsoft.grooveplayer.data.transfer

/**
 * Transfer protocol constants for Nearby P2P file transfer.
 * Defines message types, chunk sizes, and retry limits.
 */
object TransferProtocol {

    /** Service ID for Nearby Connections (use app package for uniqueness) */
    const val SERVICE_ID = "com.aethelsoft.grooveplayer.transfer"

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
}

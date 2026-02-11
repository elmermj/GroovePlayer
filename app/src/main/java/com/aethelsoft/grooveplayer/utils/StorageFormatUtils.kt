package com.aethelsoft.grooveplayer.utils

/**
 * Formats bytes for display. Uses MB when total < 1GB, then GB, TB, PB.
 * Decimals: 1 digit → 2 decimals, 2 digits → 1 decimal, 3+ digits → 0 decimals.
 */
object StorageFormatUtils {
    private const val KB = 1024L
    private const val MB = KB * 1024
    private const val GB = MB * 1024
    private const val TB = GB * 1024
    private const val PB = TB * 1024

    /**
     * Formats [bytes] using the unit appropriate for [referenceTotal].
     * If referenceTotal < 1GB, values are shown in MB; otherwise GB, TB, or PB.
     */
    fun formatBytes(bytes: Long, referenceTotal: Long): String {
        val (unit, divisor, unitName) = when {
            referenceTotal < GB -> Triple(MB.toDouble(), MB.toDouble(), "MB")
            referenceTotal < TB -> Triple(GB.toDouble(), GB.toDouble(), "GB")
            referenceTotal < PB -> Triple(TB.toDouble(), TB.toDouble(), "TB")
            else -> Triple(PB.toDouble(), PB.toDouble(), "PB")
        }
        val value = bytes / divisor
        val intPart = value.toLong()
        val digitCount = when {
            intPart == 0L -> 1
            else -> intPart.toString().length
        }
        val decimals = when (digitCount) {
            1 -> 2
            2 -> 1
            else -> 0
        }
        val format = when (decimals) {
            0 -> "%.0f"
            1 -> "%.1f"
            else -> "%.2f"
        }
        return "${format.format(value)} $unitName"
    }
}

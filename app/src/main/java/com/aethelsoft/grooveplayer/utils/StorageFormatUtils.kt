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
        val (divisor, unitName) = unitFor(referenceTotal)
        val value = bytes / divisor
        return "${formatValue(value)} $unitName"
    }

    /**
     * Compact quota label for the arc meter center, e.g. `41.1/60 GB`
     * (used number without unit; shared unit after the slash).
     */
    fun formatQuotaPair(usedBytes: Long, quotaBytes: Long): Pair<String, String> {
        val total = quotaBytes.coerceAtLeast(1L)
        val (divisor, unitName) = unitFor(total)
        val usedLabel = formatValue(usedBytes.coerceAtLeast(0L) / divisor)
        val quotaLabel = formatValue(total / divisor)
        return usedLabel to "$quotaLabel $unitName"
    }

    private fun unitFor(referenceTotal: Long): Pair<Double, String> = when {
        referenceTotal < GB -> MB.toDouble() to "MB"
        referenceTotal < TB -> GB.toDouble() to "GB"
        referenceTotal < PB -> TB.toDouble() to "TB"
        else -> PB.toDouble() to "PB"
    }

    private fun formatValue(value: Double): String {
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
        return format.format(value)
    }
}

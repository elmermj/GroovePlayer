package com.aethelsoft.grooveplayer.domain.playlist

object PlaylistOrder {
    /**
     * Moves the item at [fromIndex] so it lands at [toIndex].
     * Returns the original list when either index is out of range or unchanged.
     */
    fun <T> move(items: List<T>, fromIndex: Int, toIndex: Int): List<T> {
        if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) {
            return items
        }
        val updated = items.toMutableList()
        val item = updated.removeAt(fromIndex)
        updated.add(toIndex, item)
        return updated
    }
}

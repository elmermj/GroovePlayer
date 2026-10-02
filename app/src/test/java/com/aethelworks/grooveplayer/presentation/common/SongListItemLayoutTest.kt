package com.aethelworks.grooveplayer.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongListItemLayoutTest {
    @Test
    fun nowPlayingReservesTheUpNextDragSlot() {
        assertEquals(
            SongListSlots.UpNext.trailingWidthDp(),
            SongListSlots.NowPlaying.trailingWidthDp(),
            0f,
        )
        assertEquals(
            SongListItemDefaults.dragSlot.value,
            SongListSlots.NowPlaying.trailingWidthDp(),
            0f,
        )
        assertTrue(SongListSlots.NowPlaying.reserveDrag)
        assertTrue(SongListSlots.UpNext.drag)
    }

    @Test
    fun playCountSlotIsFixedForOnePlayAndTwoPlays() {
        val mostPlayed = SongListSlots.MostPlayed.trailingWidthDp()
        val expected = SongListItemDefaults.heartSlot.value +
            SongListItemDefaults.playCountSlot.value +
            SongListItemDefaults.menuSlot.value
        assertEquals(expected, mostPlayed, 0f)
        assertEquals(mostPlayed, SongListSlots.MostPlayed.copy().trailingWidthDp(), 0f)
        assertTrue(SongListItemDefaults.playCountSlot.value >= SongListItemDefaults.playCountReserve.length * 8f)
        assertEquals("99 plays", SongListItemDefaults.playCountReserve)
        assertTrue(!SongListSlots.MostPlayed.duration)
        assertTrue(SongListSlots.Library.duration)
        assertTrue(!SongListSlots.Library.playCount)
    }

    @Test
    fun selectionReplacesTheMenuSlotWithoutChangingWidth() {
        val library = SongListSlots.Library
        val selecting = library.copy(menu = false, selection = true)
        assertEquals(library.trailingWidthDp(), selecting.trailingWidthDp(), 0f)
        assertEquals(
            SongListItemDefaults.menuSlot.value,
            SongListItemDefaults.selectionSlot.value,
            0f,
        )
    }

    @Test
    fun cardAndThumbnailUseOneSize() {
        assertEquals(16f, SongListItemDefaults.cornerRadius.value, 0f)
        assertEquals(10f, SongListItemDefaults.thumbnailCornerRadius.value, 0f)
        assertEquals(48f, SongListItemDefaults.thumbnailSize.value, 0f)
        assertEquals(72f, SongListItemDefaults.minHeight.value, 0f)
        assertEquals(8f, SongListItemDefaults.rowSpacing.value, 0f)
        assertEquals(12f, SongListItemDefaults.horizontalPadding.value, 0f)
        assertEquals(12f, SongListItemDefaults.verticalPadding.value, 0f)
    }
}

package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.aethelsoft.grooveplayer.presentation.player.PlayerSongDetailsSheetState
import kotlin.math.abs

/**
 * Detects an upward swipe on player chrome (excluding artwork hit targets).
 *
 * - [PlayerSongDetailsSheetState.Hidden] → peek
 * - [PlayerSongDetailsSheetState.Peek] → expanded (swipe up again)
 */
fun Modifier.detectPullUpToSongDetails(
    enabled: Boolean,
    sheetState: PlayerSongDetailsSheetState,
    onOpenPeek: () -> Unit,
    onExpandDetails: () -> Unit,
): Modifier = pointerInput(enabled, sheetState) {
    if (!enabled) return@pointerInput
    var totalX = 0f
    var totalY = 0f
    detectDragGestures(
        onDragStart = {
            totalX = 0f
            totalY = 0f
        },
        onDragEnd = {
            if (abs(totalY) > abs(totalX) && totalY < -64f) {
                when (sheetState) {
                    PlayerSongDetailsSheetState.Hidden -> onOpenPeek()
                    PlayerSongDetailsSheetState.Peek -> onExpandDetails()
                    PlayerSongDetailsSheetState.Expanded -> Unit
                }
            }
            totalX = 0f
            totalY = 0f
        },
        onDragCancel = {
            totalX = 0f
            totalY = 0f
        },
    ) { _, dragAmount ->
        totalX += dragAmount.x
        totalY += dragAmount.y
    }
}

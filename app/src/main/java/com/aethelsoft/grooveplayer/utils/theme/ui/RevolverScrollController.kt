package com.aethelsoft.grooveplayer.utils.theme.ui


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.math.roundToInt

@Stable
class RevolverScrollController(
    private val slotStepDeg: Float
) {
    var rawRotation by mutableFloatStateOf(0f)
        private set

    private var lastSlot: Int? = null

    fun onScroll(delta: Float, haptic: HapticFeedback) {
        rawRotation -= delta * 0.15f

        val slot = (rawRotation / slotStepDeg).roundToInt()
        if (slot != lastSlot) {
            lastSlot = slot
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun onFling(velocity: Float) {
        rawRotation += velocity * 0.00025f
    }

    suspend fun snap() {
        rawRotation =
            (rawRotation / slotStepDeg).roundToInt() * slotStepDeg
        lastSlot = (rawRotation / slotStepDeg).roundToInt()
    }

    @Composable
    fun rotation(): Float =
        animateFloatAsState(
            rawRotation,
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = 300f
            ),
            label = "RevolverRotation"
        ).value
}
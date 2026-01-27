package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.VisualizationMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class VizPhase {
    COLLAPSED,
    COLLAPSED_FADING_OUT,
    EXPANDED,
    EXPANDED_FADING_OUT
}

@Composable
fun VisualizationControl(
    currentMode: VisualizationMode,
    onModeSelected: suspend (VisualizationMode) -> Boolean
) {
    var phase by rememberSaveable { mutableStateOf(VizPhase.COLLAPSED) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(100.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (phase == VizPhase.COLLAPSED) {
                    phase = VizPhase.COLLAPSED_FADING_OUT
                    scope.launch {
                        delay(120)
                        phase = VizPhase.EXPANDED
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Visualization",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )

            Spacer(Modifier.width(4.dp))

            VisualizationMode.entries.forEach { mode ->
                val visible = when (phase) {
                    VizPhase.COLLAPSED,
                    VizPhase.COLLAPSED_FADING_OUT ->
                        mode == currentMode

                    VizPhase.EXPANDED ->
                        true

                    VizPhase.EXPANDED_FADING_OUT ->
                        false
                }

                AnimatedVisibility(
                    visible = visible,
                    enter =
                        fadeIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + expandIn(
                            expandFrom = Alignment.Center,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ),
                    exit =
                        fadeOut(animationSpec = tween(120)) +
                                shrinkOut(animationSpec = tween(120))
                ) {
                    ModeChip(
                        label = mode.label(),
                        selected = mode == currentMode,
                        clickable = phase == VizPhase.EXPANDED
                    ) {
                        scope.launch {
                            val ok = onModeSelected(mode)
                            if (ok) {
                                phase = VizPhase.EXPANDED_FADING_OUT
                                delay(120)
                                phase = VizPhase.COLLAPSED
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    clickable: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) Color.White else Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ModeChipBg"
    )

    val textColor by animateColorAsState(
        if (selected) Color.Black else Color.White,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ModeChipText"
    )

    val modifier = Modifier
        .height(24.dp)
        .clip(RoundedCornerShape(100.dp))
        .background(bg)
        .padding(horizontal = 10.dp)

    Box(
        modifier = if (clickable) {
            modifier.clickable(onClick = onClick)
        } else {
            modifier
        },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            maxLines = 1
        )
    }
}

private fun VisualizationMode.label(): String =
    when (this) {
        VisualizationMode.OFF -> "Off"
        VisualizationMode.SIMULATED -> "Simulated"
        VisualizationMode.REAL_TIME -> "Real-time"
    }
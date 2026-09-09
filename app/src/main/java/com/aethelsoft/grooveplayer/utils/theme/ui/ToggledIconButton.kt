package com.aethelsoft.grooveplayer.utils.theme.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Minimum accessible touch target for icon toggles. */
private val MinToggleHitTarget = 48.dp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> ToggledIconButton(
    state: T,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Visual diameter of the circular chrome; never below [MinToggleHitTarget]. */
    size: Dp = MinToggleHitTarget,
    activeBackground: Color = Color.Transparent,
    inactiveBackground: Color = Color.Transparent,
    iconContent: @Composable (T) -> Unit
) {
    val hitSize = maxOf(size, MinToggleHitTarget)

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(hitSize)
            .clip(CircleShape)
            .background(
                if (state == true) activeBackground else inactiveBackground
            ),
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + scaleIn(
                    initialScale = 0.85f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )) togetherWith
                        (fadeOut(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + scaleOut(
                            targetScale = 0.85f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ))
            },
            label = "ToggledIconButton"
        ) { value ->
            iconContent(value)
        }
    }
}

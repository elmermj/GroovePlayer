package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Top-anchored miniplayer while song details are expanded.
 * Slides down to appear, slides up to disappear.
 */
@Composable
fun PlayerDetailsMiniPlayerOverlay(
    visible: Boolean,
    onCollapseToPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(350),
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300),
            ),
        ) {
            MiniPlayerBar(
                onMiniPlayerClicked = onCollapseToPlayer,
                anchorAtTop = true,
            )
        }
    }
}

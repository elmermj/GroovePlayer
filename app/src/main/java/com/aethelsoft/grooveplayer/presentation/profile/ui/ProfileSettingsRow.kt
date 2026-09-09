package com.aethelsoft.grooveplayer.presentation.profile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

enum class ActionType {
    OPTIONS,
    PASSIVE,
    /** Clickable row that navigates or runs [ProfileSettingRow]'s onClick (no expand). */
    LINK,
    EXPANDABLE,
    INACTIVE
}

/** Accessible minimum height for settings rows. */
private val MinSettingsRowHeight = 48.dp

/**
 * Renders a multi-color profile icon without applying a monochrome tint.
 */
@Composable
fun ProfileRowIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = Color.Unspecified,
        modifier = Modifier.size(24.dp)
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ProfileSettingRow(
    icon: @Composable (() -> Unit)? = null,
    title: String,
    subtitle: String? = null,
    actionType: ActionType = ActionType.PASSIVE,
    primaryContent: @Composable (() -> Unit)? = null,
    secondaryContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {},
    isSecondaryVisible: Boolean? = null,
    onSecondaryVisibleChange: ((Boolean) -> Unit)? = null,
) {
    val localPrimaryContent = primaryContent ?: @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinSettingsRowHeight)
                .background(Color.Transparent),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Spacer(Modifier.width(4.dp))
            if (icon != null) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Transparent)
            ) {
                Text(
                    text = title,
                    style = GrooveTheme.typography.sectionItemTitle.toTextStyle(),
                    color = GrooveTheme.colors.onSurface
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                        color = SoftWhite
                    )
                }
            }
        }
    }
    // Shared visibility state: controlled from parent when isSecondaryVisible/onSecondaryVisibleChange
    // are provided, otherwise managed internally per-row.
    @Composable
    fun resolveVisibilityState(initial: Boolean = false): Pair<Boolean, (Boolean) -> Unit> {
        return if (isSecondaryVisible != null && onSecondaryVisibleChange != null) {
            isSecondaryVisible to onSecondaryVisibleChange
        } else {
            val internalState = remember { mutableStateOf(initial) }
            internalState.value to { internalState.value = it }
        }
    }

    when (actionType){
        ActionType.OPTIONS -> {
            val (showSecondary, setShowSecondary) = resolveVisibilityState(initial = false)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinSettingsRowHeight)
                    .clickable {
                        setShowSecondary(!showSecondary)
                        onClick()
                    }
            ) {
                AnimatedContent(
                    targetState = showSecondary && secondaryContent != null,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(150)).togetherWith(
                                fadeOut(animationSpec = tween(150)))
                    },
                    label = "ProfileSettingRowOptions"
                ) { showAlt ->
                    if (showAlt && secondaryContent != null) {
                        secondaryContent.invoke()
                    } else {
                        localPrimaryContent.invoke()
                    }
                }
            }
        }
        ActionType.PASSIVE -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinSettingsRowHeight)
            ) {
                localPrimaryContent.invoke()
            }
        }
        ActionType.LINK -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinSettingsRowHeight)
                    .clickable(onClick = onClick)
            ) {
                localPrimaryContent.invoke()
            }
        }
        ActionType.EXPANDABLE -> {
            val (expanded, setExpanded) = resolveVisibilityState(initial = false)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        setExpanded(!expanded)
                        onClick()
                    }
            ) {
                localPrimaryContent.invoke()
                AnimatedVisibility(
                    visible = expanded && secondaryContent != null,
                    enter = expandVertically(animationSpec = tween(200)) + fadeIn(
                        animationSpec = tween(200)
                    ),
                    exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(
                        animationSpec = tween(200)
                    ),
                    label = "ProfileSettingRowExpandable"
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        secondaryContent?.invoke()
                    }
                }
            }
        }
        ActionType.INACTIVE -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinSettingsRowHeight)
                    .alpha(0.4f)
            ) {
                localPrimaryContent.invoke()
            }
        }
    }

}
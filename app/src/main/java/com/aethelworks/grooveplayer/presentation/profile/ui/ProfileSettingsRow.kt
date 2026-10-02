package com.aethelworks.grooveplayer.presentation.profile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aethelworks.grooveplayer.presentation.common.FlatChevron
import com.aethelworks.grooveplayer.presentation.common.FlatLeadingIcon
import com.aethelworks.grooveplayer.presentation.common.FlatRow
import com.aethelworks.grooveplayer.presentation.common.HairlineDivider

enum class ActionType {
    OPTIONS,
    PASSIVE,
    /** Clickable row that navigates or runs [ProfileSettingRow]'s onClick (no expand). */
    LINK,
    EXPANDABLE,
    INACTIVE
}

/**
 * Renders a multi-color profile icon without applying a monochrome tint.
 * Kept for the account header, which is outside the flat settings rows.
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

/** Outlined settings icon at SoftWhite 80%. */
@Composable
fun SettingsRowIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
) {
    FlatLeadingIcon(
        imageVector = imageVector,
        contentDescription = contentDescription,
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
    val showChevron = actionType == ActionType.LINK ||
        actionType == ActionType.EXPANDABLE ||
        actionType == ActionType.OPTIONS

    @Composable
    fun resolveVisibilityState(initial: Boolean = false): Pair<Boolean, (Boolean) -> Unit> {
        return if (isSecondaryVisible != null && onSecondaryVisibleChange != null) {
            isSecondaryVisible to onSecondaryVisibleChange
        } else {
            val internalState = remember { mutableStateOf(initial) }
            internalState.value to { internalState.value = it }
        }
    }

    @Composable
    fun RowBody(
        onClick: (() -> Unit)?,
        expanded: Boolean,
    ) {
        if (primaryContent != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            ) {
                primaryContent()
            }
        } else {
            FlatRow(
                title = title,
                subtitle = subtitle,
                leading = icon,
                trailing = if (showChevron) {
                    { FlatChevron(expanded = expanded) }
                } else {
                    null
                },
                onClick = onClick,
                showDivider = false,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (actionType == ActionType.INACTIVE) 0.4f else 1f),
    ) {
        when (actionType) {
            ActionType.OPTIONS -> {
                val (showSecondary, setShowSecondary) = resolveVisibilityState(initial = false)
                val showingAlt = showSecondary && secondaryContent != null
                val toggle = {
                    setShowSecondary(!showSecondary)
                    onClick()
                }
                AnimatedContent(
                    targetState = showingAlt,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(150)).togetherWith(
                            fadeOut(animationSpec = tween(150)),
                        )
                    },
                    label = "ProfileSettingRowOptions",
                ) { showAlt ->
                    if (showAlt && secondaryContent != null) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clickable(onClick = toggle)
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            secondaryContent()
                        }
                    } else {
                        RowBody(onClick = toggle, expanded = false)
                    }
                }
                HairlineDivider()
            }
            ActionType.PASSIVE -> {
                RowBody(onClick = null, expanded = false)
                HairlineDivider()
            }
            ActionType.LINK -> {
                RowBody(onClick = onClick, expanded = false)
                HairlineDivider()
            }
            ActionType.EXPANDABLE -> {
                val (expanded, setExpanded) = resolveVisibilityState(initial = false)
                val open = expanded && secondaryContent != null
                RowBody(
                    onClick = {
                        setExpanded(!expanded)
                        onClick()
                    },
                    expanded = open,
                )
                AnimatedVisibility(
                    visible = open,
                    enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                    exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200)),
                    label = "ProfileSettingRowExpandable",
                ) {
                    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
                        secondaryContent?.invoke()
                    }
                }
                HairlineDivider()
            }
            ActionType.INACTIVE -> {
                RowBody(onClick = null, expanded = false)
                HairlineDivider()
            }
        }
    }
}

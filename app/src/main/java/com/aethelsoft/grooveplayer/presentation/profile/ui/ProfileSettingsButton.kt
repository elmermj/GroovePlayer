package com.aethelsoft.grooveplayer.presentation.profile.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

@Composable
fun ProfileSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String,
    isInverse: Boolean = false,
    isActive: Boolean = true,
    textStyle: TextStyle = GrooveTheme.typography.buttonLabel.toTextStyle(),
    textColor: Color? = null,
) {
    val colors = GrooveTheme.colors
    val containerColor = if (isActive) {
        if (isInverse) colors.surface else colors.accent
    } else {
        colors.inactiveContainer
    }

    val contentColor = if (isActive) {
        if (isInverse) colors.muted else colors.onAccent
    } else {
        colors.inactive
    }

    val resolvedTextColor = textColor ?: contentColor

    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = colors.inactiveContainer,
            disabledContentColor = colors.inactive,
        )
    ) {
        Text(
            text = title,
            style = textStyle,
            color = resolvedTextColor,
        )
    }
}

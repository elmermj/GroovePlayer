package com.aethelsoft.grooveplayer.presentation.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.aethelsoft.grooveplayer.utils.theme.ui.InactivePrimary
import com.aethelsoft.grooveplayer.utils.theme.ui.InactiveSecondary
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftBlack
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

@Composable
fun ProfileSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String,
    isInverse: Boolean = false,
    isActive: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    textColor: Color? = null,
){
    val containerColor = if (isActive) {
        if (isInverse) SoftBlack else SoftWhite
    } else {
        InactiveSecondary
    }

    val contentColor = if (isActive) {
        if (isInverse) SoftWhite else SoftBlack
    } else {
        InactivePrimary
    }

    val resolvedTextColor = textColor ?: contentColor
    
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(),
        colors = ButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = InactiveSecondary,
            disabledContentColor = InactivePrimary
        )
    ) {
        Text(
            text = title,
            style = textStyle,
            color = resolvedTextColor
        )
    }
}
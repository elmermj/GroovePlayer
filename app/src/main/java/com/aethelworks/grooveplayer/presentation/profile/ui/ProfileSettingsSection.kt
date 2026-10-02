package com.aethelworks.grooveplayer.presentation.profile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aethelworks.grooveplayer.presentation.common.OverlineLabel

@Composable
fun ProfileSectionComponent(
    sectionTitle: String,
    showOverline: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showOverline) {
            OverlineLabel(sectionTitle)
        } else {
            Spacer(Modifier.height(16.dp))
        }
        content?.invoke()
    }
}

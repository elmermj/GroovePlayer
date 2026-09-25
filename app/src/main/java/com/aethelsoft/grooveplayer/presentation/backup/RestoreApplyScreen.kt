package com.aethelsoft.grooveplayer.presentation.backup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

@Composable
fun RestoreApplyScreen(
    startDownload: Boolean,
    onFinished: () -> Unit,
    viewModel: RestoreApplyViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(startDownload) {
        viewModel.run(startDownload)
    }
    LaunchedEffect(ui.finished) {
        if (ui.finished) onFinished()
    }

    BackHandler {
        if (!ui.busy) onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GrooveTheme.colors.canvas)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (ui.busy) {
            CircularProgressIndicator(color = GrooveTheme.colors.accent)
            Spacer(Modifier.height(24.dp))
        }
        Text(
            text = ui.status,
            style = GrooveTheme.typography.sectionTitle.toTextStyle(),
            color = GrooveTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        )
        if (!ui.retry.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = ui.retry.orEmpty(),
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = GrooveTheme.colors.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        if (!ui.detail.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = ui.detail.orEmpty(),
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = GrooveTheme.colors.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
        val fraction = ui.fraction
        if (ui.busy && fraction != null) {
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = GrooveTheme.colors.accent,
                trackColor = GrooveTheme.colors.surface,
            )
        }
        if (!ui.error.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = ui.error.orEmpty(),
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = GrooveTheme.colors.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onFinished) {
                Text(
                    text = "Go to Home",
                    color = GrooveTheme.colors.accent,
                )
            }
        }
    }
}

package com.aethelsoft.grooveplayer.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.utils.APP_BAR_HEIGHT
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.icons.XBack
import com.aethelsoft.grooveplayer.utils.theme.icons.XClose

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GradientAppBar(
    title: String,
    deviceType: DeviceType,
    modifier: Modifier = Modifier
){
    val horizontalPadding = when (deviceType) {
        DeviceType.PHONE -> S_PADDING
        DeviceType.TABLET -> S_PADDING
        DeviceType.LARGE_TABLET -> M_PADDING
    }
    val density = LocalDensity.current
    val xContentWindowInsets = contentWindowInsets
    val safeInsets = remember(contentWindowInsets) { MutableWindowInsets(xContentWindowInsets) }

    val navigation = rememberNavigationActions()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(
                APP_BAR_HEIGHT + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.95f * 1),
                        Color.Black.copy(alpha = 0.6f * 1),
                        Color.Transparent
                    )
                )
            )
            .padding(
                top = 0.dp,
                start = 0.dp,
                end = horizontalPadding,
                bottom = 0.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navigation.goBack() }
            ) {
                Icon(
                    imageVector = XBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}
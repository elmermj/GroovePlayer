package com.aethelsoft.grooveplayer.presentation.player.layouts

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.aethelsoft.grooveplayer.data.player.AudioVisualizationData
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.presentation.player.BluetoothViewModel
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel
import com.aethelsoft.grooveplayer.presentation.player.formatMillis
import com.aethelsoft.grooveplayer.presentation.player.ui.BluetoothBottomSheet
import com.aethelsoft.grooveplayer.presentation.player.ui.CustomSlider
import com.aethelsoft.grooveplayer.presentation.player.ui.PlayerControls
import com.aethelsoft.grooveplayer.presentation.player.ui.VolumeSlider
import com.aethelsoft.grooveplayer.presentation.player.ui.VisualizationControl
import com.aethelsoft.grooveplayer.utils.rememberRecordAudioPermissionState
import com.aethelsoft.grooveplayer.domain.model.VisualizationMode
import com.aethelsoft.grooveplayer.utils.helpers.BluetoothHelpers
import com.aethelsoft.grooveplayer.utils.theme.icons.XBack
import com.aethelsoft.grooveplayer.utils.theme.icons.XBluetooth
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.player.ui.BTIndicatorIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhonePlayerLayout(
    song: com.aethelsoft.grooveplayer.domain.model.Song?,
    pos: Long,
    dur: Long,
    isPlaying: Boolean,
    shuffle: Boolean,
    repeat: RepeatMode,
    playerViewModel: PlayerViewModel,
    bg: Color = Color.Black,
    onClose: () -> Unit
) {
    var showBluetoothSheet by remember { mutableStateOf(false) }
    val bluetoothViewModel: BluetoothViewModel = hiltViewModel()
    val connectedBtDevice by bluetoothViewModel.connectedDevice.collectAsState()

    val audioVisualization by playerViewModel.audioVisualization.collectAsState()
    val visualizationMode by playerViewModel.visualizationMode.collectAsState()
    // Waveform / glow visualization toggle
    val (hasRecordAudioPermission, requestRecordAudioPermission) = rememberRecordAudioPermissionState()
    val effectiveVisualization =
        when (visualizationMode) {
            VisualizationMode.OFF -> AudioVisualizationData()
            VisualizationMode.SIMULATED,
            VisualizationMode.REAL_TIME -> {
                if (hasRecordAudioPermission) audioVisualization else AudioVisualizationData()
            }
        }
    val context = LocalContext.current
    
    // Extract dominant color from artwork
    var dominantColor by remember { mutableStateOf(Color.White) }
    
    LaunchedEffect(song?.artworkUrl) {
        song?.artworkUrl?.let { url ->
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false) // Required for Palette API
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.image.toBitmap()
                    dominantColor = extractDominantColorPhone(bitmap)
                }
            } catch (e: Exception) {
                dominantColor = Color.White
            }
        } ?: run {
            dominantColor = Color.White
        }
    }

    if (showBluetoothSheet) {
        BluetoothBottomSheet(onDismiss = { showBluetoothSheet = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(XBack, contentDescription = "Close")
            }
            IconButton(onClick = { showBluetoothSheet = true }) {

                BTIndicatorIcon(
                    connectedDeviceName = connectedBtDevice?.name,
                    isConnected = connectedBtDevice != null,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(320.dp)
                    .graphicsLayer { clip = false },
                contentAlignment = Alignment.Center
            ) {
                GlowingArtworkContainerPhone(
                    dominantColor = dominantColor,
                    visualization = effectiveVisualization,
                    modifier = Modifier
                        .size(320.dp)
                        .padding(24.dp) // Add padding for glow to extend outward
                ) {
                    AsyncImage(
                        model = song?.artworkUrl,
                        contentDescription = "Artwork",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

        Spacer(modifier = Modifier.height(16.dp))
        Text(song?.title ?: "", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Text(song?.artist ?: "", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(24.dp))

        CustomSlider(
            value = if (dur > 0) pos.toFloat() / dur else 0f,
            onValueChange = { frac ->
                val target = (frac * dur).toLong()
                playerViewModel.seekTo(target)
            },
            modifier = Modifier.fillMaxWidth(),
            height = 4.dp,
            activeColor = Color.White,
            inactiveColor = Color.White.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMillis(pos))
            Text(formatMillis(dur - pos))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            VolumeSlider(
                playerViewModel = playerViewModel,
                modifier = Modifier.width(180.dp),
                backgroundColor = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PlayerControls(
            isMiniPlayer = false,
            isPlaying = isPlaying,
            shuffle = shuffle,
            repeat = repeat,
            playerViewModel = playerViewModel
        )

        Spacer(modifier = Modifier.height(12.dp))

        VisualizationControl(
            currentMode = visualizationMode,
            onModeSelected = { mode ->
                when (mode) {
                    VisualizationMode.REAL_TIME -> {
                        if (!hasRecordAudioPermission) {
                            requestRecordAudioPermission()
                        }
                        if (!hasRecordAudioPermission) {
                            false
                        } else {
                            playerViewModel.setVisualizationMode(mode)
                            true
                        }
                    }
                    VisualizationMode.OFF,
                    VisualizationMode.SIMULATED -> {
                        playerViewModel.setVisualizationMode(mode)
                        true
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        val queue by playerViewModel.queue.collectAsState()
        if (queue.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Up Next", style = MaterialTheme.typography.labelSmall)
                queue.take(5).forEach { q ->
                    Text(q.title, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

/**
 * Audio-reactive glow container for phone artwork with configurable visual effects.
 * 
 * @param dominantColor The primary color extracted from artwork for glow tinting
 * @param visualization Real-time audio analysis data (bass, mid, treble, stereo, beat)
 * @param config Glow effect configuration - use presets or customize
 * @param modifier Modifier for the container (must use graphicsLayer { clip = false })
 * @param content The artwork content to wrap with glow effect
 */
@Composable
fun GlowingArtworkContainerPhone(
    dominantColor: Color,
    visualization: AudioVisualizationData,
    config: GlowEffectConfig = GlowEffectConfig.Phone,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current

    // Balanced animations: responsive but smooth
    val bassGlow by animateFloatAsState(
        targetValue = visualization.bass,
        animationSpec = tween(75),
        label = "BassGlow"
    )
    
    val midGlow by animateFloatAsState(
        targetValue = visualization.mid,
        animationSpec = tween(70),
        label = "MidGlow"
    )
    
    val trebleGlow by animateFloatAsState(
        targetValue = visualization.treble,
        animationSpec = tween(60),
        label = "TrebleGlow"
    )
    
    val beatPulse by animateFloatAsState(
        targetValue = visualization.beat,
        animationSpec = tween(50),
        label = "BeatPulse"
    )
    
    val stereoBalance by animateFloatAsState(
        targetValue = visualization.stereoBalance,
        animationSpec = tween(120),
        label = "StereoBalance"
    )

    Box(
        modifier = modifier
            .graphicsLayer { clip = false }
            .drawBehind {
                // Calculate colors using config
                val bassColor = dominantColor.copy(
                    red = dominantColor.red * config.bassColorRedMultiplier,
                    blue = dominantColor.blue * config.bassColorBlueMultiplier
                )
                val midColor = dominantColor
                val trebleColor = dominantColor.copy(
                    red = (dominantColor.red + config.trebleColorBoost).coerceAtMost(1f),
                    green = (dominantColor.green + config.trebleColorBoost).coerceAtMost(1f),
                    blue = (dominantColor.blue + config.trebleColorBoost).coerceAtMost(1f)
                )
                
                // Calculate parameters using config
                val artworkSizePx = with(density) { 320.dp.toPx() }
                val baseIntensity = (bassGlow * 0.5f + midGlow * 0.3f + trebleGlow * 0.2f)
                val glowAlpha = (config.minAlpha + baseIntensity * config.intensityAlphaRange + beatPulse * config.beatAlphaBoost)
                    .coerceIn(0f, config.maxAlpha)
                
                val baseBlurRadius = artworkSizePx * config.baseBlurMultiplier
                val bassExpansion = artworkSizePx * config.bassExpansionMultiplier * bassGlow
                val trebleTightness = -artworkSizePx * config.trebleTightnessMultiplier * trebleGlow
                val beatExpansion = artworkSizePx * config.beatExpansionMultiplier * beatPulse
                val blurRadius = (baseBlurRadius + bassExpansion + trebleTightness + beatExpansion)
                    .coerceAtLeast(artworkSizePx * config.minBlurMultiplier)
                
                val cornerPx = with(density) { config.cornerRadius.toPx() }
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val halfSize = artworkSizePx / 2f
                
                drawIntoCanvas { canvas ->
                    // Layer 1: Bass layer with stereo-based horizontal extension (tapered edges)
                    if (bassGlow > config.bassRenderThreshold) {
                        // Stereo-based horizontal extension
                        val maxHorizontalExtension = artworkSizePx * config.bassHorizontalExtension
                        
                        // Calculate left and right extensions based on stereo balance
                        val leftExtension = if (stereoBalance <= 0) {
                            maxHorizontalExtension * (1f + kotlin.math.abs(stereoBalance))
                        } else {
                            maxHorizontalExtension * (1f - stereoBalance)
                        }
                        
                        val rightExtension = if (stereoBalance >= 0) {
                            maxHorizontalExtension * (1f + stereoBalance)
                        } else {
                            maxHorizontalExtension * (1f + stereoBalance)
                        }
                        
                        // Draw bass as ellipse for pointed/tapered edges
                        val bassPaint = Paint().asFrameworkPaint().apply {
                            isAntiAlias = true
                            color = bassColor
                                .copy(alpha = glowAlpha * config.bassLayerAlpha * bassGlow)
                                .toArgb()
                            maskFilter = BlurMaskFilter(
                                blurRadius * config.bassLayerBlurScale,
                                BlurMaskFilter.Blur.NORMAL
                            )
                        }
                        
                        // Draw as oval/ellipse for tapered horizontal edges
                        canvas.nativeCanvas.drawOval(
                            centerX - halfSize - leftExtension,
                            centerY - halfSize,
                            centerX + halfSize + rightExtension,
                            centerY + halfSize,
                            bassPaint
                        )
                    }
                    
                    // Layer 2: Mid layer
                    if (midGlow > config.midRenderThreshold) {
                        val midPaint = Paint().asFrameworkPaint().apply {
                            isAntiAlias = true
                            color = midColor
                                .copy(alpha = glowAlpha * config.midLayerAlpha * midGlow)
                                .toArgb()
                            maskFilter = BlurMaskFilter(
                                blurRadius * config.midLayerBlurScale,
                                BlurMaskFilter.Blur.NORMAL
                            )
                        }
                        
                        canvas.nativeCanvas.drawRoundRect(
                            centerX - halfSize,
                            centerY - halfSize,
                            centerX + halfSize,
                            centerY + halfSize,
                            cornerPx,
                            cornerPx,
                            midPaint
                        )
                    }
                    
                    // Layer 3: Treble layer
                    if (trebleGlow > config.trebleRenderThreshold) {
                        val treblePaint = Paint().asFrameworkPaint().apply {
                            isAntiAlias = true
                            color = trebleColor
                                .copy(alpha = glowAlpha * config.trebleLayerAlpha * trebleGlow)
                                .toArgb()
                            maskFilter = BlurMaskFilter(
                                blurRadius * config.trebleLayerBlurScale,
                                BlurMaskFilter.Blur.NORMAL
                            )
                        }
                        
                        val stereoOffsetX = halfSize * config.stereoBalanceTrebleScale * -stereoBalance
                        
                        canvas.nativeCanvas.drawRoundRect(
                            centerX - halfSize - stereoOffsetX,
                            centerY - halfSize,
                            centerX + halfSize - stereoOffsetX,
                            centerY + halfSize,
                            cornerPx,
                            cornerPx,
                            treblePaint
                        )
                    }
                    
                    // Layer 4: White beat flash
                    if (config.enableBeatFlash && beatPulse > config.beatFlashThreshold) {
                        val beatFlashPaint = Paint().asFrameworkPaint().apply {
                            isAntiAlias = true
                            color = Color.White
                                .copy(alpha = config.beatFlashAlpha * beatPulse)
                                .toArgb()
                            maskFilter = BlurMaskFilter(
                                blurRadius * config.beatFlashBlurScale,
                                BlurMaskFilter.Blur.NORMAL
                            )
                        }
                        
                        canvas.nativeCanvas.drawRoundRect(
                            centerX - halfSize,
                            centerY - halfSize,
                            centerX + halfSize,
                            centerY + halfSize,
                            cornerPx,
                            cornerPx,
                            beatFlashPaint
                        )
                    }
                }
            }
    ) {
        content()
    }
}

fun extractDominantColorPhone(bitmap: coil3.Bitmap): Color {
    val palette = Palette.from(bitmap).generate()
    val swatch =
        palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.dominantSwatch

    return swatch?.rgb?.let { Color(it) } ?: Color(0xFFFFFFFF)
}
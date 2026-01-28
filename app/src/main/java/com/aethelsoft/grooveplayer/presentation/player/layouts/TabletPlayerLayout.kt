package com.aethelsoft.grooveplayer.presentation.player.layouts

import android.graphics.BlurMaskFilter
import android.util.Log
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
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
import androidx.compose.ui.platform.LocalWindowInfo
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
import com.aethelsoft.grooveplayer.domain.model.VisualizationMode
import com.aethelsoft.grooveplayer.utils.rememberRecordAudioPermissionState
import com.aethelsoft.grooveplayer.utils.rememberRecordAudioPermissionState
import com.aethelsoft.grooveplayer.utils.helpers.BluetoothHelpers
import com.aethelsoft.grooveplayer.utils.theme.icons.XBack
import com.aethelsoft.grooveplayer.utils.theme.icons.XBluetooth
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.player.ui.BTIndicatorIcon
import com.aethelsoft.grooveplayer.presentation.player.ui.BluetoothEllipticalLazyScroll
import com.aethelsoft.grooveplayer.presentation.player.ui.EqualizerControlsComponent
import com.aethelsoft.grooveplayer.presentation.player.ui.PlayerQueueComponent
import com.aethelsoft.grooveplayer.utils.L_PADDING
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.rememberBluetoothPermissionState
import com.aethelsoft.grooveplayer.utils.theme.icons.XAudioLines
import com.aethelsoft.grooveplayer.utils.theme.icons.XListMusic
import com.aethelsoft.grooveplayer.utils.theme.ui.ToggledIconButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TabletPlayerLayout(
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
    var showQueue by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showBluetoothSheet by remember { mutableStateOf(false) }
    val queue by playerViewModel.queue.collectAsState()
    val audioVisualization by playerViewModel.audioVisualization.collectAsState()
    val visualizationMode by playerViewModel.visualizationMode.collectAsState()

    // Waveform / glow visualization toggle
    val (hasRecordAudioPermission, requestRecordAudioPermission) = rememberRecordAudioPermissionState()
    var isWaveformVisualizationEnabled by rememberSaveable { mutableStateOf(true) }
    val effectiveVisualization =
        when (visualizationMode) {
            VisualizationMode.OFF -> AudioVisualizationData()
            VisualizationMode.SIMULATED,
            VisualizationMode.REAL_TIME -> {
                if (hasRecordAudioPermission) audioVisualization else AudioVisualizationData()
            }
        }

    // Bluetooth ViewModel
    val bluetoothViewModel: com.aethelsoft.grooveplayer.presentation.player.BluetoothViewModel = hiltViewModel()
    val (hasBluetoothPermissions, requestBluetoothPermissions) = rememberBluetoothPermissionState()
    val availableDevices by bluetoothViewModel.availableDevices.collectAsState()
    val isScanning by bluetoothViewModel.isScanning.collectAsState()
    val connectedDevice by bluetoothViewModel.connectedDevice.collectAsState()
    val connectingDeviceAddress by bluetoothViewModel.connectingDeviceAddress.collectAsState()
    val connectionSuccessDisplay by bluetoothViewModel.connectionSuccessDisplay.collectAsState()
    val connectionFailedDisplay by bluetoothViewModel.connectionFailedDisplay.collectAsState()
    val isBluetoothEnabledNow = bluetoothViewModel.isBluetoothEnabled()

    // Ensure showBluetoothSheet and showQueue are never true at the same time
    LaunchedEffect(showBluetoothSheet, showQueue) {
        if (showBluetoothSheet && showQueue) {
            // If both are true, close the one that wasn't just opened
            // This is a safeguard in case both get set to true somehow
            showQueue = false
        }
    }

    // Auto-start scanning when Bluetooth sheet is opened
    LaunchedEffect(showBluetoothSheet, hasBluetoothPermissions) {
        if (showBluetoothSheet &&
            hasBluetoothPermissions &&
            bluetoothViewModel.isBluetoothEnabled() &&
            !isScanning &&
            availableDevices.isEmpty()) {
            bluetoothViewModel.startScanning()
        }
    }
    val configuration = LocalWindowInfo.current
    val screenHeight = configuration.containerSize.height.dp
    val screenWidth = configuration.containerSize.width.dp
    val maxArtworkHeight = minOf(screenHeight * 0.4f, screenWidth * 0.5f)
    val context = LocalContext.current
    val xContentWindowInsets = contentWindowInsets
    val safeInsets = remember(contentWindowInsets) { MutableWindowInsets(xContentWindowInsets) }

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
                    dominantColor = extractDominantColor(bitmap)
                    Log.d("LargeTablet", "✨ Extracted dominant color: $dominantColor from ${song.title}")
                }
            } catch (e: Exception) {
                Log.e("LargeTablet", "Failed to extract color: ${e.message}")
                dominantColor = Color.White
            }
        } ?: run {
            dominantColor = Color.White
        }
    }

    // Log visualization changes for debugging
    LaunchedEffect(audioVisualization) {
        if (audioVisualization.overall > 0.2f) {
            Log.v("LargeTablet", "🎵 Visualization - Bass: ${"%.2f".format(audioVisualization.bass)} | Mid: ${"%.2f".format(audioVisualization.mid)} | Treble: ${"%.2f".format(audioVisualization.treble)} | Stereo: ${"%.2f".format(audioVisualization.stereoBalance)} | Beat: ${"%.2f".format(audioVisualization.beat)}")
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(
                top = 32.dp,
                bottom = 32.dp,
                start = 32.dp,
                end = 32.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(48.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(XBack, contentDescription = "Close")
                }
                ToggledIconButton(
                    state = showBluetoothSheet,
                    onClick = {
                        if (!showBluetoothSheet) {
                            // If opening bluetooth sheet, close queue first
                            if (showQueue) {
                                showQueue = false
                            }
                        }
                        showBluetoothSheet = !showBluetoothSheet
                    },
                    activeBackground = Color.White,
                    inactiveBackground = Color.Transparent,
                ) {
                    BTIndicatorIcon(
                        connectedDeviceName = connectedDevice?.name,
                        isConnected = connectedDevice != null,
                        tint = if(showBluetoothSheet || connectedDevice != null) Color.Black else Color.White,
                    )
                }
            }
            val density = LocalDensity.current

            val screenWidthPx = with(density) { screenWidth.toPx() }
            val sidePanelWidthPx = with(density) { 360.dp.toPx() }
            val safeMarginPx = screenWidthPx * 0.05f

            // How far artwork can move without leaving viewport
//            val maxShiftPx = (screenWidthPx / 2f) - sidePanelWidthPx - safeMarginPx

            // Target offset based on visible panels
            val artworkTargetOffsetPx = when {
                showQueue && !showEqualizer && !showBluetoothSheet -> -(sidePanelWidthPx / 2f + safeMarginPx)        // queue only → left
                showEqualizer && !showQueue && !showBluetoothSheet -> +(sidePanelWidthPx / 2f + safeMarginPx)        // equalizer only → right
                showBluetoothSheet && !showQueue && !showEqualizer -> -(sidePanelWidthPx / 2f + safeMarginPx)        // bluetooth only → left
                else -> 0f                                       // none or both → center
            }

            val artworkScale by animateFloatAsState(
                targetValue = if (showQueue || showEqualizer || showBluetoothSheet) 0.85f else 1f,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 300f
                ),
                label = "ArtworkScale"
            )

            val artworkOffsetXPx by animateFloatAsState(
                targetValue = artworkTargetOffsetPx,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "ArtworkOffsetXPx"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxArtworkHeight + L_PADDING)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GlowingArtworkContainer(
                        dominantColor = dominantColor,
                        visualization = effectiveVisualization,
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = artworkOffsetXPx
                                scaleX = artworkScale
                                scaleY = artworkScale
                            }
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .padding(M_PADDING)
                    ) {
                        SwipeableArtwork(
                            size = maxArtworkHeight - S_PADDING,
                            artworkUrl = song?.artworkUrl,
                            onTap = { playerViewModel.playPauseToggle() },
                            onSwipePrevious = { playerViewModel.previous() },
                            onSwipeNext = { playerViewModel.next() },
                            onDismiss = onClose
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showQueue,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                ) {
                    Column(
                        modifier = Modifier
                            .height(maxArtworkHeight)
                            .width(360.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Queue",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(S_PADDING * 2),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.titleLarge
                        )

                        PlayerQueueComponent(
                            currentSong = song,
                            queue = queue,
                            onItemClick = { selectedSong ->
                                playerViewModel.setQueue(
                                    queue,
                                    queue.indexOf(selectedSong),
                                    isEndlessQueue = true
                                )
                                showQueue = false
                            },
                            maxHeight = maxArtworkHeight
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showBluetoothSheet,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(maxArtworkHeight)
                            .width(360.dp + 32.dp)
                            .align(Alignment.CenterEnd)
                    ) {
                        BluetoothEllipticalLazyScroll(
                            availableDevices = availableDevices,
                            connectedDevice = connectedDevice,
                            onDeviceClick = { device ->
                                if (connectedDevice?.address == device.address) {
                                    bluetoothViewModel.disconnectDevice()
                                } else {
                                    bluetoothViewModel.connectToDevice(device)
                                }
                            },
                            maxHeight = maxArtworkHeight,
                            connectingDeviceAddress = connectingDeviceAddress,
                            connectionSuccessDisplay = connectionSuccessDisplay,
                            connectionFailedDisplay = connectionFailedDisplay,
                            isBluetoothEnabled = isBluetoothEnabledNow,
                            hasBluetoothPermissions = hasBluetoothPermissions,
                            onRequestBluetoothPermission = requestBluetoothPermissions,
                            onBluetoothEnabledResult = { bluetoothViewModel.refreshConnectionState() },
                            onDismiss = { showBluetoothSheet = false }
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showEqualizer,
                    enter = slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(),
                    exit = slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    EqualizerControlsComponent(
                        modifier = Modifier
                            .width(360.dp)
                            .padding(top = 24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(L_PADDING))
            Text(
                song?.title ?: "",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Text(
                song?.artist ?: "",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(L_PADDING))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    CustomSlider(
                        value = if (dur > 0) pos.toFloat() / dur else 0f,
                        onValueChange = { frac ->
                            val target = (frac * dur).toLong()
                            playerViewModel.seekTo(target)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 8.dp,
                        activeColor = Color.White,
                        inactiveColor = Color.White.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(S_PADDING))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatMillis(pos), style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatMillis(dur - pos),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(S_PADDING))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        VolumeSlider(
                            playerViewModel = playerViewModel,
                            modifier = Modifier
                                .align(alignment = Alignment.CenterStart)
                                .width(180.dp),
                            backgroundColor = bg,
                        )
                        PlayerControls(
                            modifier = Modifier.align(alignment = Alignment.Center),
                            isMiniPlayer = false,
                            isPlaying = isPlaying,
                            shuffle = shuffle,
                            repeat = repeat,
                            playerViewModel = playerViewModel
                        )
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                        ) {
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
                            Spacer(modifier = Modifier.width(L_PADDING * 2))
                            ToggledIconButton(
                                state = showEqualizer,
                                onClick = { showEqualizer = !showEqualizer },
                                activeBackground = Color.White,
                                inactiveBackground = Color.Transparent,
                            ) {
                                Icon(
                                    XAudioLines,
                                    contentDescription = "Equalizer",
                                    tint = if (showEqualizer) Color.Black else Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(L_PADDING * 2))
                            ToggledIconButton(
                                state = showQueue,
                                onClick = {
                                    if (!showQueue) {
                                        // If opening queue, close bluetooth sheet first
                                        if (showBluetoothSheet) {
                                            showBluetoothSheet = false
                                        }
                                    }
                                    showQueue = !showQueue
                                },
                                activeBackground = Color.White,
                                inactiveBackground = Color.Transparent,
                            ){
                                Icon(
                                    XListMusic,
                                    contentDescription = "Queue",
                                    tint = if (showQueue) Color.Black else Color.White
                                )
                            }
                        }
                    }


                }

            }
            Spacer(modifier = Modifier.height(S_PADDING))
        }
    }
}

/**
 * Audio-reactive glow container for tablet artwork with configurable visual effects.
 * 
 * @param dominantColor The primary color extracted from artwork for glow tinting
 * @param visualization Real-time audio analysis data (bass, mid, treble, stereo, beat)
 * @param config Glow effect configuration - use presets or customize
 * @param modifier Modifier for the container (must use graphicsLayer { clip = false })
 * @param content The artwork content to wrap with glow effect
 */
@Composable
fun GlowingArtworkContainerTablet(
    dominantColor: Color,
    visualization: AudioVisualizationData,
    config: GlowEffectConfig = GlowEffectConfig.Tablet,
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
                val artworkSizePx = with(density) { 400.dp.toPx() }
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

fun extractDominantColorTablet(bitmap: coil3.Bitmap): Color {
    val palette = Palette.from(bitmap).generate()
    val swatch =
        palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.dominantSwatch

    return swatch?.rgb?.let { Color(it) } ?: Color(0xFFFFFFFF)
}
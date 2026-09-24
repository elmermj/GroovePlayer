package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.domain.model.EqualizerState
import com.aethelsoft.grooveplayer.domain.usecase.equalizer_category.*
import com.aethelsoft.grooveplayer.presentation.player.EqualizerViewModel
import com.aethelsoft.grooveplayer.utils.theme.ui.ToggledTextButton
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import kotlinx.coroutines.CoroutineScope

/** Eats vertical drags so a parent bottom sheet does not move while a band slider is dragged. */
private val BandSliderScrollLock = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        return if (source == NestedScrollSource.UserInput) Offset(0f, available.y) else Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity = available
}

@Composable
fun EqualizerControlsComponent(
    modifier: Modifier = Modifier,
    viewModel: EqualizerViewModel = hiltViewModel(),
    isSimplified: Boolean = false,
    /**
     * Phone FullPlayer bottom sheet: same logic as Tablet, but changes save automatically (no Save button),
     * bands are at least 48dp wide and scroll sideways, dB sits above each slider and the frequency below.
     * [onSliderDragChange] lets the phone sheet ignore drags that start on a band.
     * Whenever the equalizer switch is off, band sliders (every variant) dim and ignore pointer input.
     */
    isPhoneSheet: Boolean = false,
    onSliderDragChange: (Boolean) -> Unit = {},
) {
    val equalizerState by viewModel.equalizerState.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Safe access to equalizer state
    LaunchedEffect(Unit) {
        try {
            // Trigger state update when component is first shown
            // This ensures the state is fresh
        } catch (e: Exception) {
            android.util.Log.e("EqualizerControls", "Error initializing equalizer UI: ${e.message}", e)
        }
    }
    
    if(isSimplified){
        SimplifiedEqualizerControlsComponent(
            modifier = modifier,
            viewModel = viewModel,
            equalizerState = equalizerState,
            hasUnsavedChanges = hasUnsavedChanges,
            scope = scope
        )
    } else {
        FullEqualizerControlsComponent(
            modifier = modifier,
            viewModel = viewModel,
            equalizerState = equalizerState,
            hasUnsavedChanges = hasUnsavedChanges,
            scope = scope,
            isPhoneSheet = isPhoneSheet,
            onSliderDragChange = onSliderDragChange,
        )
    }

    if (isPhoneSheet) {
        // Live apply is already handled by the ViewModel; persist shortly after the last change.
        LaunchedEffect(hasUnsavedChanges, equalizerState) {
            if (hasUnsavedChanges) {
                kotlinx.coroutines.delay(600)
                try {
                    viewModel.saveSettings()
                } catch (e: Exception) {
                    Log.e("EqualizerControls", "Error auto-saving settings: ${e.message}", e)
                }
            }
        }
    }
}

@Composable
private fun SimplifiedEqualizerControlsComponent(
    modifier: Modifier = Modifier,
    viewModel: EqualizerViewModel,
    equalizerState: EqualizerState,
    hasUnsavedChanges: Boolean,
    scope: CoroutineScope,
){
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title and Enable Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Equalizer",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    if (equalizerState.isEnabled) "On" else "Off",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (equalizerState.isEnabled) Color.White else Color.White.copy(alpha = 0.6f)
                )
                Switch(
                    checked = equalizerState.isEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            try {
                                viewModel.setEnabled(enabled)
                            } catch (e: Exception) {
                                Log.e("EqualizerControls", "Error setting enabled state: ${e.message}", e)
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.White.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        }

        if (!equalizerState.isAvailable) {
            // Equalizer not available
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Equalizer not available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Your device may not support audio effects",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (equalizerState.numberOfBands == 0) {
            // No bands available
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Initializing equalizer...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        } else {
            // Preset selector
            if (equalizerState.availablePresets.isNotEmpty()) {
                PresetSelector(
                    presets = equalizerState.availablePresets,
                    currentPreset = equalizerState.currentPreset,
                    onPresetSelected = { preset ->
                        scope.launch {
                            try {
                                viewModel.setPreset(preset)
                            } catch (e: Exception) {
                                Log.e("EqualizerControls", "Error setting preset: ${e.message}", e)
                            }
                        }
                    },
                    onReset = {
                        scope.launch {
                            try {
                                viewModel.reset()
                            } catch (e: Exception) {
                                Log.e("EqualizerControls", "Error resetting equalizer: ${e.message}", e)
                            }
                        }
                    },
                    isSimplified = true
                )
            }

            // Frequency bands stay visible but ignore drags while the equalizer is off.
            FrequencyBands(
                equalizerState = equalizerState,
                enabled = equalizerState.isEnabled,
                onBandLevelChanged = { band, level ->
                    scope.launch {
                        viewModel.setBandLevel(band, level)
                    }
                }
            )

            // Save Settings Button
            Spacer(modifier = Modifier.height(S_PADDING))
            ToggledTextButton(
                state = hasUnsavedChanges,
                onClick = {
                    scope.launch {
                        try {
                            viewModel.saveSettings()
                        } catch (e: Exception) {
                            Log.e("EqualizerControls", "Error saving settings: ${e.message}", e)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
                activeBackground = Color.White,
                inactiveBackground = Color.White.copy(alpha = 0.3f),
                activeTextColor = Color.Black,
                inactiveTextColor = Color.White.copy(alpha = 0.6f),
                text = "Save Settings",
                enabled = hasUnsavedChanges,
                shape = RoundedCornerShape(100.dp)
            )
        }
    }
}

@Composable
private fun FullEqualizerControlsComponent(
    modifier: Modifier = Modifier,
    viewModel: EqualizerViewModel,
    equalizerState: EqualizerState,
    hasUnsavedChanges: Boolean,
    scope: CoroutineScope,
    isPhoneSheet: Boolean = false,
    onSliderDragChange: (Boolean) -> Unit = {},
) {
    val resetEqualizer: () -> Unit = {
        scope.launch {
            try {
                viewModel.reset()
            } catch (e: Exception) {
                Log.e("EqualizerControls", "Error resetting equalizer: ${e.message}", e)
            }
        }
    }
    Column(
        modifier = modifier
            .then(if (isPhoneSheet) Modifier else Modifier.widthIn(max = 360.dp))
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title and Enable Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Equalizer",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = if (isPhoneSheet) FontWeight.Medium else FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isPhoneSheet) {
                    TextButton(onClick = resetEqualizer) {
                        Text(
                            "Reset",
                            color = GrooveTheme.colors.muted,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Text(
                    if (equalizerState.isEnabled) "On" else "Off",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (equalizerState.isEnabled) Color.White else GrooveTheme.colors.muted
                )
                Switch(
                    checked = equalizerState.isEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            try {
                                viewModel.setEnabled(enabled)
                            } catch (e: Exception) {
                                Log.e("EqualizerControls", "Error setting enabled state: ${e.message}", e)
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.White.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(S_PADDING))

        if (!equalizerState.isAvailable) {
            // Equalizer not available
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Equalizer not available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Your device may not support audio effects",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (equalizerState.numberOfBands == 0) {
            // No bands available
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Initializing equalizer...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
            // Preset selector. Phone sheet puts Reset in the header.
            if (equalizerState.availablePresets.isNotEmpty()) {
                PresetSelector(
                    presets = equalizerState.availablePresets,
                    currentPreset = equalizerState.currentPreset,
                    onPresetSelected = { preset ->
                        scope.launch {
                            try {
                                viewModel.setPreset(preset)
                            } catch (e: Exception) {
                                Log.e("EqualizerControls", "Error setting preset: ${e.message}", e)
                            }
                        }
                    },
                    onReset = resetEqualizer,
                    isSimplified = false,
                    showReset = !isPhoneSheet,
                    sheetStyle = isPhoneSheet,
                )
                Spacer(modifier = Modifier.height(S_PADDING))
            }

            // Presets and Reset stay usable. Only the bands dim and drop pointer input.
            FrequencyBands(
                equalizerState = equalizerState,
                enabled = equalizerState.isEnabled,
                onBandLevelChanged = { band, level ->
                    scope.launch {
                        viewModel.setBandLevel(band, level)
                    }
                },
                isPhoneSheet = isPhoneSheet,
                onSliderDragChange = onSliderDragChange,
            )
            }

            // Save Settings Button (Phone sheet auto-saves instead)
            if (!isPhoneSheet) {
            Spacer(modifier = Modifier.height(S_PADDING))
            ToggledTextButton(
                state = hasUnsavedChanges,
                onClick = {
                    scope.launch {
                        try {
                            viewModel.saveSettings()
                        } catch (e: Exception) {
                            Log.e("EqualizerControls", "Error saving settings: ${e.message}", e)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
                activeBackground = Color.White,
                inactiveBackground = Color.White.copy(alpha = 0.3f),
                activeTextColor = Color.Black,
                inactiveTextColor = Color.White.copy(alpha = 0.6f),
                text = "Save Settings",
                enabled = hasUnsavedChanges,
                shape = RoundedCornerShape(100.dp)
            )
            }
        }
    }
}

@Composable
private fun PresetSelector(
    presets: List<String>,
    currentPreset: Int,
    onPresetSelected: (Int) -> Unit,
    onReset: () -> Unit,
    isSimplified: Boolean,
    showReset: Boolean = true,
    sheetStyle: Boolean = false,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Presets",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            if (showReset && isSimplified){
                Box(
                    modifier = Modifier
                        .clickable(
                            onClick = onReset
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Reset",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else if (showReset) {
                TextButton(
                    onClick = onReset,
                ) {
                    Text(
                        "Reset",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

        }

        if(!isSimplified) {
            Spacer(modifier = Modifier.height(S_PADDING))
        }
        
        // Preset chips - horizontally scrollable
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            if (sheetStyle) GrooveTheme.colors.edgeGradient else Color.Black,
                            Color.Transparent,
                            Color.Transparent,
                            Color.Transparent,
                            Color.Transparent,
                            Color.Transparent,
                            Color.Transparent,
                            if (sheetStyle) GrooveTheme.colors.edgeGradient else Color.Black,
                        )
                    )
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEachIndexed { index, preset ->
                FilterChip(
                    selected = currentPreset == index,
                    onClick = { onPresetSelected(index) },
                    label = {
                        Text(
                            preset,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp
                        )
                    },
                    colors = presetChipColors(),
                    border = presetChipBorder(selected = currentPreset == index),
                    shape = if (sheetStyle) RoundedCornerShape(GrooveTheme.radii.chip) else RoundedCornerShape(100.dp)
                )
            }
            
            // Custom preset chip
            if (currentPreset == -1) {
                FilterChip(
                    selected = true,
                    onClick = { },
                    label = {
                        Text(
                            "Custom",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp
                        )
                    },
                    colors = presetChipColors(),
                    border = presetChipBorder(selected = true),
                    shape = if (sheetStyle) RoundedCornerShape(GrooveTheme.radii.chip) else RoundedCornerShape(100.dp)
                )
            }
        }
    }
}

@Composable
private fun presetChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Color.White,
    selectedLabelColor = Color.Black,
    containerColor = GrooveTheme.colors.inactive,
    labelColor = Color.White,
)

@Composable
private fun presetChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = Color.Transparent,
    selectedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    disabledSelectedBorderColor = Color.Transparent,
)

@Composable
private fun FrequencyBands(
    equalizerState: EqualizerState,
    onBandLevelChanged: (Int, Int) -> Unit,
    enabled: Boolean,
    isPhoneSheet: Boolean = false,
    onSliderDragChange: (Boolean) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else DisabledBandAlpha },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Frequency Bands",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(S_PADDING))
        
        Row(
            modifier = if (isPhoneSheet) {
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            } else {
                Modifier.fillMaxWidth()
            },
            horizontalArrangement = if (isPhoneSheet) Arrangement.spacedBy(4.dp) else Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(equalizerState.numberOfBands) { band ->
                FrequencyBandSlider(
                    enabled = enabled,
                    isPhoneSheet = isPhoneSheet,
                    onSliderDragChange = onSliderDragChange,
                    band = band,
                    frequency = equalizerState.bandFrequencies.getOrElse(band) { 0 },
                    level = equalizerState.bandLevels.getOrElse(band) { 0 },
                    levelRange = equalizerState.levelRange,
                    onLevelChanged = { level ->
                        try {
                            onBandLevelChanged(band, level)
                        } catch (e: Exception) {
                            Log.e("EqualizerControls", "Error changing band level: ${e.message}", e)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FrequencyBandSlider(
    enabled: Boolean,
    isPhoneSheet: Boolean = false,
    onSliderDragChange: (Boolean) -> Unit = {},
    band: Int,
    frequency: Int,
    level: Int,
    levelRange: Pair<Int, Int>,
    onLevelChanged: (Int) -> Unit
) {
    val (minLevel, maxLevel) = levelRange
    
    // Normalize level to 0-1 range where:
    // 0 = minLevel (bottom of slider)
    // 1 = maxLevel (top of slider)
    val normalizedLevel = ((level - minLevel).toFloat() / (maxLevel - minLevel).toFloat())
        .coerceIn(0f, 1f)
    
    // Slider value: 0 = bottom (minLevel), 1 = top (maxLevel)
    // This maps directly to the vertical position
    val sliderValue = normalizedLevel
    
    val bandWidth = if (isPhoneSheet) 56.dp else 40.dp
    Column(
        modifier = Modifier.width(bandWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top label: Tablet shows frequency, Phone sheet shows dB
        Text(
            text = if (isPhoneSheet) formatLevel(level) else formatFrequency(frequency),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        
        // Vertical slider
        Box(
            modifier = Modifier
                .width(bandWidth)
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
            
            // Active portion (from bottom to current level)
            // When level is at minLevel, activeHeight = 0 (bottom)
            // When level is at maxLevel, activeHeight = 200.dp (top)
            val activeHeight = 200.dp * normalizedLevel
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(activeHeight.coerceAtMost(200.dp))
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White)
            )
            
            // Center line indicator (0 dB) - calculate position based on 0 dB
            // 0 dB is at the center of the range: (0 - minLevel) / (maxLevel - minLevel)
//            val zeroDbNormalized = ((0 - minLevel).toFloat() / (maxLevel - minLevel).toFloat()).coerceIn(0f, 1f)
//            val zeroDbY = 200.dp * (1f - zeroDbNormalized) - 0.5.dp // Position from top
//            Box(
//                modifier = Modifier
//                    .width(40.dp)
//                    .height(1.dp)
//                    .offset(y = zeroDbY)
//                    .background(Color.White.copy(alpha = 0.5f))
//            )
            
            // Slider thumb (interactive area - full width for easier interaction)
            VerticalSlider(
                value = sliderValue,
                enabled = enabled,
                onValueChange = { newValue ->
                    // newValue is 0-1 where 0 = bottom (minLevel), 1 = top (maxLevel)
                    // Convert directly to millibels
                    val clampedValue = newValue.coerceIn(0f, 1f)
                    val newLevel = (clampedValue * (maxLevel - minLevel) + minLevel).toInt()
                    onLevelChanged(newLevel)
                },
                onDragActiveChange = onSliderDragChange,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(min = if (isPhoneSheet) 48.dp else 0.dp)
            )
        }
        
        // Bottom label: Tablet shows dB, Phone sheet shows frequency
        Text(
            text = if (isPhoneSheet) formatFrequency(frequency) else formatLevel(level),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onDragActiveChange: (Boolean) -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val currentOnValueChange = rememberUpdatedState(onValueChange)
    val currentOnDragActiveChange = rememberUpdatedState(onDragActiveChange)
    Box(
        modifier = modifier
            .then(if (enabled) Modifier.nestedScroll(BandSliderScrollLock) else Modifier)
            .pointerInput(enabled) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // Consume so a parent (e.g. the Phone EQ bottom sheet) doesn't treat this as a sheet drag.
                        down.consume()
                        if (!enabled) {
                            // Equalizer is off: swallow the gesture without moving the band.
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.first()
                                change.consume()
                                if (!change.pressed) break
                            }
                            continue
                        }
                        currentOnDragActiveChange.value(true)
                        try {
                            val height = size.height.toFloat().coerceAtLeast(1f)
                            // Convert Y position to slider value: 0 = bottom, 1 = top
                            currentOnValueChange.value((1f - (down.position.y / height)).coerceIn(0f, 1f))

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.first()

                                if (!change.pressed) {
                                    waitForUpOrCancellation()
                                    break
                                }

                                change.consume()
                                val currentHeight = size.height.toFloat().coerceAtLeast(1f)
                                currentOnValueChange.value((1f - (change.position.y / currentHeight)).coerceIn(0f, 1f))
                            }
                        } finally {
                            currentOnDragActiveChange.value(false)
                        }
                    }
                }
            }
    )
}

private const val DisabledBandAlpha = 0.38f

private fun formatFrequency(hz: Int): String {
    return when {
        hz >= 1000 -> "${hz / 1000}kHz"
        else -> "${hz}Hz"
    }
}

private fun formatLevel(millibels: Int): String {
    val db = millibels / 100f
    return when {
        db > 0 -> "+${"%.1f".format(db)}"
        db < 0 -> "${"%.1f".format(db)}"
        else -> "0.0"
    }
}

package com.aethelworks.grooveplayer.presentation.equalizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelworks.grooveplayer.domain.model.EqualizerState
import com.aethelworks.grooveplayer.presentation.common.GlassWhite
import com.aethelworks.grooveplayer.presentation.common.Hairline
import com.aethelworks.grooveplayer.presentation.common.Overline
import com.aethelworks.grooveplayer.presentation.equalizer.EqualizerViewModel
import com.aethelworks.grooveplayer.presentation.equalizer.formatBandFreq
import com.aethelworks.grooveplayer.presentation.equalizer.formatCenterFrequency
import com.aethelworks.grooveplayer.utils.theme.ui.PoppinsFontFamily
import com.aethelworks.grooveplayer.utils.theme.ui.ToggledTextButton
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.aethelworks.grooveplayer.utils.S_PADDING
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

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
            Log.e("EqualizerControls", "Error initializing equalizer UI: ${e.message}", e)
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
                delay(600)
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
    if (isPhoneSheet) {
        PhoneSheetEqualizer(
            modifier = modifier,
            equalizerState = equalizerState,
            onEnabled = { enabled ->
                scope.launch {
                    try {
                        viewModel.setEnabled(enabled)
                    } catch (e: Exception) {
                        Log.e("EqualizerControls", "Error setting enabled state: ${e.message}", e)
                    }
                }
            },
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
            onBandLevelChanged = { band, level ->
                scope.launch {
                    viewModel.setBandLevel(band, level)
                }
            },
            onSliderDragChange = onSliderDragChange,
        )
        return
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
private const val PhoneDisabledAlpha = 0.40f

private fun formatFrequency(milliHz: Int): String = formatCenterFrequency(milliHz)

private fun formatLevel(millibels: Int): String {
    val db = millibels / 100f
    return when {
        db > 0 -> "+${"%.1f".format(db)}"
        db < 0 -> "${"%.1f".format(db)}"
        else -> "0.0"
    }
}

@Composable
private fun PhoneSheetEqualizer(
    modifier: Modifier,
    equalizerState: EqualizerState,
    onEnabled: (Boolean) -> Unit,
    onPresetSelected: (Int) -> Unit,
    onReset: () -> Unit,
    onBandLevelChanged: (Int, Int) -> Unit,
    onSliderDragChange: (Boolean) -> Unit,
) {
    val bandsFlat = equalizerState.bandLevels.isEmpty() || equalizerState.bandLevels.all { it == 0 }
    val resetEnabled = equalizerState.isEnabled && !bandsFlat
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Equalizer",
                color = GlassWhite,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onReset,
                enabled = resetEnabled,
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = GlassWhite.copy(alpha = 0.70f),
                    disabledContentColor = GlassWhite.copy(alpha = 0.30f),
                ),
            ) {
                Text(
                    text = "Reset",
                    color = GlassWhite.copy(alpha = if (resetEnabled) 0.70f else 0.30f),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (equalizerState.isEnabled) "On" else "Off",
                color = GlassWhite.copy(alpha = 0.55f),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            )
            Spacer(Modifier.width(8.dp))
            FlatEqualizerToggle(
                checked = equalizerState.isEnabled,
                onCheckedChange = onEnabled,
            )
        }

        when {
            !equalizerState.isAvailable -> {
                Hairline()
                PhoneEqualizerMessage(
                    title = "Equalizer not available",
                    body = "Your device may not support audio effects",
                )
            }
            equalizerState.numberOfBands == 0 -> {
                Hairline()
                PhoneEqualizerMessage(title = "Initializing equalizer...")
            }
            else -> {
                if (equalizerState.availablePresets.isNotEmpty()) {
                    Hairline()
                    Overline(
                        text = "Presets",
                        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 12.dp),
                    )
                    PresetChipRow(
                        presets = equalizerState.availablePresets,
                        currentPreset = equalizerState.currentPreset,
                        enabled = equalizerState.isEnabled,
                        onPresetSelected = onPresetSelected,
                    )
                }
                Hairline()
                Overline(
                    text = "Frequency Bands",
                    modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 14.dp),
                )
                PhoneFrequencyBands(
                    equalizerState = equalizerState,
                    enabled = equalizerState.isEnabled,
                    onBandLevelChanged = onBandLevelChanged,
                    onSliderDragChange = onSliderDragChange,
                )
            }
        }
    }
}

@Composable
private fun PhoneEqualizerMessage(title: String, body: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = GlassWhite.copy(alpha = 0.65f),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                color = GlassWhite.copy(alpha = 0.55f),
                fontFamily = PoppinsFontFamily,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FlatEqualizerToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val travel by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "eqToggle",
    )
    val thumb = if (checked) 18.dp else 14.dp
    val outline = if (checked) GlassWhite else GlassWhite.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .size(44.dp, 26.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .border(1.5.dp, outline, CircleShape)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val span = 44.dp - 8.dp - thumb
        Box(
            modifier = Modifier
                .offset(x = span * travel)
                .size(thumb)
                .background(
                    if (checked) GlassWhite else GlassWhite.copy(alpha = 0.55f),
                    CircleShape,
                ),
        )
    }
}

@Composable
private fun PresetChipRow(
    presets: List<String>,
    currentPreset: Int,
    enabled: Boolean,
    onPresetSelected: (Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else PhoneDisabledAlpha },
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(presets) { index, preset ->
            PresetPill(
                label = preset,
                selected = currentPreset == index,
                enabled = enabled,
                onClick = { onPresetSelected(index) },
            )
        }
        if (currentPreset == -1) {
            item {
                PresetPill(
                    label = "Custom",
                    selected = true,
                    enabled = enabled,
                    onClick = {},
                )
            }
        }
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun PresetPill(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(GlassWhite)
                } else {
                    Modifier.border(1.dp, GlassWhite.copy(alpha = 0.25f), shape)
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else GlassWhite.copy(alpha = 0.80f),
            fontFamily = PoppinsFontFamily,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun PhoneFrequencyBands(
    equalizerState: EqualizerState,
    enabled: Boolean,
    onBandLevelChanged: (Int, Int) -> Unit,
    onSliderDragChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .graphicsLayer { alpha = if (enabled) 1f else PhoneDisabledAlpha },
        verticalAlignment = Alignment.Top,
    ) {
        repeat(equalizerState.numberOfBands) { band ->
            PhoneBandColumn(
                modifier = Modifier.weight(1f),
                enabled = enabled,
                frequencyMilliHz = equalizerState.bandFrequencies.getOrElse(band) { 0 },
                level = equalizerState.bandLevels.getOrElse(band) { 0 },
                levelRange = equalizerState.levelRange,
                onLevelChanged = { level ->
                    try {
                        onBandLevelChanged(band, level)
                    } catch (e: Exception) {
                        Log.e("EqualizerControls", "Error changing band level: ${e.message}", e)
                    }
                },
                onSliderDragChange = onSliderDragChange,
            )
        }
    }
}

@Composable
private fun PhoneBandColumn(
    modifier: Modifier,
    enabled: Boolean,
    frequencyMilliHz: Int,
    level: Int,
    levelRange: Pair<Int, Int>,
    onLevelChanged: (Int) -> Unit,
    onSliderDragChange: (Boolean) -> Unit,
) {
    val (minLevel, maxLevel) = levelRange
    val hz = frequencyMilliHz / 1000
    val dbLabel = formatLevel(level)
    val description = "${formatBandFreq(hz)} band, $dbLabel decibels"
    val haptic = LocalHapticFeedback.current
    var lastLevel by remember { mutableIntStateOf(level) }
    var pressed by remember { mutableStateOf(false) }
    val span = (maxLevel - minLevel).takeIf { it != 0 } ?: 1
    val normalized = ((level - minLevel).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    val zeroNorm = ((0 - minLevel).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    val trackHeight = 150.dp
    val thumbSize by animateDpAsState(
        targetValue = if (pressed) 18.dp else 14.dp,
        animationSpec = tween(durationMillis = 150),
        label = "eqThumb",
    )

    Column(
        modifier = modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(
                current = level.toFloat(),
                range = minLevel.toFloat()..maxLevel.toFloat(),
            )
            contentDescription = description
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = dbLabel,
            color = if (level == 0) GlassWhite.copy(alpha = 0.55f) else GlassWhite,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight),
            contentAlignment = Alignment.Center,
        ) {
            val zeroFromTop = trackHeight * (1f - zeroNorm)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = zeroFromTop)
                    .background(GlassWhite.copy(alpha = 0.08f)),
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(GlassWhite.copy(alpha = 0.15f)),
            )
            val thumbFromTop = trackHeight * (1f - normalized)
            val fillTop = minOf(zeroFromTop, thumbFromTop)
            val fillHeight = (maxOf(zeroFromTop, thumbFromTop) - fillTop).coerceAtLeast(0.dp)
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(fillHeight)
                    .align(Alignment.TopCenter)
                    .offset(y = fillTop)
                    .background(GlassWhite),
            )
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .align(Alignment.TopCenter)
                    .offset(y = thumbFromTop - thumbSize / 2)
                    .background(GlassWhite, CircleShape),
            )
            VerticalSlider(
                value = normalized,
                enabled = enabled,
                onValueChange = { newValue ->
                    val newLevel = (newValue.coerceIn(0f, 1f) * (maxLevel - minLevel) + minLevel).toInt()
                    val prev = lastLevel
                    if ((prev > 0 && newLevel <= 0) || (prev < 0 && newLevel >= 0)) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    lastLevel = newLevel
                    onLevelChanged(newLevel)
                },
                onDragActiveChange = {
                    pressed = it
                    onSliderDragChange(it)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = formatBandFreq(hz),
            color = GlassWhite.copy(alpha = 0.55f),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

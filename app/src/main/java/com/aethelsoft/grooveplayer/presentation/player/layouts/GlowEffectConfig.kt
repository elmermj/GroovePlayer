package com.aethelsoft.grooveplayer.presentation.player.layouts

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration for the audio-reactive glow effect around artwork.
 * All size-related multipliers are relative to the artwork dimensions.
 * 
 * The glow effect consists of three layered blurs (bass, mid, treble) that respond
 * to different frequency bands and create a rich, dynamic visualization.
 */
data class GlowEffectConfig(
    /**
     * Base blur radius as a fraction of artwork size.
     * 
     * This is the minimum blur radius when no audio is playing.
     * - Range: 0.2f to 1.5f
     * - Default: 0.65f (65% of artwork dimension)
     * - Lower values = tighter glow
     * - Higher values = more diffused glow
     * 
     * Example:
     * - 0.3f = Tight, close to artwork
     * - 0.65f = Moderate spread (default)
     * - 1.2f = Wide, dramatic spread
     */
    val baseBlurMultiplier: Float = 0.65f,
    
    /**
     * Maximum additional blur from bass frequencies (20-250 Hz).
     * 
     * Controls how much the glow expands when bass is detected.
     * Applied as: `artworkSize * bassExpansionMultiplier * bassIntensity`
     * - Range: 0.1f to 1.5f
     * - Default: 0.55f (up to 55% expansion)
     * - Lower values = subtle bass response
     * - Higher values = explosive bass glow
     * 
     * Example:
     * - 0.2f = Minimal bass pulse
     * - 0.55f = Moderate bass impact (default)
     * - 1.2f = Massive bass explosion
     */
    val bassExpansionMultiplier: Float = 0.55f,
    
    /**
     * Blur reduction from treble frequencies (4000-20000 Hz).
     * 
     * High frequencies tighten the glow for definition/sharpness.
     * Applied as: `-artworkSize * trebleTightnessMultiplier * trebleIntensity`
     * - Range: 0.0f to 0.5f
     * - Default: 0.15f (up to 15% reduction)
     * - 0.0f = treble has no tightening effect
     * - Higher values = treble creates sharper, more defined glow
     * 
     * Example:
     * - 0.0f = Ignore treble (always soft glow)
     * - 0.15f = Subtle sharpening (default)
     * - 0.4f = Strong sharpening on high notes
     */
    val trebleTightnessMultiplier: Float = 0.15f,
    
    /**
     * Additional blur pulse on detected beats.
     * 
     * Creates expansion bursts when drums/percussion hits are detected.
     * Applied as: `artworkSize * beatExpansionMultiplier * beatIntensity`
     * - Range: 0.0f to 1.0f
     * - Default: 0.25f (25% expansion on beats)
     * - 0.0f = no beat response
     * - Higher values = more dramatic beat pulses
     * 
     * Example:
     * - 0.0f = Smooth, no beat reaction
     * - 0.25f = Visible beat pulse (default)
     * - 0.8f = Explosive beat response
     */
    val beatExpansionMultiplier: Float = 0.25f,
    
    /**
     * Minimum blur radius as a fraction of artwork size.
     * 
     * Prevents glow from becoming too small/disappearing.
     * - Range: 0.05f to 0.5f
     * - Default: 0.3f (30% minimum)
     * - Lower values = glow can get very tight
     * - Higher values = always maintains visible glow
     * 
     * Example:
     * - 0.1f = Can become very subtle
     * - 0.3f = Always noticeable (default)
     * - 0.45f = Always prominent
     */
    val minBlurMultiplier: Float = 0.3f,
    
    /**
     * Minimum glow opacity (alpha channel).
     * 
     * Base transparency level when audio is quiet.
     * - Range: 0.0f to 0.6f
     * - Default: 0.35f (35% opacity)
     * - Lower values = more transparent when quiet
     * - Higher values = always visible even when quiet
     * 
     * Example:
     * - 0.05f = Nearly invisible when quiet
     * - 0.35f = Subtly visible (default)
     * - 0.55f = Always prominent
     */
    val minAlpha: Float = 0.35f,
    
    /**
     * Maximum glow opacity (alpha channel).
     * 
     * Peak transparency level during loud passages.
     * - Range: 0.4f to 1.0f
     * - Default: 0.85f (85% opacity)
     * - Lower values = more subtle even when loud
     * - Higher values = very intense at peak
     * - 1.0f = fully opaque (may overwhelm artwork)
     * 
     * Example:
     * - 0.5f = Subtle, never overwhelming
     * - 0.85f = Strong but not overpowering (default)
     * - 1.0f = Maximum intensity, fully opaque
     */
    val maxAlpha: Float = 0.85f,
    
    /**
     * Opacity contribution from overall audio intensity.
     * 
     * How much louder audio increases glow brightness.
     * Applied as: `minAlpha + (intensityAlphaRange * audioIntensity)`
     * - Range: 0.1f to 0.9f
     * - Default: 0.45f
     * - Lower values = less variation in brightness
     * - Higher values = dramatic brightness changes
     * 
     * Example:
     * - 0.15f = Subtle brightness variation
     * - 0.45f = Moderate variation (default)
     * - 0.8f = Extreme quiet-to-loud changes
     */
    val intensityAlphaRange: Float = 0.45f,
    
    /**
     * Opacity boost from beat detection.
     * 
     * Additional brightness flash when beats are detected.
     * - Range: 0.0f to 0.5f
     * - Default: 0.2f (20% boost on beats)
     * - 0.0f = no beat brightness flash
     * - Higher values = more dramatic beat flashes
     * 
     * Example:
     * - 0.0f = Smooth, no flashing
     * - 0.2f = Subtle beat flash (default)
     * - 0.45f = Strong strobe-like beat flash
     */
    val beatAlphaBoost: Float = 0.2f,
    
    /**
     * Relative opacity of the bass frequency layer.
     * 
     * Multiplier for bass layer transparency.
     * - Range: 0.1f to 1.0f
     * - Default: 0.6f (60% of calculated alpha)
     * - Lower values = bass layer more subtle
     * - Higher values = bass layer more prominent
     * 
     * Note: Bass layer uses a deeper, blue-shifted color
     */
    val bassLayerAlpha: Float = 0.6f,
    
    /**
     * Relative opacity of the mid/voice frequency layer.
     * 
     * Multiplier for mid layer transparency.
     * - Range: 0.2f to 1.0f
     * - Default: 0.8f (80% of calculated alpha)
     * - Lower values = vocals/mids more subtle
     * - Higher values = vocals/mids more dominant
     * 
     * Note: Mid layer uses the artwork's dominant color
     */
    val midLayerAlpha: Float = 0.8f,
    
    /**
     * Relative opacity of the treble frequency layer.
     * 
     * Multiplier for treble layer transparency.
     * - Range: 0.1f to 1.0f
     * - Default: 0.5f (50% of calculated alpha)
     * - Lower values = treble shimmer very subtle
     * - Higher values = bright treble sparkle
     * 
     * Note: Treble layer uses a brightened/whitened color
     */
    val trebleLayerAlpha: Float = 0.5f,
    
    /**
     * Blur size multiplier for bass frequency layer.
     * 
     * How much larger/smaller the bass blur is vs base blur.
     * Applied as: `blurRadius * bassLayerBlurScale`
     * - Range: 0.6f to 2.0f
     * - Default: 1.2f (20% larger)
     * - Lower values = tighter bass layer
     * - Higher values = more diffused bass layer
     * 
     * Larger values create a "halo" effect
     */
    val bassLayerBlurScale: Float = 1.2f,
    
    /**
     * Blur size multiplier for mid frequency layer.
     * 
     * How much larger/smaller the mid blur is vs base blur.
     * Applied as: `blurRadius * midLayerBlurScale`
     * - Range: 0.5f to 1.5f
     * - Default: 0.85f (15% smaller)
     * - This is typically the "main" visible layer
     */
    val midLayerBlurScale: Float = 0.85f,
    
    /**
     * Blur size multiplier for treble frequency layer.
     * 
     * How much larger/smaller the treble blur is vs base blur.
     * Applied as: `blurRadius * trebleLayerBlurScale`
     * - Range: 0.2f to 1.2f
     * - Default: 0.5f (50% of base - tight/sharp)
     * - Lower values = very sharp treble accent
     * - Higher values = softer treble layer
     * 
     * Small values create a bright "core" effect
     */
    val trebleLayerBlurScale: Float = 0.5f,
    
    /**
     * Color shift intensity for bass frequencies.
     * 
     * How much to shift toward blue/deeper tones for bass.
     * - Range: 0.3f to 1.0f for red reduction
     * - Range: 1.0f to 2.0f for blue boost
     * - Default: 0.8f red, 1.2f blue
     * 
     * Creates depth and "weight" to bass frequencies
     */
    val bassColorRedMultiplier: Float = 0.8f,
    val bassColorBlueMultiplier: Float = 1.2f,
    
    /**
     * Brightness boost for treble frequencies.
     * 
     * How much white to add to treble layer for sparkle effect.
     * Applied as: `color + trebleColorBoost` per RGB channel
     * - Range: 0.0f to 0.8f
     * - Default: 0.3f (30% brighter)
     * - Lower values = subtle brightening
     * - Higher values = intense white shimmer
     * 
     * Creates "sparkle" and "air" on high frequencies
     */
    val trebleColorBoost: Float = 0.3f,
    
    /**
     * Maximum horizontal extension for bass layer based on stereo.
     * 
     * Controls how much the bass glow extends left/right based on stereo balance.
     * When stereo = 0 (center), extends equally both sides.
     * When stereo = -1 (full left), extends maximally to the left.
     * When stereo = +1 (full right), extends maximally to the right.
     * 
     * - Range: 0.0f to 0.8f
     * - Default: 0.25f (25% extension per side at center, up to 50% when panned)
     * - 0.0f = no horizontal extension
     * - Higher values = more dramatic stereo width
     * 
     * Creates immersive stereo field visualization
     */
    val bassHorizontalExtension: Float = 0.25f,
    
    /**
     * Stereo balance influence on treble layer horizontal position.
     * 
     * How much left/right audio balance shifts the treble layer.
     * Note: Treble shifts opposite direction from bass for shimmer
     * - Range: 0.0f to 0.4f
     * - Default: 0.1f (10% horizontal shift, opposite bass)
     * - 0.0f = no stereo movement
     * - Higher values = more shimmer/separation
     */
    val stereoBalanceTrebleScale: Float = 0.1f,
    
    /**
     * Corner radius for the glow shape.
     * 
     * Matches the artwork corner radius for visual consistency.
     * - Typical values: 12.dp (phone), 16.dp (tablet), 20.dp (large tablet)
     */
    val cornerRadius: Dp = 16.dp,
    
    /**
     * Minimum bass intensity threshold to render bass layer.
     * 
     * Avoids rendering bass layer when bass is negligible (saves GPU).
     * - Range: 0.0f to 0.3f
     * - Default: 0.1f (10% bass needed to show layer)
     * - 0.0f = always render (maximum quality, lower performance)
     */
    val bassRenderThreshold: Float = 0.1f,
    
    /**
     * Minimum mid intensity threshold to render mid layer.
     * 
     * Avoids rendering mid layer when mids are negligible.
     * - Range: 0.0f to 0.3f
     * - Default: 0.1f (10% mid needed to show layer)
     * - 0.0f = always render (maximum quality, lower performance)
     */
    val midRenderThreshold: Float = 0.1f,
    
    /**
     * Minimum treble intensity threshold to render treble layer.
     * 
     * Avoids rendering treble layer when treble is negligible.
     * - Range: 0.0f to 0.4f
     * - Default: 0.15f (15% treble needed to show layer)
     * - 0.0f = always render (maximum quality, lower performance)
     */
    val trebleRenderThreshold: Float = 0.15f,
    
    /**
     * Enable white beat flash layer.
     * 
     * Adds a bright white flash on detected beats for visual impact.
     * - Default: true
     * - Set to false to disable beat flash layer (saves GPU)
     */
    val enableBeatFlash: Boolean = true,
    
    /**
     * Opacity of the white beat flash layer.
     * 
     * Controls how bright the white flash appears on beats.
     * - Range: 0.2f to 1.0f
     * - Default: 0.7f (70% opacity at full beat)
     * - Higher values = more intense white flash
     * - Lower values = subtle beat accent
     */
    val beatFlashAlpha: Float = 0.7f,
    
    /**
     * Blur size multiplier for white beat flash.
     * 
     * Controls how diffused the beat flash appears.
     * - Range: 0.4f to 1.5f
     * - Default: 0.9f (90% of base blur)
     * - Lower values = tight, sharp flash
     * - Higher values = soft, diffused flash
     */
    val beatFlashBlurScale: Float = 0.9f,
    
    /**
     * Minimum beat intensity to render beat flash.
     * 
     * Avoids rendering beat layer for weak beats.
     * - Range: 0.0f to 0.6f
     * - Default: 0.2f (20% beat needed for flash)
     * - Lower values = more frequent flashes
     * - Higher values = only strong beats flash
     * - 0.0f = always flash (may be overwhelming)
     */
    val beatFlashThreshold: Float = 0.2f
) {
    companion object {
        /**
         * Default configuration for phone-sized screens.
         * Optimized for smaller displays with moderate glow.
         */
        val Phone = GlowEffectConfig(
            baseBlurMultiplier = 0.65f,
            bassExpansionMultiplier = 0.55f,
            beatExpansionMultiplier = 0.25f,
            minAlpha = 0.35f,
            maxAlpha = 0.85f,
            cornerRadius = 12.dp
        )
        
        /**
         * Default configuration for tablet-sized screens.
         * Balanced between visibility and subtlety.
         */
        val Tablet = GlowEffectConfig(
            baseBlurMultiplier = 0.65f,
            bassExpansionMultiplier = 0.55f,
            beatExpansionMultiplier = 0.25f,
            minAlpha = 0.35f,
            maxAlpha = 0.85f,
            cornerRadius = 16.dp
        )
        
        /**
         * Default configuration for large tablet screens.
         * More dramatic effect for larger viewing distances.
         */
        val LargeTablet = GlowEffectConfig(
            baseBlurMultiplier = 0.7f,
            bassExpansionMultiplier = 0.6f,
            beatExpansionMultiplier = 0.3f,
            minAlpha = 0.4f,
            maxAlpha = 0.9f,
            cornerRadius = 20.dp
        )
        
        /**
         * Subtle/minimal glow preset.
         * For users who prefer understated effects.
         */
        val Subtle = GlowEffectConfig(
            baseBlurMultiplier = 0.45f,
            bassExpansionMultiplier = 0.35f,
            trebleTightnessMultiplier = 0.1f,
            beatExpansionMultiplier = 0.15f,
            minAlpha = 0.25f,
            maxAlpha = 0.65f,
            bassLayerAlpha = 0.5f,
            midLayerAlpha = 0.7f,
            trebleLayerAlpha = 0.4f
        )
        
        /**
         * Dramatic/intense glow preset.
         * Maximum visual impact for immersive experience.
         */
        val Dramatic = GlowEffectConfig(
            baseBlurMultiplier = 1.1f,
            bassExpansionMultiplier = 1.0f,
            trebleTightnessMultiplier = 0.25f,
            beatExpansionMultiplier = 0.65f,
            minBlurMultiplier = 0.2f,
            minAlpha = 0.5f,
            maxAlpha = 0.98f,
            intensityAlphaRange = 0.7f,
            beatAlphaBoost = 0.4f,
            bassLayerAlpha = 0.85f,
            midLayerAlpha = 0.95f,
            trebleLayerAlpha = 0.7f,
            bassLayerBlurScale = 1.6f,
            bassHorizontalExtension = 0.35f,
            stereoBalanceTrebleScale = 0.25f,
            beatFlashAlpha = 0.85f,
            beatFlashThreshold = 0.15f
        )
        
        /**
         * Bass-focused preset.
         * Emphasizes low frequencies for bass-heavy music.
         */
        val BassHeavy = GlowEffectConfig(
            baseBlurMultiplier = 0.8f,
            bassExpansionMultiplier = 1.2f,
            beatExpansionMultiplier = 0.7f,
            minAlpha = 0.4f,
            maxAlpha = 0.95f,
            bassLayerAlpha = 0.95f,
            bassLayerBlurScale = 1.7f,
            bassColorBlueMultiplier = 1.6f,
            bassColorRedMultiplier = 0.6f,
            bassHorizontalExtension = 0.4f,
            midLayerAlpha = 0.5f,
            trebleLayerAlpha = 0.25f,
            stereoBalanceTrebleScale = 0.2f,
            beatFlashAlpha = 0.8f,
            beatFlashThreshold = 0.18f
        )
        
        /**
         * Extreme glow preset - pushes all limits.
         * Uses the full expanded dynamic range for maximum impact.
         * Warning: Very intense, may overwhelm artwork!
         */
        val Extreme = GlowEffectConfig(
            baseBlurMultiplier = 1.4f,
            bassExpansionMultiplier = 1.4f,
            trebleTightnessMultiplier = 0.4f,
            beatExpansionMultiplier = 0.9f,
            minBlurMultiplier = 0.1f,
            minAlpha = 0.55f,
            maxAlpha = 1.0f,
            intensityAlphaRange = 0.85f,
            beatAlphaBoost = 0.45f,
            bassLayerAlpha = 1.0f,
            midLayerAlpha = 1.0f,
            trebleLayerAlpha = 0.9f,
            bassLayerBlurScale = 1.9f,
            midLayerBlurScale = 1.3f,
            trebleLayerBlurScale = 0.8f,
            bassColorRedMultiplier = 0.4f,
            bassColorBlueMultiplier = 1.8f,
            trebleColorBoost = 0.6f,
            bassHorizontalExtension = 0.5f,
            stereoBalanceTrebleScale = 0.35f,
            enableBeatFlash = true,
            beatFlashAlpha = 0.95f,
            beatFlashBlurScale = 1.2f,
            beatFlashThreshold = 0.1f,
            bassRenderThreshold = 0.0f,
            midRenderThreshold = 0.0f,
            trebleRenderThreshold = 0.0f
        )
    }
}

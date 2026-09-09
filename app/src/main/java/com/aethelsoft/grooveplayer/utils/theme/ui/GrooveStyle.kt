package com.aethelsoft.grooveplayer.utils.theme.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.json.JSONObject

object GrooveStyleIds {
    const val DEFAULT = "default"
    const val MIDNIGHT = "midnight"
    const val GRAPHITE = "graphite"
    const val HIGH_CONTRAST = "high_contrast"
    const val WOODLAND = "woodland"
    const val DESERT = "desert"
    const val ECLIPSE_BLUE = "eclipse_blue"
    const val EMBER = "ember"
    const val OCEAN = "ocean"
    const val LAVENDER = "lavender"
    const val CUSTOM = "custom"
}

@Immutable
data class GrooveColors(
    val canvas: Color,
    /** Top-bar / mini-player edge glass — must stay distinct from [canvas]. */
    val edgeGradient: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val onSurface: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val inactive: Color,
    val inactiveContainer: Color,
    val brandSecondary: Color,
    val brandTertiary: Color,
    val warning: Color,
    val error: Color,
    val sliderTrack: Color,
    val sliderFill: Color,
)

@Immutable
data class GrooveRadii(
    val card: Dp,
    val button: Dp,
    val artwork: Dp,
    val chip: Dp,
) {
    val cardShape get() = RoundedCornerShape(card)
    val buttonShape get() = RoundedCornerShape(button)
    val chipShape get() = RoundedCornerShape(chip)
}

@Immutable
data class GrooveSpacing(
    val xs: Dp,
    val s: Dp,
    val m: Dp,
    val l: Dp,
    val appBarHeight: Dp,
    val cardPadding: Dp = m,
    val listItemSpacing: Dp = s,
    val sectionSpacing: Dp = m,
    val buttonMinHeight: Dp = 48.dp,
)

@Immutable
data class GrooveIconSizes(
    val sm: Dp = 18.dp,
    val md: Dp = 24.dp,
    val lg: Dp = 28.dp,
    val xl: Dp = 36.dp,
)

@Immutable
data class GrooveStyle(
    val id: String,
    val displayName: String,
    val colors: GrooveColors,
    val radii: GrooveRadii,
    val spacing: GrooveSpacing,
    val typography: GrooveTypographyRoles = GrooveTypographyRoles.Default,
    val iconSizes: GrooveIconSizes = GrooveIconSizes(),
) {
    fun toColorScheme(): ColorScheme = darkColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        primaryContainer = colors.surface,
        onPrimaryContainer = colors.muted,
        secondary = colors.brandSecondary,
        onSecondary = colors.muted,
        secondaryContainer = colors.surfaceRaised,
        onSecondaryContainer = colors.muted,
        tertiary = colors.brandTertiary,
        onTertiary = colors.muted,
        tertiaryContainer = colors.inactiveContainer,
        onTertiaryContainer = colors.muted,
        background = colors.canvas,
        onBackground = colors.onSurface,
        surface = colors.surface,
        onSurface = colors.onSurface,
        surfaceVariant = colors.surfaceRaised,
        onSurfaceVariant = colors.muted,
        surfaceDim = colors.canvas,
        surfaceBright = colors.surface,
        surfaceContainerLowest = colors.canvas,
        surfaceContainerLow = colors.surfaceRaised,
        surfaceContainer = colors.surface,
        surfaceContainerHigh = colors.surface,
        surfaceContainerHighest = colors.inactive,
        inverseSurface = colors.muted,
        inverseOnSurface = colors.surface,
        inversePrimary = colors.surface,
        outline = colors.muted.copy(alpha = 0.35f),
        outlineVariant = colors.muted.copy(alpha = 0.18f),
        error = colors.error,
        onError = colors.onSurface,
        errorContainer = Color(0xFF5C1A1A),
        onErrorContainer = colors.muted,
        scrim = colors.canvas,
    )
}

/**
 * Encodes / decodes a full [GrooveStyle] snapshot for custom saves.
 * Stored in [UserSettings.uiStyleOverrides] when the user confirms the styling screen.
 */
object GrooveStyleCodec {
    fun encode(style: GrooveStyle): String {
        fun Color.hex() = toArgb().toUInt().toString()
        fun Dp.v() = value
        fun GrooveTypeRole.json(prefix: String, o: JSONObject) {
            o.put("${prefix}Size", fontSizeSp)
            o.put("${prefix}Weight", fontWeight)
            o.put("${prefix}Tracking", letterSpacingSp)
            lineHeightSp?.let { o.put("${prefix}Line", it) }
        }
        return JSONObject().apply {
            put("v", 1)
            put("id", style.id)
            put("name", style.displayName)
            put("canvas", style.colors.canvas.hex())
            put("edgeGradient", style.colors.edgeGradient.hex())
            put("surface", style.colors.surface.hex())
            put("raised", style.colors.surfaceRaised.hex())
            put("onSurface", style.colors.onSurface.hex())
            put("muted", style.colors.muted.hex())
            put("accent", style.colors.accent.hex())
            put("onAccent", style.colors.onAccent.hex())
            put("inactive", style.colors.inactive.hex())
            put("inactiveContainer", style.colors.inactiveContainer.hex())
            put("brandSecondary", style.colors.brandSecondary.hex())
            put("brandTertiary", style.colors.brandTertiary.hex())
            put("warning", style.colors.warning.hex())
            put("error", style.colors.error.hex())
            put("sliderTrack", style.colors.sliderTrack.hex())
            put("sliderFill", style.colors.sliderFill.hex())
            put("cardR", style.radii.card.v())
            put("btnR", style.radii.button.v())
            put("artR", style.radii.artwork.v())
            put("chipR", style.radii.chip.v())
            put("xs", style.spacing.xs.v())
            put("s", style.spacing.s.v())
            put("m", style.spacing.m.v())
            put("l", style.spacing.l.v())
            put("appBar", style.spacing.appBarHeight.v())
            put("cardPad", style.spacing.cardPadding.v())
            put("listGap", style.spacing.listItemSpacing.v())
            put("sectionGap", style.spacing.sectionSpacing.v())
            put("btnMinH", style.spacing.buttonMinHeight.v())
            put("iconSm", style.iconSizes.sm.v())
            put("iconMd", style.iconSizes.md.v())
            put("iconLg", style.iconSizes.lg.v())
            put("iconXl", style.iconSizes.xl.v())
            put(
                "typeScale",
                GrooveTypographyScale.matching(style.typography)?.id
                    ?: GrooveTypographyScale.Medium.id,
            )
            style.typography.pageTitle.json("pageTitle", this)
            style.typography.sectionTitle.json("sectionTitle", this)
            style.typography.sectionItemTitle.json("itemTitle", this)
            style.typography.sectionItemSubtitle.json("itemSub", this)
            style.typography.menuSongTitle.json("menuTitle", this)
            style.typography.menuSongArtist.json("menuArtist", this)
            style.typography.menuSongAlbum.json("menuAlbum", this)
            style.typography.playerSongTitle.json("playerTitle", this)
            style.typography.playerSongArtist.json("playerArtist", this)
            style.typography.playerSongAlbum.json("playerAlbum", this)
            style.typography.miniPlayerSongTitle.json("miniTitle", this)
            style.typography.miniPlayerSongArtist.json("miniArtist", this)
            style.typography.buttonLabel.json("btnLabel", this)
            style.typography.cardTitle.json("cardTitle", this)
            style.typography.cardSubtitle.json("cardSub", this)
            style.typography.body.json("body", this)
        }.toString()
    }

    fun decode(raw: String?): GrooveStyle? {
        if (raw.isNullOrBlank() || !raw.trimStart().startsWith("{")) return null
        return try {
            val o = JSONObject(raw)
            fun color(key: String, fallback: Color): Color {
                val v = o.optString(key, "")
                if (v.isBlank()) return fallback
                return Color(v.toUInt(10).toInt())
            }
            fun dp(key: String, fallback: Dp): Dp {
                if (!o.has(key)) return fallback
                return o.getDouble(key).toFloat().dp
            }
            fun role(prefix: String, fallback: GrooveTypeRole): GrooveTypeRole {
                if (!o.has("${prefix}Size")) return fallback
                return GrooveTypeRole(
                    fontSizeSp = o.getDouble("${prefix}Size").toFloat(),
                    fontWeight = o.optInt("${prefix}Weight", fallback.fontWeight),
                    letterSpacingSp = o.optDouble("${prefix}Tracking", fallback.letterSpacingSp.toDouble()).toFloat(),
                    lineHeightSp = if (o.has("${prefix}Line")) o.getDouble("${prefix}Line").toFloat() else fallback.lineHeightSp,
                )
            }
            val base = GrooveStyleCatalog.Default
            GrooveStyle(
                id = o.optString("id", GrooveStyleIds.CUSTOM),
                displayName = o.optString("name", "Custom"),
                colors = base.colors.copy(
                    canvas = color("canvas", base.colors.canvas),
                    edgeGradient = color("edgeGradient", base.colors.edgeGradient),
                    surface = color("surface", base.colors.surface),
                    surfaceRaised = color("raised", base.colors.surfaceRaised),
                    onSurface = color("onSurface", base.colors.onSurface),
                    muted = color("muted", base.colors.muted),
                    accent = color("accent", base.colors.accent),
                    onAccent = color("onAccent", base.colors.onAccent),
                    inactive = color("inactive", base.colors.inactive),
                    inactiveContainer = color("inactiveContainer", base.colors.inactiveContainer),
                    brandSecondary = color("brandSecondary", base.colors.brandSecondary),
                    brandTertiary = color("brandTertiary", base.colors.brandTertiary),
                    warning = color("warning", base.colors.warning),
                    error = color("error", base.colors.error),
                    sliderTrack = color("sliderTrack", base.colors.sliderTrack),
                    sliderFill = color("sliderFill", base.colors.sliderFill),
                ),
                radii = GrooveRadii(
                    card = dp("cardR", base.radii.card),
                    button = dp("btnR", base.radii.button),
                    artwork = dp("artR", base.radii.artwork),
                    chip = dp("chipR", base.radii.chip),
                ),
                spacing = GrooveSpacing(
                    xs = dp("xs", base.spacing.xs),
                    s = dp("s", base.spacing.s),
                    m = dp("m", base.spacing.m),
                    l = dp("l", base.spacing.l),
                    appBarHeight = dp("appBar", base.spacing.appBarHeight),
                    cardPadding = dp("cardPad", base.spacing.cardPadding),
                    listItemSpacing = dp("listGap", base.spacing.listItemSpacing),
                    sectionSpacing = dp("sectionGap", base.spacing.sectionSpacing),
                    buttonMinHeight = dp("btnMinH", base.spacing.buttonMinHeight),
                ),
                iconSizes = GrooveIconSizes(
                    sm = dp("iconSm", base.iconSizes.sm),
                    md = dp("iconMd", base.iconSizes.md),
                    lg = dp("iconLg", base.iconSizes.lg),
                    xl = dp("iconXl", base.iconSizes.xl),
                ),
                typography = GrooveTypographyScale.fromId(o.optString("typeScale", ""))?.roles
                    ?: GrooveTypographyRoles(
                        pageTitle = role("pageTitle", GrooveTypographyRoles.Default.pageTitle),
                        sectionTitle = role("sectionTitle", GrooveTypographyRoles.Default.sectionTitle),
                        sectionItemTitle = role("itemTitle", GrooveTypographyRoles.Default.sectionItemTitle),
                        sectionItemSubtitle = role("itemSub", GrooveTypographyRoles.Default.sectionItemSubtitle),
                        menuSongTitle = role("menuTitle", GrooveTypographyRoles.Default.menuSongTitle),
                        menuSongArtist = role("menuArtist", GrooveTypographyRoles.Default.menuSongArtist),
                        menuSongAlbum = role("menuAlbum", GrooveTypographyRoles.Default.menuSongAlbum),
                        playerSongTitle = role("playerTitle", GrooveTypographyRoles.Default.playerSongTitle),
                        playerSongArtist = role("playerArtist", GrooveTypographyRoles.Default.playerSongArtist),
                        playerSongAlbum = role("playerAlbum", GrooveTypographyRoles.Default.playerSongAlbum),
                        miniPlayerSongTitle = role("miniTitle", GrooveTypographyRoles.Default.miniPlayerSongTitle),
                        miniPlayerSongArtist = role("miniArtist", GrooveTypographyRoles.Default.miniPlayerSongArtist),
                        buttonLabel = role("btnLabel", GrooveTypographyRoles.Default.buttonLabel),
                        cardTitle = role("cardTitle", GrooveTypographyRoles.Default.cardTitle),
                        cardSubtitle = role("cardSub", GrooveTypographyRoles.Default.cardSubtitle),
                        body = role("body", GrooveTypographyRoles.Default.body),
                    ),
            )
        } catch (_: Exception) {
            null
        }
    }
}

object GrooveStyleCatalog {
    private val defaultSpacing = GrooveSpacing(
        xs = 8.dp,
        s = 12.dp,
        m = 16.dp,
        l = 24.dp,
        appBarHeight = 72.dp,
        cardPadding = 16.dp,
        listItemSpacing = 12.dp,
        sectionSpacing = 16.dp,
        buttonMinHeight = 48.dp,
    )

    private val defaultRadii = GrooveRadii(
        card = 12.dp,
        button = 8.dp,
        artwork = 12.dp,
        chip = 8.dp,
    )

    val Default = GrooveStyle(
        id = GrooveStyleIds.DEFAULT,
        displayName = "Default",
        colors = GrooveColors(
            canvas = Color.Black,
            edgeGradient = Color(0xFF161616),
            surface = Color(0xFF212121),
            surfaceRaised = Color(0xFF1F1F1F),
            onSurface = Color.White,
            muted = Color(0xFFDBDBDB),
            accent = Color(0xFFDBDBDB),
            onAccent = Color.Black,
            inactive = Color(0xFF262626),
            inactiveContainer = Color(0xFF443E3E),
            brandSecondary = Color(0xFF443E3E),
            brandTertiary = Color(0xFF828D6F),
            warning = Color(0xFFFFB349),
            error = Color(0xFFFF4949),
            sliderTrack = Color(0xFF888888),
            sliderFill = Color.White,
        ),
        radii = defaultRadii,
        spacing = defaultSpacing,
        typography = GrooveTypographyRoles.Default,
        iconSizes = GrooveIconSizes(),
    )

    val Midnight = Default.copy(
        id = GrooveStyleIds.MIDNIGHT,
        displayName = "Midnight",
        colors = Default.colors.copy(
            edgeGradient = Color(0xFF0C0C12),
            surface = Color(0xFF121212),
            surfaceRaised = Color(0xFF0A0A0A),
            muted = Color(0xFFE8E8E8),
            accent = Color.White,
            onAccent = Color.Black,
        ),
    )

    val Graphite = Default.copy(
        id = GrooveStyleIds.GRAPHITE,
        displayName = "Graphite",
        colors = Default.colors.copy(
            edgeGradient = Color(0xFF1C1C1C),
            surface = Color(0xFF2A2A2A),
            surfaceRaised = Color(0xFF242424),
            muted = Color(0xFFC8C8C8),
            accent = Color(0xFFD4D4D4),
            brandTertiary = Color(0xFF9AA38A),
        ),
    )

    val HighContrast = Default.copy(
        id = GrooveStyleIds.HIGH_CONTRAST,
        displayName = "High contrast",
        colors = Default.colors.copy(
            edgeGradient = Color(0xFF222222),
            surface = Color.Black,
            surfaceRaised = Color(0xFF111111),
            muted = Color.White,
            accent = Color.White,
            onAccent = Color.Black,
        ),
        radii = defaultRadii.copy(card = 4.dp, button = 4.dp, chip = 4.dp),
    )

    val Woodland = Default.copy(
        id = GrooveStyleIds.WOODLAND,
        displayName = "Woodland",
        colors = Default.colors.copy(
            edgeGradient = Color(0xFF15241C),
            surface = Color(0xFF1A2420),
            surfaceRaised = Color(0xFF15201C),
            onSurface = Color(0xFFE8F0E6),
            muted = Color(0xFFA8BFA8),
            accent = Color(0xFF8FBC8F),
            onAccent = Color(0xFF0C1210),
            inactive = Color(0xFF24302A),
            inactiveContainer = Color(0xFF354A3C),
            brandSecondary = Color(0xFF4A6B52),
            brandTertiary = Color(0xFF6B8F71),
            warning = Color(0xFFD4A574),
            error = Color(0xFFCF6B6B),
            sliderTrack = Color(0xFF3D5244),
            sliderFill = Color(0xFF8FBC8F),
        ),
    )

    val Desert = Default.copy(
        id = GrooveStyleIds.DESERT,
        displayName = "Desert",
        colors = Default.colors.copy(
            edgeGradient = Color(0xFF241C14),
            surface = Color(0xFF2A2218),
            surfaceRaised = Color(0xFF221C14),
            onSurface = Color(0xFFF5EDE0),
            muted = Color(0xFFD4C4A8),
            accent = Color(0xFFE8B86D),
            onAccent = Color(0xFF1A140C),
            inactive = Color(0xFF3A3024),
            inactiveContainer = Color(0xFF5C4A35),
            brandSecondary = Color(0xFFC4956A),
            brandTertiary = Color(0xFFA67C52),
            warning = Color(0xFFFFB349),
            error = Color(0xFFE07856),
            sliderTrack = Color(0xFF5C4A35),
            sliderFill = Color(0xFFE8B86D),
        ),
    )

    val EclipseBlue = Default.copy(
        id = GrooveStyleIds.ECLIPSE_BLUE,
        displayName = "Eclipse Blue",
        colors = Default.colors.copy(
            edgeGradient = Color(0xFF101A2C),
            surface = Color(0xFF121A2A),
            surfaceRaised = Color(0xFF0E1522),
            onSurface = Color(0xFFE8EEF8),
            muted = Color(0xFFA0B4D4),
            accent = Color(0xFF5B9FD4),
            onAccent = Color(0xFF070B14),
            inactive = Color(0xFF1A2438),
            inactiveContainer = Color(0xFF2A3A55),
            brandSecondary = Color(0xFF3D5A80),
            brandTertiary = Color(0xFF6B8FBF),
            warning = Color(0xFFE8C86A),
            error = Color(0xFFE07070),
            sliderTrack = Color(0xFF2A3A55),
            sliderFill = Color(0xFF5B9FD4),
        ),
    )

    val Ember = Default.copy(
        id = GrooveStyleIds.EMBER,
        displayName = "Ember",
        colors = Default.colors.copy(
            edgeGradient = Color(0xFF221410),
            surface = Color(0xFF241612),
            surfaceRaised = Color(0xFF1C100E),
            onSurface = Color(0xFFF8ECE8),
            muted = Color(0xFFD4B0A8),
            accent = Color(0xFFE07050),
            onAccent = Color(0xFF120A08),
            inactive = Color(0xFF3A221C),
            inactiveContainer = Color(0xFF5C3328),
            brandSecondary = Color(0xFFB85A40),
            brandTertiary = Color(0xFFD49070),
            warning = Color(0xFFFFB349),
            error = Color(0xFFFF4949),
            sliderTrack = Color(0xFF5C3328),
            sliderFill = Color(0xFFE07050),
        ),
    )

    val Ocean = Default.copy(
        id = GrooveStyleIds.OCEAN,
        displayName = "Ocean",
        colors = Default.colors.copy(
            edgeGradient = Color(0xFF0C2024),
            surface = Color(0xFF0E2428),
            surfaceRaised = Color(0xFF0A1C20),
            onSurface = Color(0xFFE4F4F6),
            muted = Color(0xFF98C8D0),
            accent = Color(0xFF3DB8C4),
            onAccent = Color(0xFF061214),
            inactive = Color(0xFF163438),
            inactiveContainer = Color(0xFF245055),
            brandSecondary = Color(0xFF2A7A85),
            brandTertiary = Color(0xFF5AA8B0),
            warning = Color(0xFFE8C86A),
            error = Color(0xFFE07070),
            sliderTrack = Color(0xFF245055),
            sliderFill = Color(0xFF3DB8C4),
        ),
    )

    val Lavender = Default.copy(
        id = GrooveStyleIds.LAVENDER,
        displayName = "Lavender",
        colors = Default.colors.copy(
            edgeGradient = Color(0xFF1A1626),
            surface = Color(0xFF1E1A2A),
            surfaceRaised = Color(0xFF181422),
            onSurface = Color(0xFFF0ECF8),
            muted = Color(0xFFC4B8D8),
            accent = Color(0xFFA78BFA),
            onAccent = Color(0xFF100E16),
            inactive = Color(0xFF2A2438),
            inactiveContainer = Color(0xFF3E3555),
            brandSecondary = Color(0xFF7C6BA8),
            brandTertiary = Color(0xFFB8A0E8),
            warning = Color(0xFFE8C86A),
            error = Color(0xFFE07090),
            sliderTrack = Color(0xFF3E3555),
            sliderFill = Color(0xFFA78BFA),
        ),
    )

    val all: List<GrooveStyle> = listOf(
        Default,
        Midnight,
        Graphite,
        HighContrast,
        Woodland,
        Desert,
        EclipseBlue,
        Ember,
        Ocean,
        Lavender,
    )

    fun resolve(styleId: String?, overridesJson: String?): GrooveStyle {
        GrooveStyleCodec.decode(overridesJson)?.let { return it }
        return all.firstOrNull { it.id == styleId } ?: Default
    }
}

val LocalGrooveStyle = staticCompositionLocalOf { GrooveStyleCatalog.Default }

object GrooveTheme {
    val style: GrooveStyle
        @Composable @ReadOnlyComposable get() = LocalGrooveStyle.current

    val colors: GrooveColors
        @Composable @ReadOnlyComposable get() = LocalGrooveStyle.current.colors

    val radii: GrooveRadii
        @Composable @ReadOnlyComposable get() = LocalGrooveStyle.current.radii

    val spacing: GrooveSpacing
        @Composable @ReadOnlyComposable get() = LocalGrooveStyle.current.spacing

    val typography: GrooveTypographyRoles
        @Composable @ReadOnlyComposable get() = LocalGrooveStyle.current.typography

    val iconSizes: GrooveIconSizes
        @Composable @ReadOnlyComposable get() = LocalGrooveStyle.current.iconSizes
}

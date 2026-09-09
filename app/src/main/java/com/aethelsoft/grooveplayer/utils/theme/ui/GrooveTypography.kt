package com.aethelsoft.grooveplayer.utils.theme.ui

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Serializable typography role (size + weight). Converted to [TextStyle] via [toTextStyle].
 */
@Immutable
data class GrooveTypeRole(
    val fontSizeSp: Float,
    val fontWeight: Int = FontWeight.Normal.weight,
    val letterSpacingSp: Float = 0f,
    val lineHeightSp: Float? = null,
) {
    fun toTextStyle(base: TextStyle = TextStyle.Default): TextStyle {
        val size = fontSizeSp.sp
        return base.copy(
            fontFamily = PoppinsFontFamily,
            fontSize = size,
            fontWeight = FontWeight(fontWeight.coerceIn(100, 900)),
            letterSpacing = letterSpacingSp.sp,
            lineHeight = (lineHeightSp ?: (fontSizeSp * 1.25f)).sp,
        )
    }
}

/**
 * Named text roles used across Profile, library lists, and player chrome.
 * Users pick a [GrooveTypographyScale] preset rather than editing each role.
 */
@Immutable
data class GrooveTypographyRoles(
    val pageTitle: GrooveTypeRole,
    val sectionTitle: GrooveTypeRole,
    val sectionItemTitle: GrooveTypeRole,
    val sectionItemSubtitle: GrooveTypeRole,
    val menuSongTitle: GrooveTypeRole,
    val menuSongArtist: GrooveTypeRole,
    val menuSongAlbum: GrooveTypeRole,
    val playerSongTitle: GrooveTypeRole,
    val playerSongArtist: GrooveTypeRole,
    val playerSongAlbum: GrooveTypeRole,
    val miniPlayerSongTitle: GrooveTypeRole,
    val miniPlayerSongArtist: GrooveTypeRole,
    val buttonLabel: GrooveTypeRole,
    val cardTitle: GrooveTypeRole,
    val cardSubtitle: GrooveTypeRole,
    val body: GrooveTypeRole,
) {
    companion object {
        /** Alias for [GrooveTypographyScale.Medium] — Material 3 baseline for phone music UI. */
        val Default: GrooveTypographyRoles get() = GrooveTypographyScale.Medium.roles
    }
}

/**
 * App-wide type scale presets (Material 3–aligned body/sections; Spotify-like page titles).
 *
 * - **pageTitle** matches compact top-bar titles (~16sp Bold at Medium), not M3 display/headline.
 * - **Medium** ≈ M3 baseline for section/item/body roles.
 * - **Small / Large / Extra large** step ~Major Second (1.125) around that baseline.
 * Weights follow M3 role conventions: titles Medium/Bold, body Regular, labels Medium.
 */
enum class GrooveTypographyScale(
    val id: String,
    val displayName: String,
    val roles: GrooveTypographyRoles,
) {
    Small(
        id = "small",
        displayName = "Small",
        roles = roles(
            pageTitle = 14f to FontWeight.Bold,
            pageTitleLine = 18f,
            sectionTitle = 20f to FontWeight.Medium,
            sectionTitleLine = 26f,
            itemTitle = 15f to FontWeight.Medium,
            itemTitleLine = 22f,
            itemSub = 13f to FontWeight.Normal,
            itemSubLine = 18f,
            menuTitle = 15f to FontWeight.Medium,
            menuTitleLine = 20f,
            menuArtist = 12f to FontWeight.Normal,
            menuArtistLine = 16f,
            menuAlbum = 11f to FontWeight.Normal,
            menuAlbumLine = 14f,
            playerTitle = 22f to FontWeight.SemiBold,
            playerTitleLine = 28f,
            playerArtist = 15f to FontWeight.Normal,
            playerArtistLine = 20f,
            playerAlbum = 13f to FontWeight.Normal,
            playerAlbumLine = 18f,
            miniTitle = 13f to FontWeight.Medium,
            miniTitleLine = 16f,
            miniArtist = 11f to FontWeight.Normal,
            miniArtistLine = 14f,
            button = 13f to FontWeight.Medium,
            buttonLine = 18f,
            cardTitle = 15f to FontWeight.Medium,
            cardTitleLine = 22f,
            cardSub = 13f to FontWeight.Normal,
            cardSubLine = 18f,
            body = 13f to FontWeight.Normal,
            bodyLine = 18f,
        ),
    ),
    Medium(
        id = "medium",
        displayName = "Medium",
        roles = roles(
            pageTitle = 16f to FontWeight.Bold,
            pageTitleLine = 22f,
            sectionTitle = 22f to FontWeight.Medium,
            sectionTitleLine = 28f,
            itemTitle = 16f to FontWeight.Medium,
            itemTitleLine = 24f,
            itemSub = 14f to FontWeight.Normal,
            itemSubLine = 20f,
            menuTitle = 16f to FontWeight.Medium,
            menuTitleLine = 22f,
            menuArtist = 13f to FontWeight.Normal,
            menuArtistLine = 18f,
            menuAlbum = 12f to FontWeight.Normal,
            menuAlbumLine = 16f,
            playerTitle = 24f to FontWeight.SemiBold,
            playerTitleLine = 30f,
            playerArtist = 16f to FontWeight.Normal,
            playerArtistLine = 22f,
            playerAlbum = 14f to FontWeight.Normal,
            playerAlbumLine = 20f,
            miniTitle = 14f to FontWeight.Medium,
            miniTitleLine = 18f,
            miniArtist = 12f to FontWeight.Normal,
            miniArtistLine = 16f,
            button = 14f to FontWeight.Medium,
            buttonLine = 20f,
            cardTitle = 16f to FontWeight.Medium,
            cardTitleLine = 24f,
            cardSub = 14f to FontWeight.Normal,
            cardSubLine = 20f,
            body = 14f to FontWeight.Normal,
            bodyLine = 20f,
        ),
    ),
    Large(
        id = "large",
        displayName = "Large",
        roles = roles(
            pageTitle = 18f to FontWeight.Bold,
            pageTitleLine = 24f,
            sectionTitle = 24f to FontWeight.Medium,
            sectionTitleLine = 32f,
            itemTitle = 18f to FontWeight.Medium,
            itemTitleLine = 26f,
            itemSub = 15f to FontWeight.Normal,
            itemSubLine = 22f,
            menuTitle = 18f to FontWeight.Medium,
            menuTitleLine = 24f,
            menuArtist = 14f to FontWeight.Normal,
            menuArtistLine = 20f,
            menuAlbum = 13f to FontWeight.Normal,
            menuAlbumLine = 18f,
            playerTitle = 28f to FontWeight.SemiBold,
            playerTitleLine = 36f,
            playerArtist = 18f to FontWeight.Normal,
            playerArtistLine = 24f,
            playerAlbum = 15f to FontWeight.Normal,
            playerAlbumLine = 22f,
            miniTitle = 15f to FontWeight.Medium,
            miniTitleLine = 20f,
            miniArtist = 13f to FontWeight.Normal,
            miniArtistLine = 18f,
            button = 15f to FontWeight.Medium,
            buttonLine = 22f,
            cardTitle = 18f to FontWeight.Medium,
            cardTitleLine = 26f,
            cardSub = 15f to FontWeight.Normal,
            cardSubLine = 22f,
            body = 15f to FontWeight.Normal,
            bodyLine = 22f,
        ),
    ),
    ExtraLarge(
        id = "extra_large",
        displayName = "Extra large",
        roles = roles(
            pageTitle = 20f to FontWeight.Bold,
            pageTitleLine = 26f,
            sectionTitle = 28f to FontWeight.Medium,
            sectionTitleLine = 36f,
            itemTitle = 20f to FontWeight.Medium,
            itemTitleLine = 28f,
            itemSub = 16f to FontWeight.Normal,
            itemSubLine = 24f,
            menuTitle = 20f to FontWeight.Medium,
            menuTitleLine = 26f,
            menuArtist = 15f to FontWeight.Normal,
            menuArtistLine = 22f,
            menuAlbum = 14f to FontWeight.Normal,
            menuAlbumLine = 20f,
            playerTitle = 32f to FontWeight.SemiBold,
            playerTitleLine = 40f,
            playerArtist = 20f to FontWeight.Normal,
            playerArtistLine = 26f,
            playerAlbum = 16f to FontWeight.Normal,
            playerAlbumLine = 24f,
            miniTitle = 16f to FontWeight.Medium,
            miniTitleLine = 22f,
            miniArtist = 14f to FontWeight.Normal,
            miniArtistLine = 18f,
            button = 16f to FontWeight.Medium,
            buttonLine = 24f,
            cardTitle = 20f to FontWeight.Medium,
            cardTitleLine = 28f,
            cardSub = 16f to FontWeight.Normal,
            cardSubLine = 24f,
            body = 16f to FontWeight.Normal,
            bodyLine = 24f,
        ),
    );

    companion object {
        fun fromId(id: String?): GrooveTypographyScale? =
            entries.firstOrNull { it.id == id }

        fun matching(roles: GrooveTypographyRoles): GrooveTypographyScale? =
            entries.firstOrNull { it.roles == roles }
    }
}

private fun role(size: Float, weight: FontWeight, line: Float) =
    GrooveTypeRole(fontSizeSp = size, fontWeight = weight.weight, lineHeightSp = line)

private fun roles(
    pageTitle: Pair<Float, FontWeight>,
    pageTitleLine: Float,
    sectionTitle: Pair<Float, FontWeight>,
    sectionTitleLine: Float,
    itemTitle: Pair<Float, FontWeight>,
    itemTitleLine: Float,
    itemSub: Pair<Float, FontWeight>,
    itemSubLine: Float,
    menuTitle: Pair<Float, FontWeight>,
    menuTitleLine: Float,
    menuArtist: Pair<Float, FontWeight>,
    menuArtistLine: Float,
    menuAlbum: Pair<Float, FontWeight>,
    menuAlbumLine: Float,
    playerTitle: Pair<Float, FontWeight>,
    playerTitleLine: Float,
    playerArtist: Pair<Float, FontWeight>,
    playerArtistLine: Float,
    playerAlbum: Pair<Float, FontWeight>,
    playerAlbumLine: Float,
    miniTitle: Pair<Float, FontWeight>,
    miniTitleLine: Float,
    miniArtist: Pair<Float, FontWeight>,
    miniArtistLine: Float,
    button: Pair<Float, FontWeight>,
    buttonLine: Float,
    cardTitle: Pair<Float, FontWeight>,
    cardTitleLine: Float,
    cardSub: Pair<Float, FontWeight>,
    cardSubLine: Float,
    body: Pair<Float, FontWeight>,
    bodyLine: Float,
): GrooveTypographyRoles = GrooveTypographyRoles(
    pageTitle = role(pageTitle.first, pageTitle.second, pageTitleLine),
    sectionTitle = role(sectionTitle.first, sectionTitle.second, sectionTitleLine),
    sectionItemTitle = role(itemTitle.first, itemTitle.second, itemTitleLine),
    sectionItemSubtitle = role(itemSub.first, itemSub.second, itemSubLine),
    menuSongTitle = role(menuTitle.first, menuTitle.second, menuTitleLine),
    menuSongArtist = role(menuArtist.first, menuArtist.second, menuArtistLine),
    menuSongAlbum = role(menuAlbum.first, menuAlbum.second, menuAlbumLine),
    playerSongTitle = role(playerTitle.first, playerTitle.second, playerTitleLine),
    playerSongArtist = role(playerArtist.first, playerArtist.second, playerArtistLine),
    playerSongAlbum = role(playerAlbum.first, playerAlbum.second, playerAlbumLine),
    miniPlayerSongTitle = role(miniTitle.first, miniTitle.second, miniTitleLine),
    miniPlayerSongArtist = role(miniArtist.first, miniArtist.second, miniArtistLine),
    buttonLabel = role(button.first, button.second, buttonLine),
    cardTitle = role(cardTitle.first, cardTitle.second, cardTitleLine),
    cardSubtitle = role(cardSub.first, cardSub.second, cardSubLine),
    body = role(body.first, body.second, bodyLine),
)

/** Bridges Groove roles into Material typography so existing MaterialTheme.typography call sites track UI styling. */
fun GrooveTypographyRoles.toMaterialTypography(): Typography {
    return Typography(
        displayLarge = pageTitle.toTextStyle(),
        displayMedium = pageTitle.toTextStyle(),
        displaySmall = sectionTitle.toTextStyle(),
        headlineLarge = pageTitle.toTextStyle(),
        headlineMedium = sectionTitle.toTextStyle(),
        headlineSmall = sectionTitle.toTextStyle(),
        titleLarge = sectionTitle.toTextStyle(),
        titleMedium = sectionItemTitle.toTextStyle(),
        titleSmall = cardTitle.toTextStyle(),
        bodyLarge = menuSongTitle.toTextStyle(),
        bodyMedium = menuSongArtist.toTextStyle(),
        bodySmall = menuSongAlbum.toTextStyle(),
        labelLarge = buttonLabel.toTextStyle(),
        labelMedium = sectionItemSubtitle.toTextStyle(),
        labelSmall = body.toTextStyle(),
    )
}

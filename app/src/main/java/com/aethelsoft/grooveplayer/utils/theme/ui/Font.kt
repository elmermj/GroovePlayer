package com.aethelsoft.grooveplayer.utils.theme.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.aethelsoft.grooveplayer.R

/**
 * Poppins font family using Google Fonts
 */
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val PoppinsFontName = GoogleFont("Poppins")

val PoppinsFontFamily = FontFamily(
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.Thin),
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.ExtraLight),
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.ExtraBold),
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.Black)
)

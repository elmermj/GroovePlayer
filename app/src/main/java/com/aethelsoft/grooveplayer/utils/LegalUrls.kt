package com.aethelsoft.grooveplayer.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Public legal pages (Cloudflare Pages). Used by Profile About rows.
 */
object LegalUrls {
    const val PRIVACY = "https://grooveplayer-legal.pages.dev/privacy/"
    const val TERMS = "https://grooveplayer-legal.pages.dev/terms/"

    fun open(context: Context, url: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: ActivityNotFoundException) {
            Log.w("LegalUrls", "No browser to open $url", e)
        }
    }
}

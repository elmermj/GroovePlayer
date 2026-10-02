package com.aethelworks.grooveplayer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/** Home-screen playback widget. No configuration screen. */
class PlaybackWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        PlaybackWidgetPublisher.refresh(context)
    }
}

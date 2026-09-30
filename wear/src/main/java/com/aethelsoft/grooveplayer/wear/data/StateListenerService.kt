package com.aethelsoft.grooveplayer.wear.data

import android.util.Log
import com.aethelsoft.grooveplayer.R
import com.aethelsoft.grooveplayer.wear.WearProtocol
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

/** Wakes the process when the phone publishes state or its capability changes. */
class StateListenerService : WearableListenerService() {
    override fun onCreate() {
        super.onCreate()
        // Resource shrinker must keep this array. Play Services reads it by name.
        val advertised = resources.getStringArray(R.array.android_wear_capabilities)
        if (WearProtocol.CAPABILITY_WATCH !in advertised) {
            Log.w(TAG, "Watch capability resource is missing")
        }
        WatchSession.ensureStarted(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        WatchSession.ensureStarted(this)
        WatchSession.handleData(dataEvents)
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        WatchSession.ensureStarted(this)
        WatchSession.onCapabilityChanged(capabilityInfo)
    }

    private companion object {
        const val TAG = "StateListener"
    }
}

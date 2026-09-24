package com.aethelsoft.grooveplayer.presentation.ads

import android.app.Activity
import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * UMP (EEA/UK) consent gate before [MobileAds.initialize].
 * Call [requestConsentThenInitAds] from the first Activity; safe to call more than once.
 */
object AdsConsentHelper {
    private const val TAG = "AdsConsent"
    private val adsInitialized = AtomicBoolean(false)

    fun requestConsentThenInitAds(activity: Activity) {
        val app = activity.applicationContext as Application
        val params = ConsentRequestParameters.Builder().build()
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.message}")
                    }
                    maybeInitializeAds(app, consentInformation)
                }
            },
            { error ->
                Log.w(TAG, "Consent info update failed: ${error.message}")
                // Still try to init when the update fails (e.g. offline) so FREE-tier ads can load.
                maybeInitializeAds(app, consentInformation)
            },
        )
    }

    private fun maybeInitializeAds(app: Application, consentInformation: ConsentInformation) {
        if (!consentInformation.canRequestAds()) {
            Log.i(TAG, "canRequestAds=false; skipping MobileAds.initialize")
            return
        }
        if (!adsInitialized.compareAndSet(false, true)) return
        try {
            MobileAds.initialize(app) {
                Log.i(TAG, "MobileAds initialized after consent")
            }
        } catch (e: Exception) {
            adsInitialized.set(false)
            Log.w(TAG, "MobileAds init failed", e)
        }
    }
}

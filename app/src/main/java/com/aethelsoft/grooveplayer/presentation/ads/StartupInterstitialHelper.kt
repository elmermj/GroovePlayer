package com.aethelsoft.grooveplayer.presentation.ads

import android.app.Activity
import android.util.Log
import com.aethelsoft.grooveplayer.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Startup "video" interstitial (max 2/day, confirmed FREE tier only).
 * Uses ADMOB_INTERSTITIAL_UNIT_ID from BuildConfig.
 * Failures are non-fatal so Sign-In is never blocked on ads.
 * The load callback checks entitlement again: a Premium `/v1/me` that lands
 * while the ad is loading must not present it.
 */
object StartupInterstitialHelper {
    private const val TAG = "StartupAd"

    fun maybeShow(activity: Activity, adsViewModel: AdsViewModel) {
        if (!adsViewModel.canShowStartupAd()) {
            Log.i(TAG, "Startup interstitial suppressed; entitlement is not confirmed Free")
            return
        }
        val unitId = BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID
        if (unitId.isBlank()) {
            Log.i(TAG, "No interstitial unit id configured; skipping")
            return
        }
        InterstitialAd.load(
            activity,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    if (!adsViewModel.canShowStartupAd()) {
                        Log.i(TAG, "Dropping loaded interstitial; entitlement is not confirmed Free")
                        return
                    }
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            adsViewModel.recordStartupAdShown()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.w(TAG, "Failed to show: ${error.message}")
                        }

                        override fun onAdDismissedFullScreenContent() = Unit
                    }
                    try {
                        ad.show(activity)
                    } catch (e: Exception) {
                        Log.w(TAG, "show() failed", e)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Failed to load interstitial: ${error.message}")
                }
            },
        )
    }
}

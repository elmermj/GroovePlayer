package com.aethelsoft.grooveplayer.presentation.ads

import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Banner ad slot — only loads when privilege tier is FREE.
 * Unit ID from BuildConfig (local.properties ADMOB_BANNER_UNIT_ID).
 */
@Composable
fun BannerAdSlot(
    modifier: Modifier = Modifier,
    adsViewModel: AdsViewModel = hiltViewModel(),
) {
    val show by adsViewModel.shouldShowAds.collectAsState()
    if (!show) return

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .height(50.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                )
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { /* keep */ },
        onRelease = { adView ->
            adView.destroy()
        },
    )
}

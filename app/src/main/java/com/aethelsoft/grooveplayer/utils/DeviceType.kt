package com.aethelsoft.grooveplayer.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Represents different device types based on screen width
 */
enum class DeviceType {
    PHONE,      // < 600dp
    TABLET,     // 600dp - 840dp
    LARGE_TABLET // > 840dp
}

/**
 * Determines the device type based on the current screen width.
 * Since the I develop this based on Samsung Galaxy Tab S10 Ultra
 * I like how it looks in huge screen size. So I architect the entire
 * code base to fit 3 different layouts catered to 3 major different
 * screen categories.
 * 
 * Breakpoints:
 * - Phone: < 600dp
 * - Tablet: 600dp - 840dp
 * - Large Tablet: > 840dp
 * 
 * Uses remember to cache the device type and only recalculate when screen width changes.
 */
@Composable
fun rememberDeviceType(): DeviceType {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    
    return remember(screenWidthDp) {
        when {
            screenWidthDp < 600 -> DeviceType.PHONE
            screenWidthDp < 840 -> DeviceType.TABLET
            else -> DeviceType.LARGE_TABLET
        }
    }
}


package com.aethelworks.grooveplayer.presentation.common

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import com.aethelworks.grooveplayer.utils.DeviceType
import com.aethelworks.grooveplayer.utils.rememberDeviceType
import com.aethelworks.grooveplayer.utils.theme.ui.PoppinsFontFamily
import java.util.function.Consumer

/** Flat white used by the frosted dialogs and sheets. Not a theme accent. */
internal val GlassWhite = Color(0xFFF2F2F2)

internal object GrooveHairline {
    val color = GlassWhite.copy(alpha = 0.12f)
    val edge = GlassWhite.copy(alpha = 0.14f)
}

internal fun glassTint(supported: Boolean): Color =
    Color.Black.copy(alpha = if (supported) 0.60f else 0.92f)

/**
 * True only when the compositor will actually blur across windows.
 * Battery saver, low-end GPUs, and the developer window-blur toggle can turn this off.
 */
@Composable
internal fun rememberGlassSupported(): Boolean {
    val context = LocalContext.current
    val windowManager = remember {
        context.getSystemService(WindowManager::class.java)
    }
    var enabled by remember {
        mutableStateOf(windowManager.crossWindowBlurEnabled())
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        DisposableEffect(windowManager) {
            val listener = Consumer<Boolean> { next -> enabled = next }
            val executor = ContextCompat.getMainExecutor(context)
            windowManager.watchCrossWindowBlur(executor, listener)
            onDispose { windowManager.ignoreCrossWindowBlur(listener) }
        }
    }
    return enabled
}

internal fun View.dialogWindow(): Window? {
    var parent = this.parent
    while (parent != null) {
        if (parent is DialogWindowProvider) return parent.window
        parent = parent.parent
    }
    return null
}

internal fun Window.applyBackdropBlur(radiusPx: Int, enabled: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    if (enabled && radiusPx > 0) {
        addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        setBlurBehindRadius(radiusPx)
    } else {
        clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    }
}

private fun WindowManager.crossWindowBlurEnabled(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    return crossWindowBlurEnabledApi31()
}

@RequiresApi(Build.VERSION_CODES.S)
private fun WindowManager.crossWindowBlurEnabledApi31(): Boolean = isCrossWindowBlurEnabled

@RequiresApi(Build.VERSION_CODES.S)
private fun WindowManager.watchCrossWindowBlur(
    executor: java.util.concurrent.Executor,
    listener: Consumer<Boolean>,
) {
    addCrossWindowBlurEnabledListener(executor, listener)
}

@RequiresApi(Build.VERSION_CODES.S)
private fun WindowManager.ignoreCrossWindowBlur(listener: Consumer<Boolean>) {
    removeCrossWindowBlurEnabledListener(listener)
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Window.setBlurBehindRadius(radiusPx: Int) {
    val params = attributes
    params.blurBehindRadius = radiusPx
    attributes = params
}

@Composable
internal fun Overline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = GlassWhite.copy(alpha = 0.55f),
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
    )
}

@Composable
internal fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(GrooveHairline.color),
    )
}

@Composable
internal fun GrooveTextButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = color,
            disabledContentColor = GlassWhite.copy(alpha = 0.30f),
        ),
    ) {
        Text(
            text = text,
            color = if (enabled) color else GlassWhite.copy(alpha = 0.30f),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GrooveSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val glass = rememberGlassSupported()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = GlassWhite,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.10f),
        shape = shape,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .size(32.dp, 4.dp)
                    .background(GlassWhite.copy(alpha = 0.30f), RoundedCornerShape(2.dp)),
            )
        },
        modifier = Modifier
            .border(1.dp, GrooveHairline.edge, shape)
            .grooveBottomChromeGlass(
                shape = shape,
                tint = glassTint(glass),
                blurRadius = if (glass) 24.dp else 0.dp,
            ),
        content = content,
    )
}

/**
 * Centered dialog. On API 31+ the window behind the card blurs when the compositor allows it.
 * Otherwise the card is near-opaque black and the dim is heavier.
 *
 * [anchorUpper] keeps the card in the top half so the IME can sit underneath it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GrooveDialog(
    onDismiss: () -> Unit,
    anchorUpper: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = rememberGlassSupported()
    val deviceType = rememberDeviceType()
    val maxWidth = if (deviceType == DeviceType.PHONE) 312.dp else 360.dp
    val density = LocalDensity.current
    val blurRadiusPx = with(density) { 24.dp.roundToPx() }
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val window = LocalView.current.dialogWindow()
        SideEffect {
            window?.setDimAmount(if (glass) 0.10f else 0.50f)
            window?.applyBackdropBlur(radiusPx = blurRadiusPx, enabled = glass)
            if (anchorUpper) {
                window?.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (anchorUpper) Modifier.statusBarsPadding().imePadding() else Modifier)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = if (anchorUpper) Alignment.TopCenter else Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = glassTint(glass),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, GrooveHairline.edge),
                modifier = Modifier
                    .padding(top = if (anchorUpper) 48.dp else 0.dp)
                    .widthIn(max = maxWidth)
                    .fillMaxWidth(),
            ) {
                Column(
                    Modifier
                        .drawBehind {
                            val highlightPx = 14.dp.toPx()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.10f),
                                        Color.Transparent,
                                    ),
                                    startY = 0f,
                                    endY = highlightPx,
                                ),
                                size = Size(size.width, highlightPx),
                            )
                        }
                        .padding(24.dp),
                    content = content,
                )
            }
        }
    }
}

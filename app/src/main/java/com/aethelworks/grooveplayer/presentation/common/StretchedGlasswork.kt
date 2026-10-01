package com.aethelworks.grooveplayer.presentation.common

import android.graphics.Bitmap
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size as CoilSize
import coil3.toBitmap
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Highly rounded glass card used by the stretched-glasswork list row. */
val StretchedGlassworkCorner = 22.dp

/** Square artwork inset on the left of a stretched-glasswork row. */
val StretchedGlassworkThumbnailSize = 48.dp

private const val STRETCH_WIDTH = 80
private const val STRETCH_HEIGHT = 48
private const val RENDER_BLUR_RADIUS = 24f

/**
 * Cached artwork for one row: sharp thumbnail plus the right-edge smear.
 * [blurred] is true when that smear was softened (RenderEffect, or the
 * preview fallback that stands in when RenderEffect cannot be recorded).
 */
@Immutable
data class StretchedGlassworkState(
    val thumbnail: ImageBitmap? = null,
    val background: ImageBitmap? = null,
    val blurred: Boolean = false,
    val scrimAlpha: Float = 0.62f,
    val loading: Boolean = false,
)

/**
 * Loads [model] once, stretches its right-hand column, and blurs that strip
 * when the platform can. The bitmaps are cached by artwork identity so
 * scrolling does not decode or blur again.
 *
 * [bitmap] skips the loader and is meant for previews.
 */
@Composable
fun rememberStretchedGlasswork(
    model: Any?,
    bitmap: ImageBitmap? = null,
): StretchedGlassworkState {
    val inspection = LocalInspectionMode.current
    val allowBlur = inspection || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // Layoutlib often cannot record a RenderEffect. Previews still show the
    // glow via the one-shot blur of the tiny stretched texture.
    val recordRenderEffect = !inspection && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    if (bitmap != null) {
        return remember(bitmap, allowBlur) {
            val prepared = prepareStretch(bitmap.asAndroidBitmap())
                ?: return@remember StretchedGlassworkState()
            // Previews rasterize on the composition thread. Skip RenderEffect
            // here; the tiny texture is blurred once and reused.
            assemble(prepared, rendered = null, allowBlur = allowBlur)
        }
    }
    val context = LocalContext.current
    val key = stretchedGlassworkModelKey(model)
    var state by remember(key) {
        mutableStateOf(
            key?.let { StretchedGlassworkCache.peek(it) }
                ?: StretchedGlassworkState(loading = key != null),
        )
    }
    LaunchedEffect(key, allowBlur, recordRenderEffect) {
        if (key == null || model == null) {
            state = StretchedGlassworkState()
            return@LaunchedEffect
        }
        StretchedGlassworkCache.peek(key)?.let {
            state = it
            return@LaunchedEffect
        }
        state = StretchedGlassworkState(loading = true)
        val loaded = StretchedGlassworkCache.load(key) {
            val source = withContext(Dispatchers.IO) {
                try {
                    decodeArtwork(context, model)
                } catch (_: Exception) {
                    null
                }
            } ?: return@load null
            val prepared = withContext(Dispatchers.Default) { prepareStretch(source) }
                ?: return@load null
            val rendered = if (recordRenderEffect) blurOnHandler(prepared.stretch) else null
            assemble(prepared, rendered, allowBlur)
        }.await()
        state = loaded ?: StretchedGlassworkState()
    }
    return state
}

/**
 * Frosted card background: stretched artwork color, a dark veil, and a
 * light edge that reads strongest on the top and left.
 */
fun Modifier.stretchedGlasswork(
    state: StretchedGlassworkState,
    cornerRadius: Dp,
    fallbackTint: Color,
    highlight: Color = Color.Transparent,
): Modifier = drawWithContent {
    val background = state.background
    if (background != null) {
        drawImage(
            image = background,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(
                size.width.roundToInt().coerceAtLeast(1),
                size.height.roundToInt().coerceAtLeast(1),
            ),
            filterQuality = FilterQuality.Medium,
        )
        val veil = state.scrimAlpha
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Black.copy(alpha = veil * 0.72f),
                1f to Color.Black.copy(alpha = veil.coerceAtMost(0.78f)),
            ),
        )
    } else {
        drawRect(fallbackTint)
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f)),
            ),
        )
    }
    if (highlight.alpha > 0f) {
        drawRect(highlight)
    }
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.04f),
                Color.Transparent,
            ),
            startY = 0f,
            endY = size.height * 0.62f,
        ),
    )
    drawContent()
    val stroke = 1.dp.toPx()
    val inset = stroke / 2f
    val radius = (cornerRadius.toPx() - inset).coerceAtLeast(0f)
    drawRoundRect(
        brush = Brush.linearGradient(
            colorStops = arrayOf(
                0f to Color.White.copy(alpha = 0.5f),
                0.42f to Color.White.copy(alpha = 0.14f),
                1f to Color.White.copy(alpha = 0.05f),
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
        topLeft = Offset(inset, inset),
        size = Size(
            (size.width - stroke).coerceAtLeast(0f),
            (size.height - stroke).coerceAtLeast(0f),
        ),
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = stroke),
    )
    val topY = stroke + 1.dp.toPx()
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.42f),
                Color.White.copy(alpha = 0.12f),
                Color.Transparent,
            ),
        ),
        start = Offset(radius * 0.45f, topY),
        end = Offset(size.width * 0.72f, topY),
        strokeWidth = stroke,
    )
    val leftX = stroke + 1.dp.toPx()
    drawLine(
        brush = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.34f),
                Color.White.copy(alpha = 0.08f),
                Color.Transparent,
            ),
        ),
        start = Offset(leftX, radius * 0.4f),
        end = Offset(leftX, size.height * 0.7f),
        strokeWidth = stroke,
    )
}

@Composable
fun StretchedGlassworkThumbnail(
    state: StretchedGlassworkState,
    modifier: Modifier = Modifier,
    kind: MediaArtworkKind = MediaArtworkKind.SONG,
    contentDescription: String? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier.clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        val thumbnail = state.thumbnail
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (!state.loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = kind.placeholderIcon(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(0.46f),
                    tint = Color.White.copy(alpha = 0.55f),
                )
            }
        }
    }
}

private fun assemble(
    prepared: PreparedStretch,
    rendered: Bitmap?,
    allowBlur: Boolean,
): StretchedGlassworkState {
    val baked = when {
        rendered != null -> rendered
        allowBlur -> stackBlurBitmap(prepared.stretch)
        else -> null
    }
    val background = baked ?: prepared.stretch
    val blurred = baked != null
    if (baked != null && baked !== prepared.stretch) {
        prepared.stretch.recycle()
    }
    return StretchedGlassworkState(
        thumbnail = prepared.thumbnail.asImageBitmap(),
        background = background.asImageBitmap(),
        blurred = blurred,
        scrimAlpha = scrimAlphaFor(prepared.luminance, blurred),
        loading = false,
    )
}

private data class PreparedStretch(
    val thumbnail: Bitmap,
    val stretch: Bitmap,
    val luminance: Float,
)

private fun prepareStretch(source: Bitmap): PreparedStretch? {
    if (source.width <= 0 || source.height <= 0) return null
    val software = if (source.config == Bitmap.Config.HARDWARE) {
        source.copy(Bitmap.Config.ARGB_8888, false) ?: return null
    } else {
        source
    }
    return try {
        val crop = centerSquareCrop(software.width, software.height) ?: return null
        val side = crop.size
        val pixels = IntArray(side * side)
        software.getPixels(pixels, 0, side, crop.left, crop.top, side, side)
        val owned = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        owned.setPixels(pixels, 0, side, 0, 0, side, side)
        val thumb = if (owned.width > 160) {
            Bitmap.createScaledBitmap(owned, 160, 160, true).also {
                if (it !== owned) owned.recycle()
            }
        } else {
            owned
        }
        if (thumb.width <= 1 || thumb.height <= 1) {
            if (thumb !== software) thumb.recycle()
            return null
        }
        val column = IntArray(thumb.height)
        thumb.getPixels(column, 0, 1, thumb.width - 1, 0, 1, thumb.height)
        val stretch = Bitmap.createBitmap(STRETCH_WIDTH, STRETCH_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(stretch)
        canvas.drawBitmap(
            thumb,
            Rect(thumb.width - 1, 0, thumb.width, thumb.height),
            Rect(0, 0, stretch.width, stretch.height),
            Paint().apply { isFilterBitmap = false },
        )
        PreparedStretch(
            thumbnail = thumb,
            stretch = stretch,
            luminance = averageLuminance(column),
        )
    } finally {
        if (software !== source) software.recycle()
    }
}

private fun stackBlurBitmap(source: Bitmap): Bitmap {
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    val blurred = boxBlur(boxBlur(pixels, width, height, radius = 10), width, height, radius = 8)
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
        bitmap.setPixels(blurred, 0, width, 0, 0, width, height)
    }
}

@Suppress("NewApi")
private suspend fun blurOnHandler(source: Bitmap): Bitmap? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val handler = RenderEffectThread.handler
    return suspendCancellableCoroutine { cont ->
        val task = Runnable {
            val result = runCatching { blurWithRenderEffect(source, RENDER_BLUR_RADIUS) }.getOrNull()
            if (cont.isActive) cont.resume(result)
        }
        handler.post(task)
        cont.invokeOnCancellation { handler.removeCallbacks(task) }
    }
}

@RequiresApi(31)
private fun blurWithRenderEffect(source: Bitmap, radius: Float): Bitmap? {
    val width = source.width
    val height = source.height
    if (width <= 0 || height <= 0) return null
    val reader = ImageReader.newInstance(
        width,
        height,
        PixelFormat.RGBA_8888,
        2,
        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
    )
    val renderer = HardwareRenderer()
    try {
        val node = RenderNode("stretched-glasswork")
        node.setPosition(0, 0, width, height)
        node.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
        val recording = node.beginRecording()
        recording.drawBitmap(source, 0f, 0f, null)
        node.endRecording()
        renderer.setContentRoot(node)
        renderer.setSurface(reader.surface)
        renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
        val image = reader.acquireNextImage() ?: return null
        try {
            val buffer = image.hardwareBuffer ?: return null
            try {
                val hardware = Bitmap.wrapHardwareBuffer(buffer, null) ?: return null
                val copy = hardware.copy(Bitmap.Config.ARGB_8888, false)
                hardware.recycle()
                if (!hasVisibleColor(copy) && hasVisibleColor(source)) {
                    copy.recycle()
                    return null
                }
                return copy
            } finally {
                buffer.close()
            }
        } finally {
            image.close()
        }
    } finally {
        renderer.destroy()
        reader.close()
    }
}

private fun hasVisibleColor(bitmap: Bitmap): Boolean {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return pixels.any { color ->
        ((color ushr 24) and 0xFF) > 0 && (color and 0x00FFFFFF) != 0
    }
}

private suspend fun decodeArtwork(context: android.content.Context, model: Any): Bitmap? {
    val request = ImageRequest.Builder(context)
        .data(model)
        .allowHardware(false)
        .size(CoilSize(192, 192))
        .build()
    val result = context.imageLoader.execute(request)
    return if (result is SuccessResult) result.image.toBitmap() else null
}

private object RenderEffectThread {
    val handler: Handler by lazy {
        val thread = HandlerThread("stretched-glasswork")
        thread.isDaemon = true
        thread.start()
        Handler(thread.looper)
    }
}

private object StretchedGlassworkCache {
    private val entries = android.util.LruCache<String, StretchedGlassworkState>(24)
    private val inflight = java.util.concurrent.ConcurrentHashMap<String, Deferred<StretchedGlassworkState?>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun peek(key: String): StretchedGlassworkState? = entries.get(key)

    fun load(key: String, block: suspend () -> StretchedGlassworkState?): Deferred<StretchedGlassworkState?> {
        peek(key)?.let { return CompletableDeferred(it) }
        return inflight.getOrPut(key) {
            scope.async {
                try {
                    peek(key) ?: block()?.also { entries.put(key, it) }
                } finally {
                    inflight.remove(key)
                }
            }
        }
    }
}

package com.aethelsoft.grooveplayer.presentation.library.songs

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.size.Size
import coil3.toBitmap
import com.aethelsoft.grooveplayer.domain.model.Album
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.makeAlbumId
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkPlaceholder
import com.aethelsoft.grooveplayer.presentation.library.songs.metadata.EditMetadataUiState
import com.aethelsoft.grooveplayer.presentation.library.songs.metadata.EditSongMetadataViewModel
import com.aethelsoft.grooveplayer.presentation.player.ui.extractDominantColor
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.rememberAdaptiveWindowInfo
import com.aethelsoft.grooveplayer.utils.rememberDeviceType
import com.aethelsoft.grooveplayer.utils.theme.icons.XClose
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Immersive metadata editor. Songs and song details both enter here.
 *
 * The wash is the artwork's dominant color (Palette, same helper as the full player).
 * Fields sit on frosted glass. Phone stacks the hero over the form; tablet keeps the
 * hero beside a scrolling form. Saving still goes through [EditSongMetadataViewModel].
 */
@Composable
fun EditSongMetadataDialog(
    song: Song,
    onDismiss: () -> Unit,
    onSave: (Song) -> Unit,
    viewModel: EditSongMetadataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(song.id) {
        viewModel.loadMetadata(song)
    }

    Dialog(
        onDismissRequest = { if (!uiState.isSaving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = !uiState.isSaving,
        )
    ) {
        EditSongMetadataSheet(
            song = song,
            uiState = uiState,
            onDismiss = onDismiss,
            onTitleChange = viewModel::updateTitle,
            onGenresChange = viewModel::updateGenres,
            onArtistsChange = viewModel::updateArtists,
            onAlbumChange = viewModel::updateAlbum,
            onYearChange = viewModel::updateYear,
            onUseAlbumYearChange = viewModel::updateUseAlbumYear,
            onTrackNumberChange = viewModel::updateTrackNumber,
            onArtworkChange = viewModel::updateArtwork,
            onSearchGenres = viewModel::searchGenres,
            onSearchArtists = viewModel::searchArtists,
            onSearchAlbums = viewModel::searchAlbums,
            onSaveClick = save@{
                if (uiState.isSaving || uiState.isLoading) return@save
                val snapshot = uiState
                coroutineScope.launch {
                    val ok = viewModel.saveMetadata()
                    if (!ok) return@launch
                    onSave(song.withEditedMetadata(snapshot))
                    onDismiss()
                }
            },
        )
    }
}

@Composable
private fun EditSongMetadataSheet(
    song: Song,
    uiState: EditMetadataUiState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onGenresChange: (List<String>) -> Unit,
    onArtistsChange: (List<String>) -> Unit,
    onAlbumChange: (String?) -> Unit,
    onYearChange: (Int?) -> Unit,
    onUseAlbumYearChange: (Boolean) -> Unit,
    onTrackNumberChange: (Int?) -> Unit,
    onArtworkChange: (ByteArray?, String?) -> Unit,
    onSearchGenres: (String) -> Unit,
    onSearchArtists: (String) -> Unit,
    onSearchAlbums: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    val deviceType = rememberDeviceType()
    val window = rememberAdaptiveWindowInfo()
    val wide = deviceType != DeviceType.PHONE
    val colors = GrooveTheme.colors
    val context = LocalContext.current
    val fallbackWash = colors.brandTertiary
    var dominant by remember(song.id) { mutableStateOf(fallbackWash) }

    val artworkModel: Any? = when {
        uiState.isLoading -> song.artworkUrl?.takeIf { it.isNotBlank() }
        uiState.artworkBytes.hasArtwork() -> uiState.artworkBytes
        else -> null
    }
    val artworkIdentity: Any? = when (val model = artworkModel) {
        is ByteArray -> model.contentHashCode()
        else -> model
    }

    LaunchedEffect(artworkIdentity, fallbackWash) {
        val source = artworkModel
        if (source == null) {
            dominant = fallbackWash
            return@LaunchedEffect
        }
        val extracted = runCatching {
            val bitmap = withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(source)
                    .allowHardware(false)
                    .size(Size(160, 160))
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) result.image.toBitmap() else null
            }
            bitmap?.let { frame ->
                withContext(Dispatchers.Default) { extractDominantColor(frame) }
            }
        }.getOrNull()
        dominant = extracted ?: fallbackWash
    }

    val wash by animateColorAsState(
        targetValue = dominant,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "metadataArtworkWash",
    )

    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, tween(durationMillis = 480, easing = FastOutSlowInEasing))
    }

    val displayTitle = if (uiState.isLoading) {
        song.title.ifBlank { "Untitled" }
    } else {
        uiState.title.ifBlank { "Untitled" }
    }
    val displayArtist = if (uiState.isLoading) {
        song.artist.ifBlank { "Unknown artist" }
    } else {
        uiState.artists.asCreditLine()
    }
    val artworkSize = when {
        deviceType == DeviceType.PHONE -> if (window.heightDp < 700f) 176.dp else 208.dp
        window.heightDp < 700f -> 200.dp
        deviceType == DeviceType.LARGE_TABLET -> 300.dp
        else -> 248.dp
    }
    val horizontal = when (deviceType) {
        DeviceType.PHONE -> 20.dp
        DeviceType.TABLET -> 28.dp
        DeviceType.LARGE_TABLET -> 36.dp
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = entrance.value
                translationY = (1f - entrance.value) * 28.dp.toPx()
            }
    ) {
        ArtworkWashBackdrop(
            artworkModel = artworkModel,
            wash = wash,
            canvas = colors.canvas,
            glowBiasX = if (wide) 0.24f else 0.5f,
            glowBiasY = if (wide) 0.38f else 0.24f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            MetadataTopBar(
                isSaving = uiState.isSaving,
                saveEnabled = !uiState.isSaving && !uiState.isLoading,
                onDiscard = onDismiss,
                onSave = onSaveClick,
            )

            AnimatedVisibility(
                visible = uiState.saveError != null,
                enter = fadeIn(tween(220)) + expandVertically(),
                exit = fadeOut(tween(160)),
            ) {
                uiState.saveError?.let { message ->
                    SaveErrorBanner(
                        message = message,
                        modifier = Modifier.padding(horizontal = horizontal, vertical = 4.dp),
                    )
                }
            }

            if (wide) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                ) {
                    Column(
                        modifier = Modifier
                            .weight(if (deviceType == DeviceType.LARGE_TABLET) 0.40f else 0.44f)
                            .verticalScroll(rememberScrollState())
                            .padding(start = horizontal, end = 12.dp, bottom = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MetadataHero(
                            title = displayTitle,
                            artist = displayArtist,
                            artworkModel = artworkModel,
                            wash = wash,
                            artworkSize = artworkSize,
                            isLoading = uiState.isLoading,
                            canRemoveArtwork = uiState.artworkBytes.hasArtwork(),
                            editingEnabled = !uiState.isSaving && !uiState.isLoading,
                            onArtworkChange = onArtworkChange,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(if (deviceType == DeviceType.LARGE_TABLET) 0.60f else 0.56f)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 8.dp, end = horizontal, bottom = 28.dp),
                    ) {
                        MetadataForm(
                            uiState = uiState,
                            editingEnabled = !uiState.isSaving && !uiState.isLoading,
                            onTitleChange = onTitleChange,
                            onGenresChange = onGenresChange,
                            onArtistsChange = onArtistsChange,
                            onAlbumChange = onAlbumChange,
                            onYearChange = onYearChange,
                            onUseAlbumYearChange = onUseAlbumYearChange,
                            onTrackNumberChange = onTrackNumberChange,
                            onSearchGenres = onSearchGenres,
                            onSearchArtists = onSearchArtists,
                            onSearchAlbums = onSearchAlbums,
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth(),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontal)
                        .padding(bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MetadataHero(
                        title = displayTitle,
                        artist = displayArtist,
                        artworkModel = artworkModel,
                        wash = wash,
                        artworkSize = artworkSize,
                        isLoading = uiState.isLoading,
                        canRemoveArtwork = uiState.artworkBytes.hasArtwork(),
                        editingEnabled = !uiState.isSaving && !uiState.isLoading,
                        onArtworkChange = onArtworkChange,
                    )
                    MetadataForm(
                        uiState = uiState,
                        editingEnabled = !uiState.isSaving && !uiState.isLoading,
                        onTitleChange = onTitleChange,
                        onGenresChange = onGenresChange,
                        onArtistsChange = onArtistsChange,
                        onAlbumChange = onAlbumChange,
                        onYearChange = onYearChange,
                        onUseAlbumYearChange = onUseAlbumYearChange,
                        onTrackNumberChange = onTrackNumberChange,
                        onSearchGenres = onSearchGenres,
                        onSearchArtists = onSearchArtists,
                        onSearchAlbums = onSearchAlbums,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtworkWashBackdrop(
    artworkModel: Any?,
    wash: Color,
    canvas: Color,
    glowBiasX: Float,
    glowBiasY: Float,
) {
    val context = LocalContext.current
    val frostBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val artAlpha = if (frostBlur) 0.82f else 0.28f
    val deep = wash.deepened()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(canvas)
    ) {
        if (artworkModel != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkModel)
                    .crossfade(420)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (frostBlur) {
                            Modifier.blur(68.dp, BlurredEdgeTreatment.Unbounded)
                        } else {
                            Modifier
                        }
                    )
                    .graphicsLayer {
                        scaleX = 1.22f
                        scaleY = 1.22f
                        alpha = artAlpha
                    },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to deep.copy(alpha = 0.78f),
                            0.30f to wash.copy(alpha = 0.42f),
                            0.62f to canvas.copy(alpha = 0.78f),
                            1.00f to canvas.copy(alpha = 0.94f),
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val center = Offset(size.width * glowBiasX, size.height * glowBiasY)
                    val radius = size.minDimension * 0.78f
                    val glow = Brush.radialGradient(
                        colors = listOf(
                            wash.copy(alpha = 0.55f),
                            wash.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = radius,
                    )
                    onDrawBehind {
                        drawCircle(brush = glow, radius = radius, center = center)
                    }
                }
        )
    }
}

@Composable
private fun MetadataTopBar(
    isSaving: Boolean,
    saveEnabled: Boolean,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = GrooveTheme.colors
    val typography = GrooveTheme.typography
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.18f),
                            Color.Transparent,
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onDiscard,
                enabled = !isSaving,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.onSurface,
                    disabledContentColor = colors.onSurface.copy(alpha = 0.38f),
                ),
            ) {
                Text(
                    text = "Discard",
                    style = typography.buttonLabel.toTextStyle(),
                )
            }
            Text(
                text = "Edit metadata",
                style = typography.pageTitle.toTextStyle(),
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            Button(
                onClick = onSave,
                enabled = saveEnabled,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent,
                    disabledContainerColor = colors.accent.copy(alpha = 0.32f),
                    disabledContentColor = colors.onAccent.copy(alpha = 0.55f),
                ),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(16.dp)
                            .semantics { contentDescription = "Saving metadata" },
                        strokeWidth = 2.dp,
                        color = colors.onAccent,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(
                    text = if (isSaving) "Saving" else "Save",
                    style = typography.buttonLabel.toTextStyle(),
                )
            }
        }
    }
}

@Composable
private fun MetadataHero(
    title: String,
    artist: String,
    artworkModel: Any?,
    wash: Color,
    artworkSize: Dp,
    isLoading: Boolean,
    canRemoveArtwork: Boolean,
    editingEnabled: Boolean,
    onArtworkChange: (ByteArray?, String?) -> Unit,
) {
    val colors = GrooveTheme.colors
    val typography = GrooveTheme.typography
    val spacing = GrooveTheme.spacing
    val context = LocalContext.current
    val shape = RoundedCornerShape(22.dp)
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.isNotEmpty()) {
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    onArtworkChange(bytes, mimeType)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(artworkSize + 36.dp)
                .drawWithCache {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val glow = Brush.radialGradient(
                        colors = listOf(
                            wash.copy(alpha = 0.50f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = size.minDimension * 0.48f,
                    )
                    onDrawBehind {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.38f),
                            radius = size.minDimension * 0.34f,
                            center = Offset(size.width / 2f, size.height * 0.58f),
                        )
                        drawCircle(brush = glow, radius = size.minDimension * 0.48f, center = center)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(artworkSize)
                    .clip(shape)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.42f),
                                wash.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.08f),
                            )
                        ),
                        shape = shape,
                    ),
            ) {
                if (artworkModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(artworkModel)
                            .crossfade(280)
                            .build(),
                        contentDescription = "Album artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    MediaArtworkPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        kind = MediaArtworkKind.ALBUM,
                        cornerRadius = 22.dp,
                        contentDescription = "No artwork",
                    )
                }
            }
        }

        Text(
            text = title,
            style = typography.playerSongTitle.toTextStyle().copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.45f),
                    offset = Offset(0f, 2f),
                    blurRadius = 12f,
                )
            ),
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s),
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = artist,
            style = typography.playerSongArtist.toTextStyle().copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.40f),
                    offset = Offset(0f, 1f),
                    blurRadius = 8f,
                )
            ),
            color = colors.muted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m),
        )
        Spacer(modifier = Modifier.height(spacing.m))

        if (!isLoading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassPill(
                    label = "Replace",
                    enabled = editingEnabled,
                    onClick = { imagePicker.launch("image/*") },
                    contentDescription = "Replace artwork",
                )
                if (canRemoveArtwork) {
                    GlassPill(
                        label = "Remove",
                        enabled = editingEnabled,
                        onClick = { onArtworkChange(null, null) },
                        contentDescription = "Remove artwork",
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(spacing.l))
    }
}

@Composable
private fun MetadataForm(
    uiState: EditMetadataUiState,
    editingEnabled: Boolean,
    onTitleChange: (String) -> Unit,
    onGenresChange: (List<String>) -> Unit,
    onArtistsChange: (List<String>) -> Unit,
    onAlbumChange: (String?) -> Unit,
    onYearChange: (Int?) -> Unit,
    onUseAlbumYearChange: (Boolean) -> Unit,
    onTrackNumberChange: (Int?) -> Unit,
    onSearchGenres: (String) -> Unit,
    onSearchArtists: (String) -> Unit,
    onSearchAlbums: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GrooveTheme.spacing
    if (uiState.isLoading) {
        Column(modifier = modifier.padding(top = spacing.s)) {
            GlassPanel {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.m),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = GrooveTheme.colors.onSurface,
                    )
                    Column {
                        Text(
                            text = "Reading tags",
                            style = GrooveTheme.typography.sectionItemTitle.toTextStyle(),
                            color = GrooveTheme.colors.onSurface,
                        )
                        Text(
                            text = "Pulling title, people, and artwork from the file.",
                            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                            color = GrooveTheme.colors.muted,
                        )
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = modifier.padding(top = spacing.xs),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        GlassSection(
            title = "Song",
            subtitle = "The name shown in your library and on the player.",
        ) {
            GlassTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                placeholder = "Title",
                enabled = editingEnabled,
            )
        }

        GlassSection(
            title = "Artists",
            subtitle = "The first name is the primary artist.",
        ) {
            GlassTokenEditor(
                values = uiState.artists,
                suggestions = uiState.artistSuggestions,
                placeholder = "Add an artist",
                enabled = editingEnabled,
                onValuesChange = onArtistsChange,
                onSearch = onSearchArtists,
            )
        }

        GlassSection(
            title = "Release",
            subtitle = "Album, year, and track.",
        ) {
            AlbumGlassField(
                album = uiState.album,
                suggestions = uiState.albumSuggestions,
                enabled = editingEnabled,
                onAlbumChange = onAlbumChange,
                onSearchAlbums = onSearchAlbums,
            )
            Spacer(modifier = Modifier.height(spacing.m))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.10f))
            )
            Spacer(modifier = Modifier.height(spacing.s))
            UseAlbumYearRow(
                useAlbumYear = uiState.useAlbumYear,
                enabled = editingEnabled,
                onUseAlbumYearChange = onUseAlbumYearChange,
            )
            AnimatedVisibility(
                visible = !uiState.useAlbumYear,
                enter = fadeIn(tween(220)) + expandVertically(),
                exit = fadeOut(tween(160)) + shrinkVertically(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(spacing.s))
                    YearGlassField(
                        year = uiState.year,
                        enabled = editingEnabled,
                        onYearChange = onYearChange,
                    )
                }
            }
            Spacer(modifier = Modifier.height(spacing.s))
            TrackGlassField(
                trackNumber = uiState.trackNumber,
                enabled = editingEnabled,
                onTrackNumberChange = onTrackNumberChange,
            )
        }

        GlassSection(
            title = "Genres",
            subtitle = "The first genre is the primary tag.",
        ) {
            GlassTokenEditor(
                values = uiState.genres,
                suggestions = uiState.genreSuggestions,
                placeholder = "Add a genre",
                enabled = editingEnabled,
                onValuesChange = onGenresChange,
                onSearch = onSearchGenres,
            )
        }

        Text(
            text = "Save writes these tags into the audio file and your library.",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle().copy(shadow = labelShadow),
            color = GrooveTheme.colors.muted,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun GlassSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    val colors = GrooveTheme.colors
    val typography = GrooveTheme.typography
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = typography.sectionItemSubtitle.toTextStyle().copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.6.sp,
                shadow = labelShadow,
            ),
            color = colors.onSurface.copy(alpha = 0.92f),
        )
        Text(
            text = subtitle,
            style = typography.sectionItemSubtitle.toTextStyle().copy(shadow = labelShadow),
            color = colors.muted,
        )
        GlassPanel(content = content)
    }
}

@Composable
private fun GlassPanel(
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedPanel(shape)
            .padding(16.dp)
            .animateContentSize(animationSpec = tween(280, easing = FastOutSlowInEasing)),
    ) {
        content()
    }
}

@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onDone: (() -> Unit)? = null,
) {
    val colors = GrooveTheme.colors
    val typography = GrooveTheme.typography
    val keyboard = LocalSoftwareKeyboardController.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 0.dp, minHeight = 52.dp),
        textStyle = typography.body.toTextStyle().copy(color = colors.onSurface),
        placeholder = {
            Text(
                text = placeholder,
                style = typography.body.toTextStyle(),
                color = colors.muted.copy(alpha = 0.72f),
            )
        },
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                onDone?.invoke()
                keyboard?.hide()
            }
        ),
        colors = TextFieldDefaults.colors(
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface,
            disabledTextColor = colors.onSurface.copy(alpha = 0.45f),
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            disabledContainerColor = Color.White.copy(alpha = 0.03f),
            cursorColor = colors.onSurface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedPlaceholderColor = colors.muted.copy(alpha = 0.72f),
            unfocusedPlaceholderColor = colors.muted.copy(alpha = 0.60f),
        ),
    )
}

@Composable
private fun GlassTokenEditor(
    values: List<String>,
    suggestions: List<String>,
    placeholder: String,
    enabled: Boolean,
    onValuesChange: (List<String>) -> Unit,
    onSearch: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val pending = query.trim()
    val available = suggestions
        .filter { it.isNotBlank() && it !in values }
        .distinct()
        .take(8)
    val canAddCustom = pending.isNotEmpty() &&
        pending !in values &&
        available.none { it.equals(pending, ignoreCase = true) }
    val showSuggestions = pending.isNotEmpty() && (available.isNotEmpty() || canAddCustom)

    fun commit(token: String) {
        val cleaned = token.trim()
        if (cleaned.isEmpty() || cleaned in values) return
        onValuesChange(values + cleaned)
        query = ""
        onSearch("")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (values.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                values.forEach { token ->
                    TokenChip(
                        label = token,
                        enabled = enabled,
                        onRemove = { onValuesChange(values.filterNot { it == token }) },
                    )
                }
            }
        }
        GlassTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            placeholder = placeholder,
            enabled = enabled,
            onDone = { commit(query) },
        )
        AnimatedVisibility(
            visible = showSuggestions,
            enter = fadeIn(tween(180)) + expandVertically(),
            exit = fadeOut(tween(120)),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Suggestions",
                    style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                    color = GrooveTheme.colors.muted,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (canAddCustom) {
                        GlassPill(
                            label = "Add “$pending”",
                            enabled = enabled,
                            emphasized = true,
                            onClick = { commit(pending) },
                            contentDescription = "Add $pending",
                        )
                    }
                    available.forEach { suggestion ->
                        GlassPill(
                            label = suggestion,
                            enabled = enabled,
                            onClick = { commit(suggestion) },
                            contentDescription = "Add $suggestion",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumGlassField(
    album: String?,
    suggestions: List<String>,
    enabled: Boolean,
    onAlbumChange: (String?) -> Unit,
    onSearchAlbums: (String) -> Unit,
) {
    var text by remember { mutableStateOf(album.orEmpty()) }
    val pending = text.trim()
    val available = suggestions
        .filter { it.isNotBlank() && !it.equals(text, ignoreCase = true) }
        .distinct()
        .take(8)
    val showSuggestions = pending.isNotEmpty() && available.isNotEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassTextField(
            value = text,
            onValueChange = {
                text = it
                onAlbumChange(it.ifBlank { null })
                onSearchAlbums(it)
            },
            placeholder = "Album name",
            enabled = enabled,
        )
        AnimatedVisibility(
            visible = showSuggestions,
            enter = fadeIn(tween(180)) + expandVertically(),
            exit = fadeOut(tween(120)),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                available.forEach { suggestion ->
                    GlassPill(
                        label = suggestion,
                        enabled = enabled,
                        onClick = {
                            text = suggestion
                            onAlbumChange(suggestion)
                            onSearchAlbums(suggestion)
                        },
                        contentDescription = "Use album $suggestion",
                    )
                }
            }
        }
    }
}

@Composable
private fun YearGlassField(
    year: Int?,
    enabled: Boolean,
    onYearChange: (Int?) -> Unit,
) {
    var text by remember { mutableStateOf(year?.toString().orEmpty()) }
    GlassTextField(
        value = text,
        onValueChange = {
            text = it.filter { ch -> ch.isDigit() }.take(4)
            onYearChange(text.toIntOrNull())
        },
        placeholder = "Year",
        enabled = enabled,
        keyboardType = KeyboardType.Number,
    )
}

@Composable
private fun TrackGlassField(
    trackNumber: Int?,
    enabled: Boolean,
    onTrackNumberChange: (Int?) -> Unit,
) {
    var text by remember { mutableStateOf(trackNumber?.toString().orEmpty()) }
    GlassTextField(
        value = text,
        onValueChange = {
            text = it.filter { ch -> ch.isDigit() }.take(4)
            onTrackNumberChange(text.toIntOrNull())
        },
        placeholder = "Track number",
        enabled = enabled,
        keyboardType = KeyboardType.Number,
    )
}

@Composable
private fun UseAlbumYearRow(
    useAlbumYear: Boolean,
    enabled: Boolean,
    onUseAlbumYearChange: (Boolean) -> Unit,
) {
    val colors = GrooveTheme.colors
    val typography = GrooveTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = "Use album year",
                style = typography.body.toTextStyle(),
                color = colors.onSurface,
            )
            Text(
                text = if (useAlbumYear) "Year follows the album" else "Set a year on this song",
                style = typography.sectionItemSubtitle.toTextStyle(),
                color = colors.muted,
            )
        }
        Switch(
            checked = useAlbumYear,
            onCheckedChange = onUseAlbumYearChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.onSurface,
                uncheckedTrackColor = colors.onSurface.copy(alpha = 0.22f),
                uncheckedBorderColor = colors.onSurface.copy(alpha = 0.28f),
            ),
        )
    }
}

@Composable
private fun TokenChip(
    label: String,
    enabled: Boolean,
    onRemove: () -> Unit,
) {
    val colors = GrooveTheme.colors
    Row(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onRemove)
            .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
            .semantics { contentDescription = "Remove $label" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = GrooveTheme.typography.buttonLabel.toTextStyle(),
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = XClose,
            contentDescription = null,
            tint = colors.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun GlassPill(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    emphasized: Boolean = false,
) {
    val colors = GrooveTheme.colors
    val border = if (emphasized) colors.accent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.22f)
    val fill = if (emphasized) colors.accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f)
    Text(
        text = label,
        style = GrooveTheme.typography.buttonLabel.toTextStyle(),
        color = colors.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(50))
            .background(fill)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { this.contentDescription = contentDescription },
    )
}

@Composable
private fun SaveErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = GrooveTheme.colors
    Text(
        text = message,
        style = GrooveTheme.typography.body.toTextStyle(),
        color = colors.error,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.error.copy(alpha = 0.16f))
            .border(1.dp, colors.error.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

private fun Modifier.frostedPanel(shape: Shape): Modifier = this
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.38f),
                Color.White.copy(alpha = 0.08f),
            )
        ),
        shape = shape,
    )
    .clip(shape)
    .drawWithCache {
        val veil = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to Color.White.copy(alpha = 0.16f),
                0.45f to Color.White.copy(alpha = 0.05f),
                1.00f to Color.Black.copy(alpha = 0.22f),
            )
        )
        onDrawBehind {
            drawRect(Color.Black.copy(alpha = 0.62f))
            drawRect(brush = veil)
            drawRect(
                color = Color.White.copy(alpha = 0.20f),
                size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()),
            )
        }
    }

private val labelShadow = Shadow(
    color = Color.Black.copy(alpha = 0.72f),
    offset = Offset(0f, 1f),
    blurRadius = 8f,
)

private fun Color.deepened(amount: Float = 0.58f): Color = copy(
    red = red * amount,
    green = green * amount,
    blue = blue * amount,
    alpha = 1f,
)

private fun ByteArray?.hasArtwork(): Boolean = this != null && this.isNotEmpty()

private fun List<String>.asCreditLine(): String =
    if (isEmpty()) "Unknown artist" else joinToString(separator = " · ")

private fun Song.withEditedMetadata(state: EditMetadataUiState): Song {
    val newAlbum = state.album?.let { newName ->
        val existing = album
        if (existing != null) {
            existing.copy(
                id = makeAlbumId(existing.artist, newName),
                name = newName,
            )
        } else {
            Album(
                id = makeAlbumId(state.artists.firstOrNull() ?: artist, newName),
                name = newName,
                artist = state.artists.firstOrNull() ?: artist,
                artworkUrl = artworkUrl,
                songs = emptyList(),
                year = state.year ?: year,
            )
        }
    }
    return copy(
        title = state.title,
        artist = state.artists.firstOrNull() ?: artist,
        genre = state.genres.firstOrNull() ?: genre,
        album = newAlbum,
    )
}

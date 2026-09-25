package com.aethelsoft.grooveplayer.presentation.library.songs

import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.PopupProperties
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
import com.aethelsoft.grooveplayer.utils.theme.ui.PoppinsFontFamily
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.draw.drawBehind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val TokenCanvas = Color(0xFF000000)
private val TokenEdge = Color(0xFF161616)
private val TokenSurface = Color(0xFF212121)
private val TokenMuted = Color(0xFFDBDBDB)
private val TokenInactive = Color(0xFF262626)
private val RadiusCard = 12.dp
private val RadiusControl = 8.dp
private val GlassFillAlpha = 0.64f
private val GlassBorderColor = Color.White.copy(alpha = 0.08f)

private val LabelStyle = TextStyle(
    fontFamily = PoppinsFontFamily,
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    color = TokenMuted,
)
private val ValueStyle = TextStyle(
    fontFamily = PoppinsFontFamily,
    fontSize = 16.sp,
    fontWeight = FontWeight.Normal,
    color = Color.White,
)
private val PreviewTitleStyle = TextStyle(
    fontFamily = PoppinsFontFamily,
    fontSize = 22.sp,
    fontWeight = FontWeight.SemiBold,
    color = Color.White,
)
private val PreviewArtistStyle = TextStyle(
    fontFamily = PoppinsFontFamily,
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    color = TokenMuted,
)

/**
 * Full-height metadata editor. Songs and song details both enter here.
 * Phone is a full-bleed sheet. Tablet and large tablet use a centered glass panel.
 * Saving stays on [EditSongMetadataViewModel].
 */
@Composable
fun EditSongMetadataDialog(
    song: Song,
    onDismiss: () -> Unit,
    onSave: (Song) -> Unit,
    viewModel: EditSongMetadataViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var baseline by remember(song.id) { mutableStateOf<EditMetadataUiState?>(null) }
    var confirmDiscard by remember(song.id) { mutableStateOf(false) }

    LaunchedEffect(song.id) {
        viewModel.loadMetadata(song)
    }
    LaunchedEffect(song.id, uiState.isLoading) {
        if (!uiState.isLoading && baseline == null) {
            baseline = uiState
        }
    }

    val dirty = baseline?.let { !it.sameEdits(uiState) } == true
    val canSave = dirty && !uiState.isSaving && !uiState.isLoading

    fun requestDismiss() {
        if (uiState.isSaving) return
        if (dirty) confirmDiscard = true else onDismiss()
    }

    Dialog(
        onDismissRequest = {
            if (uiState.isSaving) return@Dialog
            // One window handles back. While the confirm card is up, back means No
            // so it cannot also dismiss the editor underneath.
            if (confirmDiscard) {
                confirmDiscard = false
            } else {
                requestDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = !uiState.isSaving,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
        }
        Box(modifier = Modifier.fillMaxSize()) {
        EditSongMetadataSheet(
            song = song,
            uiState = uiState,
            canSave = canSave,
            onClose = { requestDismiss() },
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
                if (!canSave) return@save
                val snapshot = uiState
                coroutineScope.launch {
                    val ok = viewModel.saveMetadata()
                    if (!ok) return@launch
                    onSave(song.withEditedMetadata(snapshot))
                    onDismiss()
                }
            },
        )
        if (confirmDiscard) {
            DiscardConfirmOverlay(
                onNo = { confirmDiscard = false },
                onYes = {
                    confirmDiscard = false
                    onDismiss()
                },
            )
        }
        }
    }
}

@Composable
private fun EditSongMetadataSheet(
    song: Song,
    uiState: EditMetadataUiState,
    canSave: Boolean,
    onClose: () -> Unit,
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
    val context = LocalContext.current
    val deviceType = rememberDeviceType()
    val window = rememberAdaptiveWindowInfo()
    val wide = deviceType != DeviceType.PHONE
    val reduceMotion = rememberReducedMotion()
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enter.animateTo(
            targetValue = 1f,
            animationSpec = if (reduceMotion) {
                tween(durationMillis = 180)
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                )
            },
        )
    }

    val artworkModel: Any? = when {
        uiState.isLoading -> song.artworkUrl?.takeIf { it.isNotBlank() }
        uiState.artworkBytes.hasArtwork() -> uiState.artworkBytes
        else -> null
    }
    val artworkIdentity: Any? = when (val model = artworkModel) {
        is ByteArray -> model.contentHashCode()
        else -> model
    }
    var dominant by remember(song.id) { mutableStateOf(TokenEdge) }
    LaunchedEffect(artworkIdentity) {
        val source = artworkModel
        if (source == null) {
            dominant = TokenEdge
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
        dominant = extracted ?: TokenEdge
    }
    val wash by animateColorAsState(
        targetValue = dominant,
        animationSpec = tween(durationMillis = if (reduceMotion) 180 else 520),
        label = "metadataWash",
    )

    val previewTitle = uiState.title.ifBlank { if (uiState.isLoading) song.title else "" }
    val previewArtists = if (uiState.artists.isNotEmpty()) {
        uiState.artists.joinToString(", ")
    } else if (uiState.isLoading) {
        song.artist
    } else {
        ""
    }
    val artworkSize = when {
        wide -> 180.dp
        window.heightDp < 680f -> 160.dp
        window.heightDp > 900f -> 200.dp
        else -> 180.dp
    }
    val editingEnabled = !uiState.isSaving && !uiState.isLoading
    val foregroundMotion = Modifier.graphicsLayer {
        alpha = enter.value
        if (!reduceMotion) {
            translationY = (1f - enter.value) * 18.dp.toPx()
            val scale = 0.98f + (0.02f * enter.value)
            scaleX = scale
            scaleY = scale
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ArtworkWashBackdrop(
            artworkModel = artworkModel,
            wash = wash,
            hasArtwork = artworkModel != null,
            modifier = Modifier.graphicsLayer { alpha = enter.value },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .then(foregroundMotion),
            contentAlignment = Alignment.Center,
        ) {
            if (wide) {
                TabletMetadataPanel(
                    deviceType = deviceType,
                    artworkSize = artworkSize,
                    artworkModel = artworkModel,
                    wash = wash,
                    previewTitle = previewTitle,
                    previewArtists = previewArtists,
                    uiState = uiState,
                    editingEnabled = editingEnabled,
                    canSave = canSave,
                    onClose = onClose,
                    onTitleChange = onTitleChange,
                    onArtistsChange = onArtistsChange,
                    onAlbumChange = onAlbumChange,
                    onYearChange = onYearChange,
                    onUseAlbumYearChange = onUseAlbumYearChange,
                    onTrackNumberChange = onTrackNumberChange,
                    onGenresChange = onGenresChange,
                    onArtworkChange = onArtworkChange,
                    onSearchArtists = onSearchArtists,
                    onSearchAlbums = onSearchAlbums,
                    onSearchGenres = onSearchGenres,
                    onSaveClick = onSaveClick,
                )
            } else {
                PhoneMetadataSheet(
                    artworkSize = artworkSize,
                    artworkModel = artworkModel,
                    wash = wash,
                    previewTitle = previewTitle,
                    previewArtists = previewArtists,
                    uiState = uiState,
                    editingEnabled = editingEnabled,
                    canSave = canSave,
                    onClose = onClose,
                    onTitleChange = onTitleChange,
                    onArtistsChange = onArtistsChange,
                    onAlbumChange = onAlbumChange,
                    onYearChange = onYearChange,
                    onUseAlbumYearChange = onUseAlbumYearChange,
                    onTrackNumberChange = onTrackNumberChange,
                    onGenresChange = onGenresChange,
                    onArtworkChange = onArtworkChange,
                    onSearchArtists = onSearchArtists,
                    onSearchAlbums = onSearchAlbums,
                    onSearchGenres = onSearchGenres,
                    onSaveClick = onSaveClick,
                )
            }
        }
    }
}

@Composable
private fun PhoneMetadataSheet(
    artworkSize: Dp,
    artworkModel: Any?,
    wash: Color,
    previewTitle: String,
    previewArtists: String,
    uiState: EditMetadataUiState,
    editingEnabled: Boolean,
    canSave: Boolean,
    onClose: () -> Unit,
    onTitleChange: (String) -> Unit,
    onArtistsChange: (List<String>) -> Unit,
    onAlbumChange: (String?) -> Unit,
    onYearChange: (Int?) -> Unit,
    onUseAlbumYearChange: (Boolean) -> Unit,
    onTrackNumberChange: (Int?) -> Unit,
    onGenresChange: (List<String>) -> Unit,
    onArtworkChange: (ByteArray?, String?) -> Unit,
    onSearchArtists: (String) -> Unit,
    onSearchAlbums: (String) -> Unit,
    onSearchGenres: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EditorTopBar(onClose = onClose, enabled = !uiState.isSaving)
        ArtworkHero(
            artworkModel = artworkModel,
            wash = wash,
            artworkSize = artworkSize,
            editingEnabled = editingEnabled,
            canRemove = uiState.artworkBytes.hasArtwork(),
            onArtworkChange = onArtworkChange,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        LivePreview(title = previewTitle, artists = previewArtists)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            GlassPanel(
                artworkModel = artworkModel,
                modifier = Modifier.fillMaxWidth(),
            ) {
                MetadataFields(
                    uiState = uiState,
                    editingEnabled = editingEnabled,
                    onTitleChange = onTitleChange,
                    onArtistsChange = onArtistsChange,
                    onAlbumChange = onAlbumChange,
                    onYearChange = onYearChange,
                    onUseAlbumYearChange = onUseAlbumYearChange,
                    onTrackNumberChange = onTrackNumberChange,
                    onGenresChange = onGenresChange,
                    onSearchArtists = onSearchArtists,
                    onSearchAlbums = onSearchAlbums,
                    onSearchGenres = onSearchGenres,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        GlassFooter(
            canSave = canSave,
            isSaving = uiState.isSaving,
            onDiscard = onClose,
            onSave = onSaveClick,
        )
        SaveErrorLine(uiState.saveError)
    }
}

@Composable
private fun TabletMetadataPanel(
    deviceType: DeviceType,
    artworkSize: Dp,
    artworkModel: Any?,
    wash: Color,
    previewTitle: String,
    previewArtists: String,
    uiState: EditMetadataUiState,
    editingEnabled: Boolean,
    canSave: Boolean,
    onClose: () -> Unit,
    onTitleChange: (String) -> Unit,
    onArtistsChange: (List<String>) -> Unit,
    onAlbumChange: (String?) -> Unit,
    onYearChange: (Int?) -> Unit,
    onUseAlbumYearChange: (Boolean) -> Unit,
    onTrackNumberChange: (Int?) -> Unit,
    onGenresChange: (List<String>) -> Unit,
    onArtworkChange: (ByteArray?, String?) -> Unit,
    onSearchArtists: (String) -> Unit,
    onSearchAlbums: (String) -> Unit,
    onSearchGenres: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    val panelMax = if (deviceType == DeviceType.LARGE_TABLET) 640.dp else 560.dp
    GlassPanel(
        artworkModel = artworkModel,
        fill = true,
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .widthIn(max = panelMax)
            .fillMaxWidth()
            .fillMaxHeight(0.90f),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            EditorTopBar(onClose = onClose, enabled = !uiState.isSaving)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, bottom = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ArtworkHero(
                        artworkModel = artworkModel,
                        wash = wash,
                        artworkSize = artworkSize,
                        editingEnabled = editingEnabled,
                        canRemove = uiState.artworkBytes.hasArtwork(),
                        onArtworkChange = onArtworkChange,
                    )
                    LivePreview(title = previewTitle, artists = previewArtists)
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 8.dp),
                    ) {
                        MetadataFields(
                            uiState = uiState,
                            editingEnabled = editingEnabled,
                            onTitleChange = onTitleChange,
                            onArtistsChange = onArtistsChange,
                            onAlbumChange = onAlbumChange,
                            onYearChange = onYearChange,
                            onUseAlbumYearChange = onUseAlbumYearChange,
                            onTrackNumberChange = onTrackNumberChange,
                            onGenresChange = onGenresChange,
                            onSearchArtists = onSearchArtists,
                            onSearchAlbums = onSearchAlbums,
                            onSearchGenres = onSearchGenres,
                        )
                    }
                    GlassFooter(
                        canSave = canSave,
                        isSaving = uiState.isSaving,
                        onDiscard = onClose,
                        onSave = onSaveClick,
                        inset = true,
                    )
                    SaveErrorLine(uiState.saveError)
                }
            }
        }
    }
}

@Composable
private fun ArtworkWashBackdrop(
    artworkModel: Any?,
    wash: Color,
    hasArtwork: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(TokenCanvas),
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
                        if (canBlur) Modifier.blur(56.dp, BlurredEdgeTreatment.Unbounded) else Modifier
                    )
                    .graphicsLayer {
                        scaleX = 1.18f
                        scaleY = 1.18f
                        alpha = if (canBlur) 0.92f else 0.30f
                    },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (hasArtwork) {
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to wash.copy(alpha = 0.42f),
                                0.42f to TokenEdge.copy(alpha = 0.55f),
                                1.00f to TokenCanvas.copy(alpha = 0.94f),
                            ),
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(TokenEdge, TokenCanvas),
                        )
                    },
                ),
        )
    }
}

@Composable
private fun EditorTopBar(
    onClose: () -> Unit,
    enabled: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 4.dp),
    ) {
        IconButton(
            onClick = onClose,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp),
        ) {
            Icon(
                imageVector = XClose,
                contentDescription = "Close",
                tint = Color.White,
            )
        }
        Text(
            text = "Edit metadata",
            style = TextStyle(
                fontFamily = PoppinsFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .semantics { heading() },
        )
    }
}

@Composable
private fun ArtworkHero(
    artworkModel: Any?,
    wash: Color,
    artworkSize: Dp,
    editingEnabled: Boolean,
    canRemove: Boolean,
    onArtworkChange: (ByteArray?, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(RadiusCard)
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
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
    fun pick() {
        if (editingEnabled) imagePicker.launch("image/*")
    }

    Box(
        modifier = modifier
            .padding(top = 4.dp, bottom = 8.dp)
            .size(artworkSize)
            .drawBehind {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(wash.copy(alpha = 0.30f), Color.Transparent),
                        center = center,
                        radius = size.minDimension * 0.72f,
                    ),
                    radius = size.minDimension * 0.72f,
                    center = center,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .border(1.dp, GlassBorderColor, shape)
                .pointerInput(editingEnabled) {
                    detectTapGestures(
                        onTap = { pick() },
                        onLongPress = { pick() },
                    )
                }
                .semantics { contentDescription = "Album artwork" },
        ) {
            if (artworkModel != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artworkModel)
                        .crossfade(280)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                MediaArtworkPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    kind = MediaArtworkKind.ALBUM,
                    cornerRadius = RadiusCard,
                    contentDescription = "No artwork",
                )
            }
        }
        if (canRemove && editingEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .size(48.dp)
                    .semantics { contentDescription = "Remove artwork" }
                    .clickable { onArtworkChange(null, null) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(RadiusControl))
                        .background(TokenSurface.copy(alpha = 0.88f))
                        .border(1.dp, GlassBorderColor, RoundedCornerShape(RadiusControl)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = XClose,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        if (editingEnabled) {
            Text(
                text = "Replace",
                style = TextStyle(
                    fontFamily = PoppinsFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(RadiusControl))
                    .background(TokenSurface.copy(alpha = 0.88f))
                    .border(1.dp, GlassBorderColor, RoundedCornerShape(RadiusControl))
                    .clickable(onClick = ::pick)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics { contentDescription = "Replace artwork" },
            )
        }
    }
}

@Composable
private fun LivePreview(
    title: String,
    artists: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title.ifBlank { "Title" },
            style = PreviewTitleStyle,
            color = if (title.isBlank()) TokenMuted.copy(alpha = 0.55f) else Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = artists.ifBlank { "Artists" },
            style = PreviewArtistStyle,
            color = if (artists.isBlank()) TokenMuted.copy(alpha = 0.45f) else TokenMuted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetadataFields(
    uiState: EditMetadataUiState,
    editingEnabled: Boolean,
    onTitleChange: (String) -> Unit,
    onArtistsChange: (List<String>) -> Unit,
    onAlbumChange: (String?) -> Unit,
    onYearChange: (Int?) -> Unit,
    onUseAlbumYearChange: (Boolean) -> Unit,
    onTrackNumberChange: (Int?) -> Unit,
    onGenresChange: (List<String>) -> Unit,
    onSearchArtists: (String) -> Unit,
    onSearchAlbums: (String) -> Unit,
    onSearchGenres: (String) -> Unit,
) {
    if (uiState.isLoading) {
        Text(
            text = "Reading tags",
            style = LabelStyle,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        return
    }
    val artistFocus = remember { FocusRequester() }
    val trackFocus = remember { FocusRequester() }
    val yearFocus = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GlassTextField(
            label = "Title",
            value = uiState.title,
            onValueChange = onTitleChange,
            placeholder = "Title",
            enabled = editingEnabled,
            imeAction = ImeAction.Next,
            onNext = { artistFocus.requestFocus() },
        )
        TokenMenuField(
            label = "Artists",
            placeholder = "Add an artist",
            values = uiState.artists,
            suggestions = uiState.artistSuggestions,
            enabled = editingEnabled,
            focusRequester = artistFocus,
            onValuesChange = onArtistsChange,
            onSearch = onSearchArtists,
        )
        AlbumMenuField(
            album = uiState.album,
            suggestions = uiState.albumSuggestions,
            enabled = editingEnabled,
            onAlbumChange = onAlbumChange,
            onSearch = onSearchAlbums,
            onNext = { trackFocus.requestFocus() },
        )
        TrackYearRow(
            trackNumber = uiState.trackNumber,
            year = uiState.year,
            useAlbumYear = uiState.useAlbumYear,
            enabled = editingEnabled,
            trackFocus = trackFocus,
            yearFocus = yearFocus,
            onTrackNumberChange = onTrackNumberChange,
            onYearChange = onYearChange,
            onUseAlbumYearChange = onUseAlbumYearChange,
        )
        TokenMenuField(
            label = "Genres",
            placeholder = "Add a genre",
            values = uiState.genres,
            suggestions = uiState.genreSuggestions,
            enabled = editingEnabled,
            onValuesChange = onGenresChange,
            onSearch = onSearchGenres,
        )
    }
}

@Composable
private fun TrackYearRow(
    trackNumber: Int?,
    year: Int?,
    useAlbumYear: Boolean,
    enabled: Boolean,
    trackFocus: FocusRequester,
    yearFocus: FocusRequester,
    onTrackNumberChange: (Int?) -> Unit,
    onYearChange: (Int?) -> Unit,
    onUseAlbumYearChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        NumberGlassField(
            label = "Track number",
            value = trackNumber,
            placeholder = "Track",
            enabled = enabled,
            focusRequester = trackFocus,
            imeAction = ImeAction.Next,
            onNext = { yearFocus.requestFocus() },
            onValueChange = onTrackNumberChange,
            modifier = Modifier.weight(1f),
        )
        Column(modifier = Modifier.weight(1f)) {
            NumberGlassField(
                label = "Year",
                value = year,
                placeholder = "Year",
                enabled = enabled && !useAlbumYear,
                focusRequester = yearFocus,
                imeAction = ImeAction.Done,
                onValueChange = onYearChange,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Use album year",
                    style = LabelStyle,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    maxLines = 2,
                )
                Switch(
                    checked = useAlbumYear,
                    onCheckedChange = onUseAlbumYearChange,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color.White,
                        uncheckedThumbColor = TokenMuted,
                        uncheckedTrackColor = TokenInactive,
                        uncheckedBorderColor = GlassBorderColor,
                    ),
                )
            }
        }
    }
}

@Composable
private fun GlassTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
    keyboardType: KeyboardType = KeyboardType.Text,
    focusRequester: FocusRequester? = null,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(text = label, style = LabelStyle)
            Spacer(modifier = Modifier.height(4.dp))
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minWidth = 0.dp, minHeight = 48.dp)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .border(
                    width = 1.dp,
                    color = if (focused) Color.White.copy(alpha = 0.20f) else GlassBorderColor,
                    shape = RoundedCornerShape(RadiusControl),
                ),
            textStyle = ValueStyle,
            placeholder = {
                Text(text = placeholder, style = ValueStyle.copy(color = TokenMuted.copy(alpha = 0.45f)))
            },
            shape = RoundedCornerShape(RadiusControl),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { onNext?.invoke() },
                onDone = {
                    onDone?.invoke()
                    keyboard?.hide()
                },
            ),
            colors = glassFieldColors(),
        )
    }
}

@Composable
private fun NumberGlassField(
    label: String,
    value: Int?,
    placeholder: String,
    enabled: Boolean,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Done,
    onNext: (() -> Unit)? = null,
) {
    val canonical = value?.toString().orEmpty()
    var text by remember { mutableStateOf(canonical) }
    // Tags arrive after the first composition. Follow the loaded value unless the
    // text already represents it, so a draft like "01" is not rewritten mid-typing.
    LaunchedEffect(canonical) {
        val parsed = text.toIntOrNull()
        val alreadyShowing = if (value == null) {
            parsed == null && text.isEmpty()
        } else {
            parsed == value
        }
        if (!alreadyShowing) text = canonical
    }
    GlassTextField(
        label = label,
        value = text,
        onValueChange = {
            text = it
            onValueChange(it.toIntOrNull())
        },
        placeholder = placeholder,
        enabled = enabled,
        modifier = modifier,
        keyboardType = KeyboardType.Number,
        imeAction = imeAction,
        focusRequester = focusRequester,
        onNext = onNext,
    )
}

@Composable
private fun TokenMenuField(
    label: String,
    placeholder: String,
    values: List<String>,
    suggestions: List<String>,
    enabled: Boolean,
    onValuesChange: (List<String>) -> Unit,
    onSearch: (String) -> Unit,
    focusRequester: FocusRequester? = null,
) {
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    val pending = query.trim()
    val available = suggestions.filter { it.isNotBlank() && it !in values }.distinct().take(8)
    val canAdd = pending.isNotEmpty() && pending !in values
    val showMenu = menuOpen && pending.isNotEmpty() && (available.isNotEmpty() || canAdd)

    fun commit(token: String) {
        val cleaned = token.trim()
        if (cleaned.isEmpty() || cleaned in values) return
        onValuesChange(values + cleaned)
        query = ""
        menuOpen = false
        onSearch("")
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = LabelStyle)
        if (values.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                values.forEach { token ->
                    FilterChip(
                        selected = true,
                        onClick = { if (enabled) onValuesChange(values.filterNot { it == token }) },
                        enabled = enabled,
                        label = {
                            Text(
                                text = token,
                                fontFamily = PoppinsFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = XClose,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        shape = RoundedCornerShape(RadiusControl),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.White,
                            selectedLabelColor = Color.Black,
                            selectedTrailingIconColor = Color.Black,
                            containerColor = TokenSurface,
                            labelColor = TokenMuted,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = enabled,
                            selected = true,
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            disabledSelectedBorderColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .semantics { contentDescription = "Remove $token" },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        SuggestionMenu(
            expanded = showMenu,
            onDismiss = { menuOpen = false },
            field = {
                GlassTextField(
                    label = "",
                    value = query,
                    onValueChange = {
                        query = it
                        onSearch(it)
                        menuOpen = it.isNotBlank()
                    },
                    placeholder = placeholder,
                    enabled = enabled,
                    focusRequester = focusRequester,
                    onDone = { commit(query) },
                )
            },
        ) {
            if (canAdd) {
                GlassMenuItem(text = "Add “$pending”", onClick = { commit(pending) })
            }
            available.forEach { suggestion ->
                GlassMenuItem(text = suggestion, onClick = { commit(suggestion) })
            }
        }
    }
}

@Composable
private fun AlbumMenuField(
    album: String?,
    suggestions: List<String>,
    enabled: Boolean,
    onAlbumChange: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onNext: () -> Unit,
) {
    var text by remember { mutableStateOf(album.orEmpty()) }
    var menuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(album) {
        val next = album.orEmpty()
        if (text != next) {
            text = next
            menuOpen = false
        }
    }
    val available = suggestions
        .filter { it.isNotBlank() && !it.equals(text, ignoreCase = true) }
        .distinct()
        .take(8)
    val showMenu = menuOpen && text.isNotBlank() && available.isNotEmpty()

    SuggestionMenu(
        expanded = showMenu,
        onDismiss = { menuOpen = false },
        field = {
            GlassTextField(
                label = "Album",
                value = text,
                onValueChange = {
                    text = it
                    onAlbumChange(it.ifBlank { null })
                    onSearch(it)
                    menuOpen = it.isNotBlank()
                },
                placeholder = "Album name",
                enabled = enabled,
                imeAction = ImeAction.Next,
                onNext = onNext,
            )
        },
    ) {
        available.forEach { suggestion ->
            GlassMenuItem(
                text = suggestion,
                onClick = {
                    text = suggestion
                    onAlbumChange(suggestion)
                    menuOpen = false
                    onSearch(suggestion)
                },
            )
        }
    }
}

@Composable
private fun SuggestionMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    field: @Composable () -> Unit,
    items: @Composable () -> Unit,
) {
    var fieldWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { fieldWidth = it.width },
    ) {
        field()
        DropdownMenu(
            expanded = expanded && fieldWidth > 0,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(with(density) { fieldWidth.toDp() }),
            shape = RoundedCornerShape(RadiusCard),
            containerColor = TokenEdge,
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, GlassBorderColor),
            properties = PopupProperties(focusable = false),
        ) {
            items()
        }
    }
}

@Composable
private fun GlassMenuItem(
    text: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                fontFamily = PoppinsFontFamily,
                fontSize = 14.sp,
                color = TokenMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun GlassPanel(
    artworkModel: Any?,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(RadiusCard)
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.28f),
                spotColor = Color.Black.copy(alpha = 0.35f),
            )
            .border(1.dp, GlassBorderColor, shape)
            .clip(shape),
    ) {
        if (artworkModel != null && canBlur) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(artworkModel).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .blur(28.dp),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(TokenSurface.copy(alpha = GlassFillAlpha)),
        )
        Box(
            modifier = Modifier
                .padding(16.dp)
                .then(if (fill) Modifier.fillMaxSize() else Modifier),
        ) {
            content()
        }
    }
}

@Composable
private fun GlassFooter(
    canSave: Boolean,
    isSaving: Boolean,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    inset: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (inset) {
                    Modifier
                } else {
                    Modifier.background(TokenSurface.copy(alpha = 0.70f))
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GlassBorderColor),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = onDiscard,
                enabled = !isSaving,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = TokenMuted,
                    disabledContentColor = TokenMuted.copy(alpha = 0.38f),
                ),
            ) {
                Text(
                    text = "Discard",
                    fontFamily = PoppinsFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(RadiusControl),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.28f),
                    disabledContentColor = Color.Black.copy(alpha = 0.45f),
                ),
            ) {
                Text(
                    text = if (isSaving) "Saving…" else "Save",
                    fontFamily = PoppinsFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SaveErrorLine(message: String?) {
    if (message.isNullOrBlank()) return
    Text(
        text = message,
        color = GrooveTheme.colors.error,
        fontFamily = PoppinsFontFamily,
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun DiscardConfirmOverlay(
    onNo: () -> Unit,
    onYes: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onNo,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .widthIn(max = 340.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .shadow(4.dp, RoundedCornerShape(RadiusCard))
                .border(1.dp, GlassBorderColor, RoundedCornerShape(RadiusCard))
                .clip(RoundedCornerShape(RadiusCard))
                .background(TokenSurface.copy(alpha = 0.92f))
                .padding(20.dp),
        ) {
                Text(
                    text = "Discard changes?",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your edits will be lost.",
                    fontFamily = PoppinsFontFamily,
                    fontSize = 13.sp,
                    color = TokenMuted,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onNo,
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = TokenMuted),
                    ) {
                        Text(
                            text = "No",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onYes,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(RadiusControl),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text(
                            text = "Yes",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
        }
    }
}

@Composable
private fun glassFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    disabledTextColor = Color.White.copy(alpha = 0.38f),
    focusedContainerColor = TokenEdge.copy(alpha = 0.72f),
    unfocusedContainerColor = TokenEdge.copy(alpha = 0.55f),
    disabledContainerColor = TokenEdge.copy(alpha = 0.35f),
    cursorColor = Color.White,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    focusedPlaceholderColor = TokenMuted.copy(alpha = 0.45f),
    unfocusedPlaceholderColor = TokenMuted.copy(alpha = 0.45f),
)

@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        val duration = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        val transition = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        }.getOrDefault(1f)
        duration == 0f || transition == 0f
    }
}

private fun ByteArray?.hasArtwork(): Boolean = this != null && this.isNotEmpty()

private fun EditMetadataUiState.sameEdits(other: EditMetadataUiState): Boolean {
    if (title != other.title) return false
    if (genres != other.genres) return false
    if (artists != other.artists) return false
    if (album != other.album) return false
    if (year != other.year) return false
    if (trackNumber != other.trackNumber) return false
    if (useAlbumYear != other.useAlbumYear) return false
    if (artworkMimeType != other.artworkMimeType) return false
    val left = artworkBytes
    val right = other.artworkBytes
    if (left == null || right == null) return left == null && right == null
    return left.contentEquals(right)
}

private fun Song.withEditedMetadata(state: EditMetadataUiState): Song {
    val newAlbum = state.album?.let { newName ->
        val existing = album
        if (existing != null) {
            existing.copy(id = makeAlbumId(existing.artist, newName), name = newName)
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

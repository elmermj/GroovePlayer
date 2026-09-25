package com.aethelsoft.grooveplayer.presentation.library.importing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.data.library.LibraryImportUiState
import com.aethelsoft.grooveplayer.domain.library.ImportPromptCopy
import com.aethelsoft.grooveplayer.domain.library.LibraryUpgradePrompt
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.services.LibraryImportService
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

data class LibraryImportActions(
    val pickFolder: () -> Unit,
    val restoreSong: (Song) -> Unit,
)

val LocalLibraryImport = staticCompositionLocalOf {
    LibraryImportActions(pickFolder = {}, restoreSong = {})
}

@Composable
fun LibraryImportHost(
    viewModel: LibraryImportViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importState by viewModel.importState.collectAsState()
    val showUpgrade by viewModel.showUpgradePrompt.collectAsState()
    val colors = GrooveTheme.colors
    var launchedDelete by remember { mutableStateOf<List<Uri>?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        LibraryImportService.start(context, uri)
    }
    val systemDelete = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val pending = (importState as? LibraryImportUiState.NeedsSystemDelete)?.mediaStoreUris?.size ?: 0
        viewModel.onSystemDeleteFinished(result.resultCode == Activity.RESULT_OK, pending)
    }

    LaunchedEffect(importState) {
        val pending = importState as? LibraryImportUiState.NeedsSystemDelete ?: return@LaunchedEffect
        if (launchedDelete == pending.mediaStoreUris) return@LaunchedEffect
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || pending.mediaStoreUris.isEmpty()) return@LaunchedEffect
        launchedDelete = pending.mediaStoreUris
        val request = MediaStore.createDeleteRequest(context.contentResolver, pending.mediaStoreUris)
        systemDelete.launch(IntentSenderRequest.Builder(request.intentSender).build())
    }

    val actions = LibraryImportActions(
        pickFolder = {
            picker.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
                    )
                },
            )
        },
        restoreSong = { song -> viewModel.restore(song) },
    )

    CompositionLocalProvider(LocalLibraryImport provides actions) {
        content()
        when (val state = importState) {
            is LibraryImportUiState.Running -> {
                AlertDialog(
                    onDismissRequest = {},
                    properties = DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false,
                    ),
                    containerColor = colors.surface,
                    titleContentColor = colors.onSurface,
                    textContentColor = SoftWhite,
                    title = { Text(ImportPromptCopy.progress(state.completed, state.total)) },
                    text = {
                        Column {
                            Text(state.fileName.ifBlank { " " }, color = SoftWhite)
                            LinearProgressIndicator(
                                progress = {
                                    if (state.total <= 0) 0f else state.completed.toFloat() / state.total
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                color = colors.accent,
                                trackColor = colors.canvas,
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = viewModel::cancelImport) {
                            Text("Cancel", color = colors.accent)
                        }
                    },
                )
            }
            is LibraryImportUiState.NeedsDeleteDecision -> {
                AlertDialog(
                    onDismissRequest = viewModel::keepOriginals,
                    properties = DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                    ),
                    containerColor = colors.surface,
                    titleContentColor = colors.onSurface,
                    textContentColor = SoftWhite,
                    title = { Text(ImportPromptCopy.TITLE) },
                    text = {
                        Column {
                            Text(state.body)
                            if (!state.note.isNullOrBlank()) {
                                Text(
                                    state.note,
                                    modifier = Modifier.padding(top = 8.dp),
                                    color = SoftWhite,
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = viewModel::keepOriginals) {
                            Text(ImportPromptCopy.KEEP, color = colors.accent)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { scope.launch { viewModel.deleteOriginals() } }) {
                            Text(ImportPromptCopy.DELETE, color = colors.muted)
                        }
                    },
                )
            }
            is LibraryImportUiState.Finished -> {
                AlertDialog(
                    onDismissRequest = viewModel::dismissFinished,
                    containerColor = colors.surface,
                    titleContentColor = colors.onSurface,
                    textContentColor = SoftWhite,
                    title = { Text("Import") },
                    text = { Text(state.message) },
                    confirmButton = {
                        TextButton(onClick = viewModel::dismissFinished) {
                            Text("OK", color = colors.accent)
                        }
                    },
                )
            }
            else -> Unit
        }
        if (showUpgrade && importState is LibraryImportUiState.Idle) {
            AlertDialog(
                onDismissRequest = viewModel::later,
                containerColor = colors.surface,
                titleContentColor = colors.onSurface,
                textContentColor = SoftWhite,
                text = { Text(LibraryUpgradePrompt.BODY) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.importFoldersFromPrompt()
                        actions.pickFolder()
                    }) {
                        Text(
                            LibraryUpgradePrompt.IMPORT,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            color = colors.accent,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::later) {
                        Text(LibraryUpgradePrompt.LATER, color = colors.muted)
                    }
                },
            )
        }
    }
}

package com.aethelsoft.grooveplayer.presentation.backup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aethelsoft.grooveplayer.presentation.backup.layouts.LargeTabletBackupLayout
import com.aethelsoft.grooveplayer.presentation.backup.layouts.PhoneBackupLayout
import com.aethelsoft.grooveplayer.presentation.backup.layouts.TabletBackupLayout
import com.aethelsoft.grooveplayer.presentation.common.BasePageTemplate

@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    onRestoreLibrary: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    BackHandler(onBack = onNavigateBack)

    val lifecycleOwner = LocalLifecycleOwner.current

    // Backup open / focus: refetch /v1/me + objects when this destination is first composed.
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // onResume (and return to this destination): same live refetch without process death.
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BasePageTemplate(
        phoneLayout = { PhoneBackupLayout(viewModel, onRestoreLibrary) },
        tabletLayout = { TabletBackupLayout(viewModel, onRestoreLibrary) },
        largeTabletLayout = { LargeTabletBackupLayout(viewModel, onRestoreLibrary) },
        onNavigateToSearch = {},
        viewModel = viewModel,
        isSearchEnabled = false,
        pageTitle = "Backup",
        useSearchBar = false,
    )
}

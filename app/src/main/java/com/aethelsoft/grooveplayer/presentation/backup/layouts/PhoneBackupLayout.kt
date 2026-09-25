package com.aethelsoft.grooveplayer.presentation.backup.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.backup.BackupViewModel
import com.aethelsoft.grooveplayer.presentation.backup.ui.BackupContent
import com.aethelsoft.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelsoft.grooveplayer.presentation.common.topBarContentInset
import com.aethelsoft.grooveplayer.presentation.billing.BillingViewModel
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

@Composable
fun PhoneBackupLayout(
    viewModel: BackupViewModel,
    onRestoreLibrary: () -> Unit,
) {
    BackupScrollLayout(
        viewModel = viewModel,
        horizontalPadding = M_PADDING,
        compactTrimStack = true,
        onRestoreLibrary = onRestoreLibrary,
    )
}

@Composable
fun TabletBackupLayout(
    viewModel: BackupViewModel,
    onRestoreLibrary: () -> Unit,
) {
    BackupScrollLayout(
        viewModel = viewModel,
        horizontalPadding = M_PADDING,
        compactTrimStack = true,
        onRestoreLibrary = onRestoreLibrary,
    )
}

@Composable
fun LargeTabletBackupLayout(
    viewModel: BackupViewModel,
    onRestoreLibrary: () -> Unit,
) {
    BackupScrollLayout(
        viewModel = viewModel,
        horizontalPadding = M_PADDING,
        compactTrimStack = false,
        onRestoreLibrary = onRestoreLibrary,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupScrollLayout(
    viewModel: BackupViewModel,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    compactTrimStack: Boolean,
    onRestoreLibrary: () -> Unit,
    billingViewModel: BillingViewModel = hiltViewModel(),
) {
    val user by viewModel.authUser.collectAsState()
    val tier by viewModel.privilegeTier.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val objects by viewModel.objects.collectAsState()
    val objectsLoading by viewModel.objectsLoading.collectAsState()
    val objectsError by viewModel.objectsError.collectAsState()
    val objectsFilter by viewModel.objectsFilter.collectAsState()
    val deleteInFlight by viewModel.deleteInFlight.collectAsState()
    val trimInFlight by viewModel.trimInFlight.collectAsState()
    val trimMessage by viewModel.trimMessage.collectAsState()
    val librarySnapshot by viewModel.librarySnapshot.collectAsState()
    val libraryLoading by viewModel.libraryLoading.collectAsState()
    val restoreInFlight by viewModel.restoreInFlight.collectAsState()
    val restoreMessage by viewModel.restoreMessage.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val products by billingViewModel.products.collectAsState()
    val purchaseInFlight by billingViewModel.purchaseInFlight.collectAsState()
    val cancelInFlight by billingViewModel.cancelInFlight.collectAsState()
    val billingError by billingViewModel.billingError.collectAsState()

    val canvas = GrooveTheme.colors.canvas
    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(canvas),
        state = pullState,
        contentAlignment = Alignment.TopStart,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
        ) {
            item { Spacer(Modifier.height(topBarContentInset())) }
            item {
                BackupContent(
                    user = user,
                    tier = tier,
                    backupState = backupState,
                    onStartBackup = { viewModel.startBackup() },
                    objects = objects,
                    objectsLoading = objectsLoading,
                    objectsError = objectsError,
                    objectsFilter = objectsFilter,
                    onFilterChange = { viewModel.setObjectsFilter(it) },
                    deleteInFlight = deleteInFlight,
                    onDeleteObject = { viewModel.deleteObject(it) },
                    billingProducts = products,
                    purchaseInFlight = purchaseInFlight,
                    cancelInFlight = cancelInFlight,
                    billingError = billingError,
                    onPurchaseAddon = { activity, productId ->
                        billingViewModel.purchase(activity, productId)
                    },
                    onCancelAddon = { billingViewModel.cancelAddon(it) },
                    trimInFlight = trimInFlight,
                    trimMessage = trimMessage,
                    onTrim = { strategy, artist, album, year, limitBytes ->
                        viewModel.trimCloud(strategy, artist, album, year, limitBytes)
                    },
                    compactTrimStack = compactTrimStack,
                    librarySnapshot = librarySnapshot,
                    libraryLoading = libraryLoading,
                    restoreInFlight = restoreInFlight,
                    restoreMessage = restoreMessage,
                    onRestoreLibrary = onRestoreLibrary,
                )
            }
            item {
                Spacer(
                    Modifier.height(
                        M_PADDING + grooveBottomContentInset(includeMiniPlayer = true) + 134.dp,
                    ),
                )
            }
        }
    }
}

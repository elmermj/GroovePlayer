package com.aethelsoft.grooveplayer.presentation.library.importing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.data.library.LibraryImportController
import com.aethelsoft.grooveplayer.data.library.LibraryUpgradeStore
import com.aethelsoft.grooveplayer.data.library.PrivateLibraryMaintenance
import com.aethelsoft.grooveplayer.data.local.db.dao.SongDao
import com.aethelsoft.grooveplayer.domain.library.LibraryUpgradePrompt
import com.aethelsoft.grooveplayer.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryImportViewModel @Inject constructor(
    private val controller: LibraryImportController,
    private val maintenance: PrivateLibraryMaintenance,
    private val songDao: SongDao,
    private val upgradeStore: LibraryUpgradeStore,
) : ViewModel() {
    val importState = controller.state

    private val _showUpgradePrompt = MutableStateFlow(false)
    val showUpgradePrompt: StateFlow<Boolean> = _showUpgradePrompt.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { maintenance.markVisibility() }
            val hidden = runCatching { songDao.countHidden() }.getOrDefault(0)
            _showUpgradePrompt.value = LibraryUpgradePrompt.shouldShow(hidden, upgradeStore.isDismissed())
            runCatching { maintenance.backfillHashes() }
        }
    }

    fun cancelImport() = controller.requestCancel()

    fun keepOriginals() = controller.keepOriginals()

    fun dismissFinished() = controller.dismiss()

    fun later() {
        upgradeStore.dismiss()
        _showUpgradePrompt.value = false
    }

    fun importFoldersFromPrompt() {
        upgradeStore.dismiss()
        _showUpgradePrompt.value = false
    }

    suspend fun deleteOriginals() = controller.deleteOriginals()

    fun onSystemDeleteFinished(confirmed: Boolean, requested: Int) {
        controller.onSystemDeleteFinished(confirmed, requested)
    }

    fun restore(song: Song) {
        viewModelScope.launch { controller.restore(song) }
    }
}

package com.aethelsoft.grooveplayer.presentation.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.data.share.ShareTransferState
import com.aethelsoft.grooveplayer.data.share.NsdShareDiscovery
import com.aethelsoft.grooveplayer.domain.model.ShareSessionInfo
import com.aethelsoft.grooveplayer.domain.model.ShareableItem
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.ShareRepository
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor(
    application: Application,
    private val shareRepository: ShareRepository,
    private val musicRepository: MusicRepository,
    private val nsdShareDiscovery: NsdShareDiscovery
) : AndroidViewModel(application) {

    val transferState: StateFlow<ShareTransferState> = shareRepository.transferState

    private val _songsToShare = MutableStateFlow<List<Song>>(emptyList())
    val songsToShare: StateFlow<List<Song>> = _songsToShare.asStateFlow()

    private val _offerItems = MutableStateFlow<List<ShareableItem>>(emptyList())
    val offerItems: StateFlow<List<ShareableItem>> = _offerItems.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<ShareSessionInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<ShareSessionInfo>> = _discoveredDevices.asStateFlow()

    fun loadSongsToShare() {
        viewModelScope.launch {
            val songs = ShareIntentHolder.songs.value
            _songsToShare.value = songs
        }
    }

    /** Load all songs as default when user wants to share but none are selected. */
    suspend fun prepareToShareAsSender() {
        val current = ShareIntentHolder.songs.value
        if (current.isEmpty()) {
            val songs = musicRepository.getAllSongs()
            ShareIntentHolder.setSongs(songs)
            _songsToShare.value = songs
        }
    }

    /** Clear songs so the user acts as receiver. */
    fun clearSongsToReceive() {
        ShareIntentHolder.clear()
        _songsToShare.value = emptyList()
    }

    fun startSender(sessionInfo: ShareSessionInfo) {
        viewModelScope.launch {
            shareRepository.startSender(_songsToShare.value, sessionInfo)
        }
    }

    fun connectAndReceiveOffer(sessionInfo: ShareSessionInfo) {
        viewModelScope.launch {
            val items = shareRepository.connectAndReceiveOffer(sessionInfo)
            items?.let {
                _offerItems.value = it
                _selectedIds.value = it.map { i -> i.id }.toSet()
            }
        }
    }

    fun toggleSelection(id: String) {
        _selectedIds.update { ids ->
            if (ids.contains(id)) ids - id else ids + id
        }
    }

    fun approveAndReceive() {
        viewModelScope.launch {
            shareRepository.approveAndReceive(_selectedIds.value.toList())
        }
    }

    fun rejectOffer() {
        viewModelScope.launch {
            shareRepository.rejectOffer()
        }
    }

    fun cancelTransfer() {
        shareRepository.cancelTransfer()
    }

    fun resetOffer() {
        _offerItems.value = emptyList()
        _selectedIds.value = emptySet()
    }

    fun discoverNearbyDevices() = nsdShareDiscovery.discover()

    fun startDeviceDiscovery() {
        viewModelScope.launch {
            _discoveredDevices.value = emptyList()
            discoverNearbyDevices().catch { }.collect { info ->
                _discoveredDevices.update { it + info }
            }
        }
    }

    fun clearDiscoveredDevices() {
        _discoveredDevices.value = emptyList()
    }
}

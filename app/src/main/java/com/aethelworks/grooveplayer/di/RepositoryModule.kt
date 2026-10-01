package com.aethelworks.grooveplayer.di

import com.aethelworks.grooveplayer.data.bluetooth.BluetoothRepositoryImpl
import com.aethelworks.grooveplayer.data.local.mediastore.MediaStoreRepository
import com.aethelworks.grooveplayer.data.player.ExoPlayerManager
import com.aethelworks.grooveplayer.data.repository.AudioTagRepositoryImpl
import com.aethelworks.grooveplayer.data.repository.EqualizerRepositoryImpl
import com.aethelworks.grooveplayer.data.repository.PlaybackHistoryRepositoryImpl
import com.aethelworks.grooveplayer.data.repository.SongLikeRepositoryImpl
import com.aethelworks.grooveplayer.data.repository.ShareRepositoryImpl
import com.aethelworks.grooveplayer.data.repository.TransferRepositoryImpl
import com.aethelworks.grooveplayer.data.repository.PlaylistRepositoryImpl
import com.aethelworks.grooveplayer.data.repository.SongMetadataRepositoryImpl
import com.aethelworks.grooveplayer.data.repository.UserRepositoryImpl
import com.aethelworks.grooveplayer.data.repository.AuthRepositoryImpl
import com.aethelworks.grooveplayer.data.billing.BillingRepositoryImpl
import com.aethelworks.grooveplayer.data.backup.BackupRepositoryImpl
import com.aethelworks.grooveplayer.data.backup.GrooveDownloadsLocator
import com.aethelworks.grooveplayer.data.playback.CloudAudioLookupImpl
import com.aethelworks.grooveplayer.data.playback.CloudPlaybackCacheStore
import com.aethelworks.grooveplayer.data.playback.LocalAudioProbe
import com.aethelworks.grooveplayer.data.playback.PremiumCloudEntitlement
import com.aethelworks.grooveplayer.data.playback.PlaybackStreamTicketStore
import com.aethelworks.grooveplayer.data.library.FileM3uLocationHash
import com.aethelworks.grooveplayer.data.library.PlaylistLibraryIndex
import com.aethelworks.grooveplayer.data.profile.RecentUpdatesCatalog
import com.aethelworks.grooveplayer.data.playback.SongCatalogStore
import com.aethelworks.grooveplayer.domain.backup.AppLibraryPaths
import com.aethelworks.grooveplayer.domain.playback.CloudAudioLookup
import com.aethelworks.grooveplayer.domain.playback.CloudPlaybackCache
import com.aethelworks.grooveplayer.domain.playback.CloudStreamEntitlement
import com.aethelworks.grooveplayer.domain.playback.LocalAudioAvailability
import com.aethelworks.grooveplayer.domain.playback.PlaybackStreamTickets
import com.aethelworks.grooveplayer.domain.playback.SongCatalog
import com.aethelworks.grooveplayer.domain.playlist.M3uLocationHash
import com.aethelworks.grooveplayer.domain.playlist.PlaylistLibrary
import com.aethelworks.grooveplayer.data.ads.StartupAdQuotaStore
import com.aethelworks.grooveplayer.domain.repository.AuthRepository
import com.aethelworks.grooveplayer.domain.repository.BillingRepository
import com.aethelworks.grooveplayer.domain.repository.BackupRepository
import com.aethelworks.grooveplayer.domain.repository.StartupAdQuotaRepository
import com.aethelworks.grooveplayer.domain.repository.AudioTagRepository
import com.aethelworks.grooveplayer.domain.repository.BluetoothRepository
import com.aethelworks.grooveplayer.domain.repository.EqualizerRepository
import com.aethelworks.grooveplayer.domain.repository.MusicRepository
import com.aethelworks.grooveplayer.domain.repository.PlaybackHistoryRepository
import com.aethelworks.grooveplayer.domain.repository.PlaylistRepository
import com.aethelworks.grooveplayer.domain.repository.RecentUpdatesRepository
import com.aethelworks.grooveplayer.domain.repository.SongLikeRepository
import com.aethelworks.grooveplayer.domain.repository.PlayerRepository
import com.aethelworks.grooveplayer.domain.repository.ShareRepository
import com.aethelworks.grooveplayer.domain.repository.transfer.TransferRepository
import com.aethelworks.grooveplayer.domain.repository.SongMetadataRepository
import com.aethelworks.grooveplayer.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for binding repository interfaces to their implementations.
 * This follows Clean Architecture by providing domain repository interfaces
 * with their data layer implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindPlayerRepository(
        exoPlayerManager: ExoPlayerManager
    ): PlayerRepository
    
    @Binds
    abstract fun bindMusicRepository(
        mediaStoreRepository: MediaStoreRepository
    ): MusicRepository
    
    @Binds
    abstract fun bindPlaybackHistoryRepository(
        impl: PlaybackHistoryRepositoryImpl
    ): PlaybackHistoryRepository

    @Binds
    abstract fun bindSongLikeRepository(
        impl: SongLikeRepositoryImpl
    ): SongLikeRepository
    
    @Binds
    abstract fun bindAudioTagRepository(
        impl: AudioTagRepositoryImpl
    ): AudioTagRepository

    @Binds
    abstract fun bindSongMetadataRepository(
        impl: SongMetadataRepositoryImpl
    ): SongMetadataRepository
    
    @Binds
    abstract fun bindBluetoothRepository(
        impl: BluetoothRepositoryImpl
    ): BluetoothRepository
    
    @Binds
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    abstract fun bindBillingRepository(
        impl: BillingRepositoryImpl
    ): BillingRepository

    @Binds
    abstract fun bindBackupRepository(
        impl: BackupRepositoryImpl
    ): BackupRepository

    @Binds
    abstract fun bindStartupAdQuotaRepository(
        impl: StartupAdQuotaStore
    ): StartupAdQuotaRepository
    
    @Binds
    abstract fun bindEqualizerRepository(
        impl: EqualizerRepositoryImpl
    ): EqualizerRepository

    @Binds
    abstract fun bindShareRepository(
        impl: ShareRepositoryImpl
    ): ShareRepository
    
    @Binds
    abstract fun bindSearchRepository(
        impl: com.aethelworks.grooveplayer.data.repository.SearchRepositoryImpl
    ): com.aethelworks.grooveplayer.domain.repository.SearchRepository

    @Binds
    abstract fun bindTransferRepository(
        impl: TransferRepositoryImpl
    ): TransferRepository

    @Binds
    abstract fun bindPlaylistRepository(
        impl: PlaylistRepositoryImpl
    ): PlaylistRepository

    @Binds
    abstract fun bindRecentUpdatesRepository(
        impl: RecentUpdatesCatalog
    ): RecentUpdatesRepository

    @Binds
    abstract fun bindLocalAudioAvailability(
        impl: LocalAudioProbe
    ): LocalAudioAvailability

    @Binds
    abstract fun bindCloudPlaybackCache(
        impl: CloudPlaybackCacheStore
    ): CloudPlaybackCache

    @Binds
    abstract fun bindSongCatalog(
        impl: SongCatalogStore
    ): SongCatalog

    @Binds
    abstract fun bindCloudAudioLookup(
        impl: CloudAudioLookupImpl
    ): CloudAudioLookup

    @Binds
    abstract fun bindCloudStreamEntitlement(
        impl: PremiumCloudEntitlement
    ): CloudStreamEntitlement

    @Binds
    @Singleton
    abstract fun bindAppLibraryPaths(
        impl: GrooveDownloadsLocator,
    ): AppLibraryPaths

    @Binds
    abstract fun bindPlaybackStreamTickets(
        impl: PlaybackStreamTicketStore
    ): PlaybackStreamTickets

    @Binds
    @Singleton
    abstract fun bindPlaylistLibrary(
        impl: PlaylistLibraryIndex,
    ): PlaylistLibrary

    @Binds
    @Singleton
    abstract fun bindM3uLocationHash(
        impl: FileM3uLocationHash,
    ): M3uLocationHash
}

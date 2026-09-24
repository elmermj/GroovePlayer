package com.aethelsoft.grooveplayer.di

import com.aethelsoft.grooveplayer.data.bluetooth.BluetoothRepositoryImpl
import com.aethelsoft.grooveplayer.data.local.mediastore.MediaStoreRepository
import com.aethelsoft.grooveplayer.data.player.ExoPlayerManager
import com.aethelsoft.grooveplayer.data.repository.AudioTagRepositoryImpl
import com.aethelsoft.grooveplayer.data.repository.EqualizerRepositoryImpl
import com.aethelsoft.grooveplayer.data.repository.PlaybackHistoryRepositoryImpl
import com.aethelsoft.grooveplayer.data.repository.ShareRepositoryImpl
import com.aethelsoft.grooveplayer.data.repository.TransferRepositoryImpl
import com.aethelsoft.grooveplayer.data.repository.SongMetadataRepositoryImpl
import com.aethelsoft.grooveplayer.data.repository.UserRepositoryImpl
import com.aethelsoft.grooveplayer.data.repository.AuthRepositoryImpl
import com.aethelsoft.grooveplayer.data.billing.BillingRepositoryImpl
import com.aethelsoft.grooveplayer.data.backup.BackupRepositoryImpl
import com.aethelsoft.grooveplayer.data.playback.CloudAudioLookupImpl
import com.aethelsoft.grooveplayer.data.playback.CloudPlaybackCacheStore
import com.aethelsoft.grooveplayer.data.playback.LocalAudioProbe
import com.aethelsoft.grooveplayer.data.playback.PremiumCloudEntitlement
import com.aethelsoft.grooveplayer.data.playback.SongCatalogStore
import com.aethelsoft.grooveplayer.domain.playback.CloudAudioLookup
import com.aethelsoft.grooveplayer.domain.playback.CloudPlaybackCache
import com.aethelsoft.grooveplayer.domain.playback.CloudStreamEntitlement
import com.aethelsoft.grooveplayer.domain.playback.LocalAudioAvailability
import com.aethelsoft.grooveplayer.domain.playback.SongCatalog
import com.aethelsoft.grooveplayer.data.ads.StartupAdQuotaStore
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import com.aethelsoft.grooveplayer.domain.repository.BillingRepository
import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import com.aethelsoft.grooveplayer.domain.repository.StartupAdQuotaRepository
import com.aethelsoft.grooveplayer.domain.repository.AudioTagRepository
import com.aethelsoft.grooveplayer.domain.repository.BluetoothRepository
import com.aethelsoft.grooveplayer.domain.repository.EqualizerRepository
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import com.aethelsoft.grooveplayer.domain.repository.ShareRepository
import com.aethelsoft.grooveplayer.domain.repository.transfer.TransferRepository
import com.aethelsoft.grooveplayer.domain.repository.SongMetadataRepository
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

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
        impl: com.aethelsoft.grooveplayer.data.repository.SearchRepositoryImpl
    ): com.aethelsoft.grooveplayer.domain.repository.SearchRepository

    @Binds
    abstract fun bindTransferRepository(
        impl: TransferRepositoryImpl
    ): TransferRepository

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
}

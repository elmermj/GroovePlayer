package com.aethelsoft.grooveplayer.di

import com.aethelsoft.grooveplayer.data.bluetooth.BluetoothRepositoryImpl
import com.aethelsoft.grooveplayer.data.local.mediastore.MediaStoreRepository
import com.aethelsoft.grooveplayer.data.player.ExoPlayerManager
import com.aethelsoft.grooveplayer.data.repository.EqualizerRepositoryImpl
import com.aethelsoft.grooveplayer.data.repository.PlaybackHistoryRepositoryImpl
import com.aethelsoft.grooveplayer.data.repository.SongMetadataRepositoryImpl
import com.aethelsoft.grooveplayer.data.repository.UserRepositoryImpl
import com.aethelsoft.grooveplayer.domain.repository.BluetoothRepository
import com.aethelsoft.grooveplayer.domain.repository.EqualizerRepository
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
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
    abstract fun bindEqualizerRepository(
        impl: EqualizerRepositoryImpl
    ): EqualizerRepository
    
    @Binds
    abstract fun bindSearchRepository(
        impl: com.aethelsoft.grooveplayer.data.repository.SearchRepositoryImpl
    ): com.aethelsoft.grooveplayer.domain.repository.SearchRepository
}

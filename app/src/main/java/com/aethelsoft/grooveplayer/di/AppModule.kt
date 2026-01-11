package com.aethelsoft.grooveplayer.di

import android.content.Context
import androidx.room.Room
import com.aethelsoft.grooveplayer.data.local.db.GroovePlayerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing application-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GroovePlayerDatabase {
        return Room.databaseBuilder(
            context,
            GroovePlayerDatabase::class.java,
            "grooveplayer_database"
        )
        .fallbackToDestructiveMigration(false) // For development - in production, use proper migrations
        .build()
    }
    
    @Provides
    fun providePlaybackHistoryDao(database: GroovePlayerDatabase) = database.playbackHistoryDao()
    
    @Provides
    fun provideSongMetadataDao(database: GroovePlayerDatabase) = database.songMetadataDao()
    
    @Provides
    fun provideArtistDao(database: GroovePlayerDatabase) = database.artistDao()
    
    @Provides
    fun provideAlbumDao(database: GroovePlayerDatabase) = database.albumDao()
    
    @Provides
    fun provideUserProfileDao(database: GroovePlayerDatabase) = database.userProfileDao()
    
    @Provides
    fun provideUserSettingsDao(database: GroovePlayerDatabase) = database.userSettingsDao()
}

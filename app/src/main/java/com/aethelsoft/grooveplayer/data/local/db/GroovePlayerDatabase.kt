package com.aethelsoft.grooveplayer.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aethelsoft.grooveplayer.data.local.db.dao.AlbumDao
import com.aethelsoft.grooveplayer.data.local.db.dao.ArtistDao
import com.aethelsoft.grooveplayer.data.local.db.dao.PlaybackHistoryDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SearchHistoryDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SongMetadataDao
import com.aethelsoft.grooveplayer.data.local.db.dao.UserProfileDao
import com.aethelsoft.grooveplayer.data.local.db.dao.UserSettingsDao
import com.aethelsoft.grooveplayer.data.local.db.entity.AlbumEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.ArtistEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.PlaybackHistoryEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.SearchHistoryEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.SongMetadataEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.UserProfileEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.UserSettingsEntity

@Database(
    entities = [
        PlaybackHistoryEntity::class,
        SongMetadataEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        UserProfileEntity::class,
        UserSettingsEntity::class,
        SearchHistoryEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GroovePlayerDatabase : RoomDatabase() {
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun songMetadataDao(): SongMetadataDao
    abstract fun artistDao(): ArtistDao
    abstract fun albumDao(): AlbumDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}


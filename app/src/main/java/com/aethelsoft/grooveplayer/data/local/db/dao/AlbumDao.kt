package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aethelsoft.grooveplayer.data.local.db.entity.AlbumArtistCrossRef
import com.aethelsoft.grooveplayer.data.local.db.entity.AlbumEntity

@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums WHERE name LIKE :query || '%' ORDER BY name ASC")
    suspend fun searchAlbums(query: String): List<AlbumEntity>
    
    @Query("SELECT DISTINCT name FROM albums ORDER BY name ASC")
    suspend fun getAllAlbumNames(): List<String>
    
    @Query("SELECT * FROM albums WHERE name = :name LIMIT 1")
    suspend fun getAlbumByName(name: String): AlbumEntity?
    
    @Query(
        """
        SELECT albums.* FROM albums
        INNER JOIN album_artists ON album_artists.albumId = albums.albumId
        INNER JOIN artists ON artists.artistId = album_artists.artistId
        WHERE albums.name = :name AND artists.name = :artist
        LIMIT 1
        """
    )
    suspend fun getAlbum(name: String, artist: String): AlbumEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(album: AlbumEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbumArtistCrossRef(crossRef: AlbumArtistCrossRef)
    
    @Query("SELECT DISTINCT album FROM playback_history WHERE album LIKE :query || '%' ORDER BY album ASC")
    suspend fun searchAlbumsFromHistory(query: String): List<String>
    
    @Query(
        """
        SELECT albums.* FROM albums
        INNER JOIN album_artists ON album_artists.albumId = albums.albumId
        INNER JOIN artists ON artists.artistId = album_artists.artistId
        WHERE artists.name = :artistName
        ORDER BY albums.updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestAlbumByArtist(artistName: String): AlbumEntity?
}


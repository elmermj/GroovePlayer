package com.aethelsoft.grooveplayer.presentation.navigation

import android.net.Uri

object AppRoutes {
    const val HOME = "home"
    const val SONGS = "songs"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
    const val PLAYLISTS = "playlists"
    const val FULL_PLAYER = "full_player"
    const val RECENTLY_PLAYED = "recently_played"
    const val FAVORITE_TRACKS = "favorite_tracks"
    const val FAVORITE_ARTISTS = "favorite_artists"
    const val FAVORITE_ALBUMS = "favorite_albums"
    const val SEARCH = "search/{query}"
    const val ALBUM_DETAIL = "album/{albumId}"
    const val ARTIST_DETAIL = "artist/{artistId}"
    
    fun searchRoute(query: String) = "search/${Uri.encode(query)}"
    fun albumDetailRoute(albumId: String) = "album/${Uri.encode(albumId)}"
    fun artistDetailRoute(artistId: String) = "artist/${Uri.encode(artistId)}"
}
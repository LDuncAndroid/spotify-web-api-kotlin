/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2020; Original author: Adam Ratzman */
package com.adamratzman.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifySearchResult(
    val albums: PagingObject<SimpleAlbum>? = null,
    val artists: PagingObject<Artist>? = null,
    val playlists: PagingObject<SimplePlaylist>? = null,
    val tracks: PagingObject<Track>? = null
)

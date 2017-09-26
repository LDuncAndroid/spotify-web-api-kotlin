package endpoints.artists

import main.SpotifyAPI
import main.toObject
import obj.*
import java.util.stream.Collectors

class ArtistsAPI(api: SpotifyAPI) : Endpoint(api) {
    fun getArtist(artistId: String): Artist {
        return get("https://api.spotify.com/v1/artists/$artistId").toObject()
    }

    fun getArtists(vararg artistIds: String): List<Artist?> {
        return get("https://api.spotify.com/v1/artists?ids=${artistIds.toList().stream().collect(Collectors.joining(","))}")
                .removePrefix("{\n  \"artists\" :").removeSuffix("}").toObject()
    }

    fun getArtistAlbums(artistId: String): LinkedResult<SimpleAlbum> {
        return get("https://api.spotify.com/v1/artists/$artistId/albums").toObject()
    }

    fun getArtistTopTracks(artistId: String, country: Market): List<Track> {
        return get("https://api.spotify.com/v1/artists/$artistId/top-tracks?country=${country.code}")
                .removePrefix("{\n  \"tracks\" :").removeSuffix("}").toObject()
    }

    fun getRelatedArtists(artistId: String): List<Artist> {
        return get("https://api.spotify.com/v1/artists/$artistId/related-artists")
                .removePrefix("{\n  \"artists\" :").removeSuffix("}").toObject()
    }
}
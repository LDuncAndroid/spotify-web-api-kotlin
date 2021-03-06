/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2020; Original author: Adam Ratzman */
package com.adamratzman.spotify.private

import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.SpotifyException
import com.adamratzman.spotify.api
import com.adamratzman.spotify.endpoints.client.SpotifyTrackPositions
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class ClientPlaylistAPITest : Spek({
    describe("Client playlist test") {
        val cp = (api as? SpotifyClientApi)?.playlists
        val playlistsBefore = cp?.getClientPlaylists()?.complete()
        val createdPlaylist = cp?.createClientPlaylist("this is a test playlist", "description")
            ?.complete()

        createdPlaylist ?: return@describe
        it("get playlists for user, then see if we can create/delete playlists") {
            assertEquals(cp.getClientPlaylists().complete().items.size - 1, playlistsBefore?.items?.size)
        }
        it("edit playlists") {
            cp.changeClientPlaylistDetails(
                createdPlaylist.id, "test playlist", public = false,
                collaborative = true, description = "description 2"
            ).complete()

            cp.addTracksToClientPlaylist(createdPlaylist.id, "3WDIhWoRWVcaHdRwMEHkkS", "7FjZU7XFs7P9jHI9Z0yRhK").complete()

            cp.uploadClientPlaylistCover(
                createdPlaylist.id,
                imageUrl = "https://developer.spotify.com/assets/WebAPI_intro.png"
            ).complete()

            var updatedPlaylist = cp.getClientPlaylist(createdPlaylist.id).complete()!!
            val fullPlaylist = updatedPlaylist.toFullPlaylist().complete()!!

            assertTrue(
                updatedPlaylist.collaborative && updatedPlaylist.public == false &&
                    updatedPlaylist.name == "test playlist" && fullPlaylist.description == "description 2"
            )

            assertTrue(updatedPlaylist.tracks.total == 2 && updatedPlaylist.images.isNotEmpty())

            cp.reorderClientPlaylistTracks(updatedPlaylist.id, 1, insertionPoint = 0).complete()

            updatedPlaylist = cp.getClientPlaylist(createdPlaylist.id).complete()!!

            assertTrue(updatedPlaylist.toFullPlaylist().complete()?.tracks?.items?.get(0)?.track?.id == "7FjZU7XFs7P9jHI9Z0yRhK")

            cp.removeAllClientPlaylistTracks(updatedPlaylist.id).complete()

            updatedPlaylist = cp.getClientPlaylist(createdPlaylist.id).complete()!!

            assertTrue(updatedPlaylist.tracks.total == 0)
        }

        it("remove playlist tracks") {
            val trackIdOne = "3WDIhWoRWVcaHdRwMEHkkS"
            val trackIdTwo = "7FjZU7XFs7P9jHI9Z0yRhK"
            cp.addTracksToClientPlaylist(createdPlaylist.id, trackIdOne, trackIdOne, trackIdTwo, trackIdTwo).complete()

            assertTrue(cp.getPlaylistTracks(createdPlaylist.id).complete().items.size == 4)

            cp.removeTrackFromClientPlaylist(createdPlaylist.id, trackIdOne).complete()

            assertEquals(
                listOf(trackIdTwo, trackIdTwo),
                cp.getPlaylistTracks(createdPlaylist.id).complete().items.map { it.track?.id })

            cp.addTrackToClientPlaylist(createdPlaylist.id, trackIdOne).complete()

            cp.removeTrackFromClientPlaylist(createdPlaylist.id, trackIdTwo, SpotifyTrackPositions(1)).complete()

            assertEquals(
                listOf(trackIdTwo, trackIdOne),
                cp.getPlaylistTracks(createdPlaylist.id).complete().items.map { it.track?.id })

            cp.setClientPlaylistTracks(createdPlaylist.id, trackIdOne, trackIdOne, trackIdTwo, trackIdTwo).complete()

            cp.removeTracksFromClientPlaylist(createdPlaylist.id, trackIdOne, trackIdTwo).complete()

            assertTrue(cp.getPlaylistTracks(createdPlaylist.id).complete().items.isEmpty())

            cp.setClientPlaylistTracks(createdPlaylist.id, trackIdTwo, trackIdOne, trackIdTwo, trackIdTwo, trackIdOne)
                .complete()

            cp.removeTracksFromClientPlaylist(
                createdPlaylist.id, Pair(trackIdOne, SpotifyTrackPositions(4)),
                Pair(trackIdTwo, SpotifyTrackPositions(0))
            ).complete()

            assertEquals(
                listOf(trackIdOne, trackIdTwo, trackIdTwo),
                cp.getPlaylistTracks(createdPlaylist.id).complete().items.map { it.track?.id })

            assertFailsWith<SpotifyException.BadRequestException> {
                cp.removeTracksFromClientPlaylist(createdPlaylist.id, Pair(trackIdOne, SpotifyTrackPositions(3))).complete()
            }
        }

        it("destroy (unfollow) playlist") {
            cp.deleteClientPlaylist(createdPlaylist.id).complete()
            assertTrue(cp.getClientPlaylist(createdPlaylist.id).complete() == null)
        }
    }
})

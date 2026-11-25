package com.maxrave.simpmusic.utils

import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.artist.Albums
import com.maxrave.domain.data.model.browse.artist.ArtistBrowse
import com.maxrave.domain.data.model.browse.artist.Related
import com.maxrave.domain.data.model.browse.artist.ResultAlbum
import com.maxrave.domain.data.model.browse.artist.ResultRelated
import com.maxrave.domain.data.model.browse.artist.ResultSingle
import com.maxrave.domain.data.model.browse.artist.ResultSong
import com.maxrave.domain.data.model.browse.artist.Singles
import com.maxrave.domain.data.model.browse.artist.Songs
import com.maxrave.domain.data.model.searchResult.songs.Album
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.utils.Resource
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomePicksTest {
    private val labels = HomePicksLabels("picks", "mix", "based", "latest", "more", "similar")
    private val square = listOf(Thumbnail(120, "https://x/a=w120-h120-l90", 120))

    private fun pick(id: String) = HomePick(id, "T$id", listOf(Artist(null, "A$id")), "https://x/$id=w60-h60")

    private fun track(id: String) =
        Track(Album("", ""), listOf(Artist("UCx", "x")), null, 100, true, false, null, square, "T$id", id, null, null, null, null)

    private fun page(
        songs: List<String> = emptyList(),
        singles: List<Pair<String, String>> = emptyList(),
        albums: List<Pair<String, String>> = emptyList(),
        related: List<String> = emptyList(),
    ) = ArtistBrowse(
        albums = Albums("", albums.map { (id, year) -> ResultAlbum(id, false, square, id, year) }, ""),
        channelId = null,
        description = null,
        name = "n",
        radioId = null,
        related = Related("", related.map { ResultRelated(it, "1K", square, it) }),
        shuffleId = null,
        singles = Singles("", "", singles.map { (id, year) -> ResultSingle(id, square, id, year) }),
        songs = Songs(null, songs.map { ResultSong(it, it, null, 1, Album("", ""), "", square, true, false, null) }),
        video = null,
        featuredOn = null,
        videoList = null,
        subscribed = null,
        subscribers = null,
        thumbnails = null,
        views = null,
    )

    @Test
    fun togglesAndStopsAtFive() {
        val five = (1..5).fold(emptyList<HomePick>()) { list, i -> list.toggle(pick("$i")) }
        assertEquals(5, five.size)
        assertEquals(five, five.toggle(pick("6")))
        assertEquals(listOf("1", "2", "4", "5"), five.toggle(pick("3")).map { it.id })
    }

    @Test
    fun survivesStorageAndCorruption() {
        val picks = HomePicks(listOf(pick("UC1")), listOf(pick("v1")))
        assertEquals(picks, decodeHomePicks(encodeHomePicks(picks)))
        // Asked already, so a broken value must not bring the question back or crash.
        assertTrue(decodeHomePicks("{broken").isEmpty)
        assertTrue(decodeHomePicks(encodeHomePicks(HomePicks())).isEmpty)
    }

    @Test
    fun lookupsThatFailAreNull() =
        runBlocking {
            assertEquals(1, flowOf(Resource.Success(1)).dataOrNull())
            assertNull(flowOf(Resource.Error<Int>("no")).dataOrNull())
            assertNull(emptyFlow<Resource<Int>>().dataOrNull())
            assertNull(flow<Resource<Int>> { error("offline") }.dataOrNull())
        }

    @Test
    fun nothingPickedBuildsNothing() {
        assertTrue(buildPickShelves(HomePicks(), emptyList(), emptyList(), labels).isEmpty())
    }

    @Test
    fun offlineStillShowsThePicks() {
        val picks = HomePicks(listOf(pick("UC1")), listOf(pick("v1")))
        val shelf = buildPickShelves(picks, listOf(null), listOf(emptyList()), labels).single()
        assertEquals("picks", shelf.title)
        assertEquals("UC1", shelf.contents[0]?.browseId)
        assertNull(shelf.contents[0]?.videoId)
        assertEquals("v1", shelf.contents[1]?.videoId)
    }

    @Test
    fun buildsEveryShelfInOrder() {
        val picks = HomePicks(listOf(pick("UC1"), pick("UC2"), pick("UC3")), listOf(pick("v1"), pick("v2")))
        val shelves =
            buildPickShelves(
                picks,
                listOf(
                    page(
                        songs = listOf("s1", "v1"),
                        singles = listOf("old" to "2019", "new" to "Single • 2025"),
                        related = listOf("UC2", "UC8", "UC9", "MPLA"),
                    ),
                    // The middle artist's lookup failed: the third must not be mistaken for it.
                    null,
                    page(songs = listOf("s3"), albums = listOf("mid" to "2022", "undated" to "null"), related = listOf("UC9")),
                ),
                listOf(listOf(track("r1"), track("s1")), listOf(track("v2"), track("r2"))),
                labels,
            )
        assertEquals(listOf("picks", "mix", "latest", "TUC1", "TUC3", "similar"), shelves.map { it.title })
        // Interleaved, no repeats, none of the chosen songs themselves.
        assertEquals(listOf("r1", "s1", "s3", "r2"), shelves[1].contents.map { it?.videoId })
        assertEquals(listOf("new", "mid", "old", "undated"), shelves[2].contents.map { it?.browseId })
        assertEquals("TUC1 • 2025", shelves[2].contents[0]?.description)
        assertEquals("TUC1", shelves[2].contents[2]?.description?.substringBefore(" •"))
        assertEquals("TUC3", shelves[2].contents[3]?.description)
        assertEquals("UC3", shelves[4].channelId)
        // UC9 comes from two artists, so it leads; picked artists and non-artist pages are out.
        assertEquals(listOf("UC9", "UC8"), shelves[5].contents.map { it?.browseId })
        // Cards draw large square art, and a blank album is no album.
        val song = shelves[3].contents[0]
        assertEquals("https://x/a=w544-h544-l90", song?.thumbnails?.single()?.url)
        assertNull(song?.album)
    }
}

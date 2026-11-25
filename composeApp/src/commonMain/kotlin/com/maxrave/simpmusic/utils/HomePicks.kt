package com.maxrave.simpmusic.utils

import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.artist.ArtistBrowse
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.searchResult.songs.Album
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.utils.Resource
import com.maxrave.logger.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** How many artists, and how many songs, the listener names. */
const val MAX_HOME_PICKS = 5

private const val HOME_PICKS_KEY = "cybersync_home_picks"
private const val SHELF_SIZE = 20
private const val LOOKUP_TIMEOUT_MS = 30_000L
private val json = Json { ignoreUnknownKeys = true }

/** One chosen artist ([id] is the channel) or song ([id] is the video), with enough to draw it offline. */
@Serializable
data class HomePick(
    val id: String,
    val title: String,
    val artists: List<Artist> = emptyList(),
    val thumbnail: String? = null,
)

/** The favourites Home is built from. Empty lists mean "asked, and chose nothing". */
@Serializable
data class HomePicks(
    val artists: List<HomePick> = emptyList(),
    val songs: List<HomePick> = emptyList(),
) {
    val isEmpty get() = artists.isEmpty() && songs.isEmpty()
}

/** Adds [pick], or removes it when it is already in; a full list takes no more. */
fun List<HomePick>.toggle(pick: HomePick): List<HomePick> =
    when {
        any { it.id == pick.id } -> filterNot { it.id == pick.id }
        size >= MAX_HOME_PICKS -> this
        else -> this + pick
    }

/** Null until the listener has been asked; skipping stores an empty [HomePicks]. */
val DataStoreManager.homePicks: Flow<HomePicks?>
    get() = getString(HOME_PICKS_KEY).distinctUntilChanged().map { it?.let(::decodeHomePicks) }

suspend fun DataStoreManager.saveHomePicks(picks: HomePicks) = putString(HOME_PICKS_KEY, encodeHomePicks(picks))

internal fun encodeHomePicks(picks: HomePicks): String = json.encodeToString(HomePicks.serializer(), picks)

internal fun decodeHomePicks(stored: String): HomePicks =
    runCatching { json.decodeFromString(HomePicks.serializer(), stored) }.getOrDefault(HomePicks())

/** The first answer of a one-shot lookup, or null when it fails or takes too long. */
internal suspend fun <T> Flow<Resource<T>>.dataOrNull(): T? =
    withTimeoutOrNull(LOOKUP_TIMEOUT_MS) {
        catch { Logger.w("HomePicks", "Lookup failed: ${it.message}") }.firstOrNull()?.data
    }

/** Takes one from each list in turn, so no single source fills the front of the result. */
internal fun <T> List<List<T>>.interleave(): List<T> =
    (0 until (maxOfOrNull { it.size } ?: 0)).flatMap { i -> mapNotNull { it.getOrNull(i) } }

internal data class HomePicksLabels(
    val picks: String,
    val mix: String,
    val basedOnPicks: String,
    val latest: String,
    val moreFrom: String,
    val similar: String,
)

/**
 * Home's shelves for [picks]. [pages] lines up with `picks.artists` (null where the lookup failed) and
 * [radios] holds the radio of each chosen song. Whatever could not be fetched is left out; the picks
 * themselves need no network, so their shelf is always there.
 */
internal fun buildPickShelves(
    picks: HomePicks,
    pages: List<ArtistBrowse?>,
    radios: List<List<Track>>,
    labels: HomePicksLabels,
): List<HomeItem> {
    if (picks.isEmpty) return emptyList()
    val pickedSongs = picks.songs.map { it.id }.toSet()
    val pickedArtists = picks.artists.map { it.id }.toSet()
    val artistPages = picks.artists.zip(pages).mapNotNull { (pick, page) -> page?.let { pick to it } }
    val topSongs = artistPages.map { (_, page) -> page.songs?.results.orEmpty().map { song ->
        songContent(song.videoId, song.title, song.artists, song.album, song.thumbnails, song.isExplicit, song.durationSeconds, song.videoType)
    } }

    val shelves = mutableListOf<HomeItem>()

    fun shelf(
        title: String,
        contents: List<Content>,
        subtitle: String? = null,
        channelId: String? = null,
    ) {
        if (contents.isNotEmpty()) {
            shelves += HomeItem(contents = contents.take(SHELF_SIZE), title = title, subtitle = subtitle, channelId = channelId)
        }
    }

    shelf(
        labels.picks,
        picks.artists.map { pageContent(it.id, it.title, null, it.thumbnail) } +
            picks.songs.map { songContent(it.id, it.title, it.artists, null, listOfNotNull(it.thumbnail?.let(::squareThumbnail))) },
    )

    val radioSongs =
        radios.map { tracks ->
            tracks.map { songContent(it.videoId, it.title, it.artists, it.album, it.thumbnails.orEmpty(), it.isExplicit, it.durationSeconds, it.videoType) }
        }
    shelf(
        labels.mix,
        (radioSongs + topSongs).interleave().filter { it.videoId !in pickedSongs }.distinctBy { it.videoId },
        subtitle = labels.basedOnPicks,
    )

    shelf(
        labels.latest,
        artistPages
            .flatMap { (pick, page) ->
                val singles = page.singles?.results.orEmpty().map { Release(it.browseId, it.title, it.year, it.thumbnails) }
                val albums = page.albums?.results.orEmpty().map { Release(it.browseId, it.title, it.year, it.thumbnails) }
                (singles.take(4) + albums.take(3)).map { pick to it }
            }.distinctBy { (_, release) -> release.browseId }
            // Stable, so releases of one year stay in YouTube's own newest-first order.
            .sortedByDescending { (_, release) -> release.yearNumber ?: 0 }
            .map { (pick, release) ->
                val about = listOfNotNull(pick.title, release.yearNumber?.toString()).joinToString(" • ")
                pageContent(release.browseId, release.title, about, release.thumbnails.lastOrNull()?.url)
            },
        subtitle = labels.basedOnPicks,
    )

    artistPages.zip(topSongs).forEach { (artist, songs) ->
        val (pick, _) = artist
        shelf(pick.title, songs.distinctBy { it.videoId }, subtitle = labels.moreFrom, channelId = pick.id)
    }

    val related = artistPages.map { (_, page) -> page.related?.results.orEmpty().filter { it.browseId.startsWith("UC") } }
    val timesNamed = related.flatten().groupingBy { it.browseId }.eachCount()
    shelf(
        labels.similar,
        related
            .interleave()
            .filter { it.browseId !in pickedArtists }
            .distinctBy { it.browseId }
            // An artist several of the picks lead to is the safer bet.
            .sortedByDescending { timesNamed[it.browseId] ?: 0 }
            .map { pageContent(it.browseId, it.title, it.subscribers.takeIf(String::isNotBlank), it.thumbnails.lastOrNull()?.url) },
        subtitle = labels.basedOnPicks,
    )
    return shelves
}

private class Release(
    val browseId: String,
    val title: String,
    year: String,
    val thumbnails: List<Thumbnail>,
) {
    // YouTube's field can read "Single • 2024" or, where it sent none, the word "null".
    val yearNumber: Int? = Regex("(19|20)\\d\\d").find(year)?.value?.toInt()
}

/** An artist or a release: a card that opens a page rather than playing. */
private fun pageContent(
    browseId: String,
    title: String,
    description: String?,
    thumbnail: String?,
) = Content(
    album = null,
    artists = null,
    description = description,
    isExplicit = false,
    playlistId = null,
    browseId = browseId,
    thumbnails = listOfNotNull(thumbnail?.let(::squareThumbnail)),
    title = title,
    videoId = null,
    views = null,
)

private fun songContent(
    videoId: String,
    title: String,
    artists: List<Artist>?,
    album: Album?,
    thumbnails: List<Thumbnail>,
    isExplicit: Boolean = false,
    durationSeconds: Int? = null,
    videoType: String? = null,
) = Content(
    album = album?.takeIf { it.id.isNotBlank() },
    artists = artists,
    description = null,
    isExplicit = isExplicit,
    playlistId = null,
    browseId = null,
    // Only a square picture may be enlarged to one; a video's wide frame keeps its shape and its card.
    thumbnails = thumbnails.map { if (it.width == it.height) squareThumbnail(it.url) else it },
    title = title,
    videoId = videoId,
    views = null,
    durationSeconds = durationSeconds,
    videoType = videoType,
)

/** Rows come with list-sized art (`=w120-h120`); a card needs the same picture larger. */
private fun squareThumbnail(url: String) = Thumbnail(544, url.replace(Regex("=w\\d+-h\\d+"), "=w544-h544"), 544)

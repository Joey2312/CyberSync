package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.simpmusic.utils.HomePick
import com.maxrave.simpmusic.utils.HomePicks
import com.maxrave.simpmusic.utils.dataOrNull
import com.maxrave.simpmusic.utils.homePicks
import com.maxrave.simpmusic.utils.interleave
import com.maxrave.simpmusic.utils.saveHomePicks
import com.maxrave.simpmusic.utils.toggle
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PickStep { ARTISTS, SONGS }

/** What the list under the search field shows. [step] says which kind of thing [items] holds. */
data class PickResults(
    val step: PickStep,
    val items: List<HomePick> = emptyList(),
    val isSuggestions: Boolean = true,
    val loading: Boolean = false,
    val failed: Boolean = false,
)

class HomePicksViewModel(
    private val dataStoreManager: DataStoreManager,
    private val searchRepository: SearchRepository,
    private val artistRepository: ArtistRepository,
    private val songRepository: SongRepository,
    private val homeRepository: HomeRepository,
) : BaseViewModel() {
    private val _picks = MutableStateFlow(HomePicks())
    val picks: StateFlow<HomePicks> = _picks

    private val _step = MutableStateFlow(PickStep.ARTISTS)
    val step: StateFlow<PickStep> = _step

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow(PickResults(PickStep.ARTISTS, loading = true))
    val results: StateFlow<PickResults> = _results

    private var saving = false

    init {
        viewModelScope.launch {
            dataStoreManager.homePicks.first()?.let { _picks.value = it }
            combine(step, query) { step, query -> step to query.trim() }.collectLatest { (step, query) ->
                if (query.isEmpty()) {
                    _results.value = PickResults(step, loading = true)
                    _results.value = PickResults(step, suggestions(step))
                } else {
                    // The previous results stay up while the next ones load, so typing does not flicker.
                    _results.update { (if (it.step == step) it else PickResults(step)).copy(loading = true, failed = false) }
                    delay(350)
                    val found = search(step, query)
                    _results.value = PickResults(step, found.orEmpty(), isSuggestions = false, failed = found == null)
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun goTo(step: PickStep) {
        _query.value = ""
        _step.value = step
    }

    /** Returns false when the list is full and [pick] was not taken. */
    fun toggle(pick: HomePick): Boolean {
        val before = _picks.value
        _picks.value =
            when (_step.value) {
                PickStep.ARTISTS -> before.copy(artists = before.artists.toggle(pick))
                PickStep.SONGS -> before.copy(songs = before.songs.toggle(pick))
            }
        return _picks.value != before
    }

    /** [onSaved] leaves the screen, and with it this scope, so it has to wait for the write. */
    fun save(onSaved: () -> Unit) {
        if (saving) return
        saving = true
        viewModelScope.launch {
            dataStoreManager.saveHomePicks(_picks.value)
            onSaved()
        }
    }

    private suspend fun search(
        step: PickStep,
        query: String,
    ): List<HomePick>? =
        when (step) {
            PickStep.ARTISTS ->
                searchRepository.getSearchDataArtist(query).dataOrNull()?.map {
                    HomePick(it.browseId, it.artist, thumbnail = it.thumbnails.lastOrNull()?.url)
                }

            PickStep.SONGS ->
                searchRepository.getSearchDataSong(query).dataOrNull()?.map {
                    HomePick(it.videoId, it.title.orEmpty(), it.artists.orEmpty(), it.thumbnails?.lastOrNull()?.url)
                }
        }?.usable(step)

    private suspend fun suggestions(step: PickStep): List<HomePick> =
        when (step) {
            PickStep.ARTISTS -> {
                val followed = artistRepository.getFollowedArtists().catch { }.firstOrNull().orEmpty()
                val chart = homeRepository.getChartData(dataStoreManager.chartKey.first()).dataOrNull()
                followed.map { HomePick(it.channelId, it.name, thumbnail = it.thumbnails) } +
                    chart?.artists?.itemArtists.orEmpty().map {
                        HomePick(it.browseId, it.title, thumbnail = it.thumbnails.lastOrNull()?.url)
                    }
            }

            PickStep.SONGS -> {
                val byArtist =
                    coroutineScope {
                        _picks.value.artists
                            .map { async { artistRepository.getArtistData(it.id).dataOrNull() } }
                            .awaitAll()
                    }.map { page ->
                        page?.songs?.results.orEmpty().map {
                            HomePick(it.videoId, it.title, it.artists.orEmpty(), it.thumbnails.lastOrNull()?.url)
                        }
                    }
                val played = songRepository.getMostPlayedSongs().catch { }.firstOrNull().orEmpty()
                byArtist.interleave() +
                    played.take(20).map { song ->
                        val ids = song.artistId.orEmpty()
                        HomePick(
                            song.videoId,
                            song.title,
                            song.artistName.orEmpty().mapIndexed { i, name -> Artist(ids.getOrNull(i), name) },
                            song.thumbnails,
                        )
                    }
            }
        }.usable(step)

    // A search can also return channels that are not artist pages, which Home would open as albums.
    private fun List<HomePick>.usable(step: PickStep) =
        filter { it.title.isNotBlank() && (step == PickStep.SONGS || it.id.startsWith("UC")) }.distinctBy { it.id }
}

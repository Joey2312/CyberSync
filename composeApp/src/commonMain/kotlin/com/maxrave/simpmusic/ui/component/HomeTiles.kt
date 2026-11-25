package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.simpmusic.ui.component.AsyncImage
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toListName
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.expect.ui.HorizontalScrollBar
import com.maxrave.simpmusic.extension.mouseDragScroll
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.ArtworkShape
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.cyberBlock
import com.maxrave.simpmusic.ui.theme.monoFontFamily
import com.maxrave.simpmusic.ui.theme.neonCyan
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.HomeViewModel
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.album
import simpmusic.composeapp.generated.resources.artist
import simpmusic.composeapp.generated.resources.play_all
import simpmusic.composeapp.generated.resources.playlist

/** Home draws its shelves as rectangular tiles, like song rows, instead of artwork cards. */
val HOME_AS_TILES: Boolean = true

private val TileMinWidth = 290.dp
private val TileHeight = 64.dp
private val TileGap = 8.dp
private const val MAX_ROWS = 3

/** A Home shelf as tiles: same header, taps and long-press menu as the card shelves. */
@Composable
fun HomeTileShelf(
    data: HomeItem,
    navController: NavController,
    viewModel: HomeViewModel,
) {
    var sheetTrack by remember { mutableStateOf<Track?>(null) }
    sheetTrack?.let { track ->
        NowPlayingBottomSheet(
            onDismiss = { sheetTrack = null },
            song = track.toSongEntity(),
            navController = navController,
        )
    }
    val playingId by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val contents = remember(data.contents) { data.contents.filterNotNull() }
    Column {
        HomeSectionHeader(data = data, navController = navController, onPlayAll = { viewModel.playAll(data) })
        HomeTileGrid(contents) { content, modifier ->
            val isSong = !content.videoId.isNullOrEmpty()
            HomeTile(
                title = content.title,
                subtitle = content.tileSubtitle(),
                thumbnail = content.thumbnails.lastOrNull()?.url,
                modifier = modifier,
                isPlaying = isSong && content.videoId == playingId,
                isExplicit = content.isExplicit == true,
                isLive = content.isLive,
                onLongClick = if (isSong) ({ sheetTrack = content.toTrack() }) else null,
                // A song's own click already plays it.
                onPlay = if (isSong) null else content.hoverPlay(viewModel),
                onClick = { content.open(navController, viewModel) },
            )
        }
    }
}

@Composable
private fun Content.tileSubtitle(): String {
    val names = artists?.takeIf { it.isNotEmpty() }?.toListName()?.connectArtists()
    return when {
        !videoId.isNullOrEmpty() ->
            listOfNotNull(names, album?.name ?: views).filter { it.isNotBlank() }.joinToString(" • ")
        (browseId ?: playlistId).orEmpty().startsWith("UC") ->
            description?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.artist)
        else ->
            description?.takeIf { it.isNotBlank() } ?: names
                ?: stringResource(if (playlistId == null) Res.string.album else Res.string.playlist)
    }
}

/**
 * Tiles in up to [MAX_ROWS] rows. The columns share the width exactly, so none is cut through at the
 * edge; whatever does not fit scrolls sideways, a column at a time.
 */
@Composable
fun <T> HomeTileGrid(
    items: List<T>,
    tile: @Composable (T, Modifier) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        val columns = ((maxWidth + TileGap) / (TileMinWidth + TileGap)).toInt().coerceAtLeast(1)
        val rows = ((items.size + columns - 1) / columns).coerceIn(1, MAX_ROWS)
        // On a phone's single column the next one shows its edge, or nothing would say there is more.
        val peek = if (columns == 1 && items.size > rows) 0.9f else 1f
        val width = (maxWidth - TileGap * (columns - 1)) / columns * peek
        val state = rememberLazyGridState()
        val fling = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = state, snapPosition = SnapPosition.Start))
        Column {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(rows),
                state = state,
                flingBehavior = fling,
                horizontalArrangement = Arrangement.spacedBy(TileGap),
                verticalArrangement = Arrangement.spacedBy(TileGap),
                modifier = Modifier.height(TileHeight * rows + TileGap * (rows - 1)).mouseDragScroll(state, fling),
            ) {
                items(items) { tile(it, Modifier.width(width)) }
            }
            if (items.size > rows * columns) {
                HorizontalScrollBar(Modifier.fillMaxWidth().padding(top = 4.dp), scrollState = state, flingBehavior = fling)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTile(
    title: String,
    subtitle: String,
    thumbnail: String?,
    modifier: Modifier = Modifier,
    rank: String? = null,
    isPlaying: Boolean = false,
    isExplicit: Boolean = false,
    isLive: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusedAsState()
    val active = hovered || focused
    val dark = LocalIsDarkTheme.current
    val accent = if (dark) neonCyan else MaterialTheme.colorScheme.primary
    Row(
        modifier
            .height(TileHeight)
            .then(
                if (dark) {
                    Modifier
                } else {
                    Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                },
            ).cyberBlock(active = isPlaying || active)
            .combinedClickable(
                interactionSource = source,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick,
            ).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rank != null) {
            Text(
                rank,
                style = TextStyle(fontFamily = monoFontFamily(), fontSize = 14.sp, color = accent),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(end = 8.dp).width(28.dp),
            )
        }
        if (!NO_ARTWORK || isPlaying) Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            if (isPlaying) {
                AudioPlayingIndicator(Modifier.fillMaxSize())
            } else {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(ArtworkShape),
                )
            }
        }
        // A name too long for its tile scrolls while the pointer or focus is on it.
        val marquee = if (active) Modifier.basicMarquee(initialDelayMillis = 600, velocity = 30.dp) else Modifier
        Column(Modifier.weight(1f).padding(start = if (NO_ARTWORK && !isPlaying) 6.dp else 12.dp)) {
            Text(
                title,
                style = typo().titleSmall,
                color = if (isPlaying) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = marquee,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isExplicit) ExplicitBadge(Modifier.size(20.dp).padding(end = 4.dp))
                if (isLive) LiveBadge(Modifier.padding(end = 6.dp))
                Text(subtitle, style = typo().bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = marquee)
            }
        }
        if (onPlay != null && active) {
            IconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
                Icon(SimpIcons.PlayArrow, contentDescription = stringResource(Res.string.play_all), tint = accent, modifier = Modifier.size(26.dp))
            }
        }
    }
}

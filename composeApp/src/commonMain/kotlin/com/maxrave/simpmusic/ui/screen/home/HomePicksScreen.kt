package com.maxrave.simpmusic.ui.screen.home

import com.maxrave.simpmusic.ui.component.NO_ARTWORK
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.simpmusic.ui.component.AsyncImage
import com.maxrave.simpmusic.expect.ui.PlatformBackHandler
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.HomePicksDestination
import com.maxrave.simpmusic.ui.theme.Button
import com.maxrave.simpmusic.ui.theme.CyberButtonShape
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.OutlinedButton
import com.maxrave.simpmusic.ui.theme.cyber
import com.maxrave.simpmusic.ui.theme.cyberBlock
import com.maxrave.simpmusic.ui.theme.displayFontFamily
import com.maxrave.simpmusic.ui.theme.monoFontFamily
import com.maxrave.simpmusic.ui.theme.neonCyan
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.HomePick
import com.maxrave.simpmusic.utils.MAX_HOME_PICKS
import com.maxrave.simpmusic.viewModel.HomePicksViewModel
import com.maxrave.simpmusic.viewModel.PickStep
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.home_picks_artists_title
import simpmusic.composeapp.generated.resources.home_picks_back
import simpmusic.composeapp.generated.resources.home_picks_clear
import simpmusic.composeapp.generated.resources.home_picks_done
import simpmusic.composeapp.generated.resources.home_picks_failed
import simpmusic.composeapp.generated.resources.home_picks_full
import simpmusic.composeapp.generated.resources.home_picks_hint
import simpmusic.composeapp.generated.resources.home_picks_next
import simpmusic.composeapp.generated.resources.home_picks_no_match
import simpmusic.composeapp.generated.resources.home_picks_remove
import simpmusic.composeapp.generated.resources.home_picks_scanning
import simpmusic.composeapp.generated.resources.home_picks_search_artists
import simpmusic.composeapp.generated.resources.home_picks_search_songs
import simpmusic.composeapp.generated.resources.home_picks_skip
import simpmusic.composeapp.generated.resources.home_picks_songs_title
import simpmusic.composeapp.generated.resources.home_picks_start_typing
import simpmusic.composeapp.generated.resources.home_picks_suggestions
import simpmusic.composeapp.generated.resources.save

private val ThumbShape = RoundedCornerShape(6.dp).cyber()

/**
 * Two steps, artists then songs, each up to [MAX_HOME_PICKS]. Every step may be left short or empty.
 * On first launch leaving any way counts as an answer, so it is never asked again; from Settings,
 * leaving without saving keeps the old picks.
 */
@Composable
fun HomePicksScreen(
    firstRun: Boolean,
    navController: NavController,
    hideNavBar: () -> Unit,
    showNavBar: () -> Unit,
    viewModel: HomePicksViewModel = koinViewModel(),
) {
    val picks by viewModel.picks.collectAsStateWithLifecycle()
    val step by viewModel.step.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        hideNavBar()
        onDispose { showNavBar() }
    }

    // By route, so a second tap during the exit animation finds nothing left to pop.
    val leave: () -> Unit = { navController.popBackStack<HomePicksDestination>(inclusive = true) }
    val saveAndLeave: () -> Unit = { viewModel.save(leave) }
    val onBack: () -> Unit = {
        when {
            step == PickStep.SONGS -> viewModel.goTo(PickStep.ARTISTS)
            firstRun -> saveAndLeave()
            else -> leave()
        }
    }
    PlatformBackHandler(enabled = true, onBack = onBack)

    val cyber = LocalIsDarkTheme.current
    val accent = if (cyber) neonCyan else MaterialTheme.colorScheme.primary
    val label = TextStyle(fontFamily = monoFontFamily(), fontSize = 11.sp, letterSpacing = 1.sp, color = accent)
    val isArtists = step == PickStep.ARTISTS
    val chosen = if (isArtists) picks.artists else picks.songs
    val full = chosen.size >= MAX_HOME_PICKS
    val fullMessage = stringResource(Res.string.home_picks_full)
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    LaunchedEffect(step, query) { listState.scrollToItem(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("// ${if (isArtists) 1 else 2}/2", style = label)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.padding(end = 10.dp).size(width = 4.dp, height = 26.dp).background(accent))
                Text(
                    stringResource(if (isArtists) Res.string.home_picks_artists_title else Res.string.home_picks_songs_title)
                        .let { if (cyber) it.uppercase() else it },
                    style =
                        TextStyle(
                            fontFamily = displayFontFamily(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    modifier = Modifier.weight(1f),
                )
                Text("${chosen.size}/$MAX_HOME_PICKS", style = label.copy(fontSize = 16.sp))
            }
            Text(
                stringResource(Res.string.home_picks_hint),
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(MAX_HOME_PICKS) { i ->
                    PickSlot(i, chosen.getOrNull(i), label, Modifier.weight(1f)) { viewModel.toggle(it) }
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                singleLine = true,
                textStyle = typo().bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                shape = if (cyber) CyberButtonShape else OutlinedTextFieldDefaults.shape,
                placeholder = {
                    Text(
                        stringResource(if (isArtists) Res.string.home_picks_search_artists else Res.string.home_picks_search_songs),
                        style = typo().bodyMedium,
                    )
                },
                leadingIcon = { Icon(SimpIcons.Search, contentDescription = null, tint = accent) },
                trailingIcon =
                    if (query.isEmpty()) {
                        null
                    } else {
                        {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(SimpIcons.Close, contentDescription = stringResource(Res.string.home_picks_clear))
                            }
                        }
                    },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            // A step change swaps the kind of result; never offer an artist while picking songs.
            val items = if (results.step == step) results.items else emptyList()
            val status =
                when {
                    results.loading || results.step != step -> stringResource(Res.string.home_picks_scanning)
                    results.failed -> stringResource(Res.string.home_picks_failed)
                    items.isEmpty() && results.isSuggestions -> stringResource(Res.string.home_picks_start_typing)
                    items.isEmpty() -> stringResource(Res.string.home_picks_no_match)
                    results.isSuggestions -> stringResource(Res.string.home_picks_suggestions)
                    else -> ""
                }
            Text(if (status.isEmpty()) "" else "// " + status.uppercase(), style = label, modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(items, key = { it.id }) { pick ->
                    val selected = chosen.any { it.id == pick.id }
                    PickRow(pick, selected, dimmed = full && !selected) {
                        if (!viewModel.toggle(pick)) viewModel.makeToast(fullMessage)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            when {
                                !isArtists -> Res.string.home_picks_back
                                firstRun -> Res.string.home_picks_skip
                                else -> Res.string.cancel
                            },
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Button(
                    onClick = { if (isArtists) viewModel.goTo(PickStep.SONGS) else saveAndLeave() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(
                            when {
                                isArtists -> Res.string.home_picks_next
                                firstRun -> Res.string.home_picks_done
                                else -> Res.string.save
                            },
                        ),
                        // Named: the app's label style carries its own grey, which wins over the button's.
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** One of the numbered places at the top; tapping a filled one empties it. */
@Composable
private fun PickSlot(
    index: Int,
    pick: HomePick?,
    label: TextStyle,
    modifier: Modifier,
    onRemove: (HomePick) -> Unit,
) {
    val removeLabel = pick?.let { stringResource(Res.string.home_picks_remove, it.title) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .widthIn(max = 80.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .cyberBlock(active = pick != null)
                .then(
                    if (LocalIsDarkTheme.current) Modifier else Modifier.clip(ThumbShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ).then(if (pick != null) Modifier.clickable(onClickLabel = removeLabel) { onRemove(pick) } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (pick == null) {
                Text("0${index + 1}", style = label.copy(color = label.color.copy(alpha = 0.5f)))
            } else {
                if (NO_ARTWORK) {
                    Text(
                        pick.title,
                        style = typo().bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                } else {
                    PickArt(pick, Modifier.fillMaxSize())
                }
                Icon(
                    SimpIcons.Close,
                    contentDescription = removeLabel,
                    tint = label.color,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
                            .size(16.dp),
                )
            }
        }
        if (!NO_ARTWORK) Text(
            pick?.title.orEmpty(),
            style = typo().bodySmall.copy(fontSize = 10.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun PickRow(
    pick: HomePick,
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (dimmed) 0.4f else 1f)
            .cyberBlock(active = selected)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PickArt(pick, Modifier.size(44.dp).clip(ThumbShape))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                pick.title,
                style = typo().titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (pick.artists.isNotEmpty()) {
                Text(
                    pick.artists.joinToString { it.name },
                    style = typo().bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            if (selected) SimpIcons.Check else SimpIcons.Add,
            contentDescription = null,
            tint = if (LocalIsDarkTheme.current) neonCyan else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun PickArt(
    pick: HomePick,
    modifier: Modifier,
) {
    AsyncImage(
        model = pick.thumbnail,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = rememberHolderPainter(),
        error = rememberHolderPainter(),
        modifier = modifier,
    )
}

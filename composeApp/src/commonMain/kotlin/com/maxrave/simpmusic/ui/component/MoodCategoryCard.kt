package com.maxrave.simpmusic.ui.component

import com.maxrave.simpmusic.ui.theme.cyber
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.component.AsyncImage
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.ui.theme.ArtworkShape
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.painterResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.monochrome

/**
 * A "Moods & Genres" browse category tile: the [playlistTitleGradient] and SimpMusic badge of an
 * artwork-less playlist tile, plus the tilted cover Spotify puts on its browse cards.
 *
 * [artworkUrl] is null until resolved — the category list carries no artwork at all, so the cover
 * costs a separate browse per category and arrives late. The tile is designed to look finished
 * without it.
 */
@Composable
fun MoodCategoryCard(
    title: String,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    if (com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme.current) {
        CyberMoodCategoryCard(title, artworkUrl, modifier, onClick)
        return
    }
    val badge = painterResource(Res.drawable.monochrome)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(2f)
                .clip(RoundedCornerShape(10.dp).cyber())
                .angledGradientBackground(
                    colors = playlistTitleGradient(title),
                    degrees = 45f,
                ).drawBehind {
                    // PlaylistThumbnailPainter sizes the badge off the width because its tile is
                    // square. This one is 2:1, so the same fractions would double it — anchor on
                    // the height instead to keep the badge the size the eye expects.
                    val radius = size.height * 0.09f
                    // The corner opposite the title, which starts on the right in a right-to-left
                    // language; drawn at a fixed right corner, the badge sat on top of the title.
                    val centerX = if (layoutDirection == LayoutDirection.Rtl) radius * 2f else size.width - radius * 2f
                    val centerY = radius * 2f
                    drawCircle(
                        center = Offset(centerX, centerY),
                        color = Color.White,
                        radius = radius,
                    )
                    val badgeSize = size.copy(width = radius * 3f, height = radius * 3f)
                    translate(
                        left = centerX - badgeSize.width / 2f,
                        top = centerY - badgeSize.height / 2f,
                    ) {
                        with(badge) {
                            draw(badgeSize, alpha = 0.2f)
                        }
                    }
                }.clickable(onClick = onClick),
    ) {
        if (artworkUrl != null) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        // Offset past the corner first, THEN rotate: the tile clips its children,
                        // so letting the cover run off the edge is what produces the cut-off
                        // diagonal instead of a square pasted inside the card.
                        .offset(x = 8.dp, y = 12.dp)
                        .size(64.dp)
                        .rotate(25f)
                        .clip(ArtworkShape),
            )
        }
        Text(
            text = title,
            style = typo().titleSmall,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    // Safe width ends at the LEFT edge of the tilted cover, not just clear of the
                    // badge. The cover is 64.dp rotated 25°, so its bounding box grows to
                    // 64*(cos25+sin25) ≈ 85.dp — about 10.dp wider on each side — and it is offset
                    // 8.dp past the right edge. That leaves ~66.dp of it inside the tile.
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 72.dp),
        )
    }
}

/**
 * CyberSync tile: dark panel, one cut corner, 1dp cyan edge that lights up on hover, the cover
 * square on the right fading into the panel, title in capitals in the display face.
 */
@Composable
private fun CyberMoodCategoryCard(
    title: String,
    artworkUrl: String?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = androidx.compose.foundation.shape.CutCornerShape(topStart = 10.dp)
    val interaction = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val panel = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow
    val accent = com.maxrave.simpmusic.ui.theme.neonCyan
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(2f)
                .clip(shape)
                .background(panel)
                .border(1.dp, if (hovered) accent else accent.copy(alpha = 0.22f), shape)
                .hoverable(interaction)
                .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = onClick),
    ) {
        if (artworkUrl != null && !NO_ARTWORK) {
            Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().aspectRatio(1f)) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // Fade the cover's left edge into the panel so it reads as part of the tile.
                Box(
                    Modifier.fillMaxSize().background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(panel, panel.copy(alpha = 0.35f), Color.Transparent)),
                    ),
                )
            }
        }
        // Accent tick on the top edge, same mark the panels carry.
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp)
                .size(width = 28.dp, height = 2.dp)
                .background(if (hovered) accent else accent.copy(alpha = 0.6f)),
        )
        Text(
            text = title.uppercase(),
            style =
                androidx.compose.ui.text.TextStyle(
                    fontFamily = com.maxrave.simpmusic.ui.theme.displayFontFamily(),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    fontSize = 15.sp,
                    letterSpacing = 1.5.sp,
                ),
            color = if (hovered) accent else Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 14.dp, end = if (NO_ARTWORK) 10.dp else 72.dp),
        )
    }
}

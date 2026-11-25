package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImage as CoilAsyncImage

/** CyberSync draws no artwork at all: no covers, portraits, avatars or backdrops. */
val NO_ARTWORK: Boolean = true

/**
 * Same-named stand-in for Coil's AsyncImage, imported instead of it across the app. With [NO_ARTWORK]
 * it loads and draws nothing and takes no room, so the text beside it moves up into its place.
 * [keepSpace] is for a picture whose size something else depends on: the live sound bar is laid out
 * over the player's artwork.
 */
@Composable
fun AsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: Painter? = null,
    error: Painter? = null,
    fallback: Painter? = error,
    onLoading: ((AsyncImagePainter.State.Loading) -> Unit)? = null,
    onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    keepSpace: Boolean = false,
) {
    if (NO_ARTWORK) {
        if (keepSpace) Spacer(modifier)
        return
    }
    CoilAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        placeholder = placeholder,
        error = error,
        fallback = fallback,
        onLoading = onLoading,
        onSuccess = onSuccess,
        onError = onError,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
    )
}

package com.maxrave.simpmusic.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

private const val SEGMENTS = 24
private const val HEX = "0123456789ABCDEF"

/** One fill-and-flash pass of [CyberLoader]'s bar. */
private const val CYCLE_MS = 2600L

/** Compose's default Crossfade / AnimatedVisibility fade length. */
private const val FADE_MS = 300L

/**
 * Holds a loading [value] on screen until [CyberLoader]'s current pass has filled and flashed, so the
 * page never cuts in mid-fill. Pass the screen's state; it comes back unchanged once the pass ends.
 * Light theme draws Material's indicator instead, which has no pass to finish, so nothing is held there.
 */
@Composable
fun <T> rememberLoaderHeld(value: T, isLoading: (T) -> Boolean): T {
    if (!LocalIsDarkTheme.current) return value
    var shown by remember { mutableStateOf(value) }
    val startedAt = remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    LaunchedEffect(value) {
        if (isLoading(value)) {
            if (!isLoading(shown)) startedAt.value = TimeSource.Monotonic.markNow()
        } else if (isLoading(shown)) {
            // Let go ~300 ms early: the page's crossfade then fades the loader out over the full-bar
            // flash, rather than over the bar snapping back to empty. Inside that window, go at once.
            val inCycle = startedAt.value.elapsedNow().inWholeMilliseconds % CYCLE_MS
            delay((CYCLE_MS - FADE_MS - inCycle).coerceAtLeast(0))
        }
        shown = value
    }
    // A new loading value shows at once (the loader restarts with it); only leaving loading waits.
    return if (isLoading(value) || !isLoading(shown)) value else shown
}

@Composable
fun rememberLoaderHeld(loading: Boolean): Boolean = rememberLoaderHeld(loading) { it }

/**
 * CyberSync's loading screen: a LOADING label with a blinking block cursor, a segmented bar that
 * fills left to right with a bright head and flashes when full, and a slow hex ticker underneath.
 * Indeterminate by design, so it never shows a percentage it does not have.
 */
@Composable
fun CyberLoader(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cyberLoader")
    // 0..1 fill, then a short hold (1..1.25) where the full bar flashes before restarting.
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "fill",
    )
    val blink by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "cursor",
    )
    var ticker by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val rnd = kotlin.random.Random.Default
        while (true) {
            ticker =
                (0 until 4).joinToString("  ") {
                    "0x" + (0 until 4).map { HEX[rnd.nextInt(16)] }.joinToString("")
                }
            delay(450)
        }
    }

    val mono = monoFontFamily()
    val dim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    Column(modifier) {
        Row {
            Text(
                "LOADING",
                style = TextStyle(fontFamily = displayFontFamily(), fontSize = 14.sp, letterSpacing = 4.sp),
                color = neonCyan,
            )
            Text(
                if (blink < 0.5f) " _" else "  ",
                style = TextStyle(fontFamily = mono, fontSize = 14.sp),
                color = neonCyan,
            )
        }
        Spacer(Modifier.height(10.dp))
        Canvas(Modifier.width(264.dp).height(10.dp)) {
            val gap = 3.dp.toPx()
            val segW = (size.width - gap * (SEGMENTS - 1)) / SEGMENTS
            val cut = 3.dp.toPx()
            val fill = cycle.coerceAtMost(1f) * SEGMENTS
            val holding = cycle > 1f
            for (i in 0 until SEGMENTS) {
                val x = i * (segW + gap)
                val seg =
                    Path().apply {
                        moveTo(x + cut, 0f)
                        lineTo(x + segW, 0f)
                        lineTo(x + segW - cut, size.height)
                        lineTo(x, size.height)
                        close()
                    }
                val alpha =
                    when {
                        holding -> if (((cycle - 1f) * 16).toInt() % 2 == 0) 0.95f else 0.35f
                        i < fill - 1 -> 0.55f
                        i < fill -> 1f // the head
                        else -> 0.10f
                    }
                drawPath(seg, neonCyan.copy(alpha = alpha))
            }
            // Thin baseline under the bar.
            drawLine(neonCyan.copy(alpha = 0.2f), Offset(0f, size.height + 4.dp.toPx()), Offset(size.width, size.height + 4.dp.toPx()), 1.dp.toPx())
        }
        Spacer(Modifier.height(12.dp))
        Text(ticker, style = TextStyle(fontFamily = mono, fontSize = 10.sp, letterSpacing = 1.sp), color = dim)
    }
}

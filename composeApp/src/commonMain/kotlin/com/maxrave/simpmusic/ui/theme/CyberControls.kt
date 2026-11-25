package com.maxrave.simpmusic.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Buttons lose two opposite corners: the machined chamfer every cyber control shares. */
val CyberButtonShape: Shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)

private val buttonFill = Color(0xFF0E0820)

/**
 * A button surface. Dark theme: chamfered panel with a 1dp cyan edge, or (primary) solid cyan.
 * Light theme keeps the caller's original [lightShape] and [lightColor].
 */
@Composable
fun Modifier.cyberButton(
    lightColor: Color,
    primary: Boolean = false,
    lightShape: Shape = CircleShape,
): Modifier =
    if (!LocalIsDarkTheme.current) {
        clip(lightShape).background(lightColor)
    } else if (primary) {
        clip(CyberButtonShape).background(neonCyan)
    } else {
        clip(CyberButtonShape)
            .background(buttonFill)
            .border(1.dp, neonCyan.copy(alpha = 0.45f), CyberButtonShape)
    }

/** Song rows as panels: a dark block with a cut corner and a dim edge (dark theme only). */
@Composable
fun Modifier.cyberBlock(active: Boolean = false): Modifier =
    if (!LocalIsDarkTheme.current) {
        this
    } else {
        val shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
        clip(shape)
            .background(buttonFill)
            .border(1.dp, neonCyan.copy(alpha = if (active) 0.75f else 0.18f), shape)
    }

/**
 * The song-length strip from the Now Playing artwork, as a control: slanted segments filled up to
 * the playhead, mono times either side. Tap or drag to seek; [progress] and the callbacks are 0..1.
 */
@Composable
fun CyberTimeline(
    progress: Float,
    elapsed: String,
    total: String,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
    segmentHeight: Dp = 6.dp,
    timeSize: Float = 11f,
) {
    val style = TextStyle(fontFamily = monoFontFamily(), fontSize = timeSize.sp, color = neonCyan.copy(alpha = 0.8f))
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(elapsed, style = style, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        Canvas(
            Modifier
                .weight(1f)
                .height(segmentHeight + 10.dp)
                .pointerInput(Unit) {
                    detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)); onSeekFinished() }
                }.pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = onSeekFinished,
                        onDragCancel = onSeekFinished,
                    ) { change, _ -> onSeek((change.position.x / size.width).coerceIn(0f, 1f)) }
                },
        ) {
            val unit = 1.dp.toPx()
            val gap = 2f * unit
            val n = (size.width / (7f * unit)).toInt().coerceIn(12, 64)
            val sw = (size.width - gap * (n - 1)) / n
            val sh = segmentHeight.toPx()
            val top = (size.height - sh) / 2f
            val slant = minOf(sw * 0.4f, 2f * unit)
            val p = progress.coerceIn(0f, 1f)
            val head = (p * n).toInt().coerceAtMost(n - 1)
            for (i in 0 until n) {
                val x = i * (sw + gap)
                val alpha =
                    when {
                        i < head -> 0.75f
                        i == head -> 1f
                        else -> 0.12f
                    }
                val path =
                    Path().apply {
                        moveTo(x + slant, top)
                        lineTo(x + sw, top)
                        lineTo(x + sw - slant, top + sh)
                        lineTo(x, top + sh)
                        close()
                    }
                drawPath(path, neonCyan.copy(alpha = alpha))
            }
            // Playhead tick.
            val hx = (p * size.width).coerceIn(minOf(unit, size.width / 2), maxOf(size.width - unit, size.width / 2))
            drawLine(neonCyan, Offset(hx, 0f), Offset(hx, size.height), strokeWidth = 1.5f * unit)
        }
        Spacer(Modifier.width(8.dp))
        Text(total, style = style, maxLines = 1)
    }
}

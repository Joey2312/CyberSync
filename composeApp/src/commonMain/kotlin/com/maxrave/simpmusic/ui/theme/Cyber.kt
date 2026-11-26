package com.maxrave.simpmusic.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

// CyberSync visual language. Rules: one accent (cyan); magenta only for likes and alerts; flat
// 1dp lines, no glow, no gradients; every readout shows something real.

private val CutSize = 14.dp

/** Panels lose one corner, top-left, the way a terminal window would be machined. */
val CyberPanelShape: Shape = CutCornerShape(topStart = CutSize)

/** Material surfaces (cards, dialogs, menus, chips, sheets) take small cut corners. */
val CyberShapes =
    Shapes(
        extraSmall = CutCornerShape(2.dp),
        small = CutCornerShape(3.dp),
        medium = CutCornerShape(topStart = 6.dp),
        large = CutCornerShape(topStart = 10.dp),
        extraLarge = CutCornerShape(topStart = CutSize),
    )

/**
 * A flat 1dp frame in dim cyan, plus one short bright tick on the top edge just past the cut:
 * a single deliberate highlight instead of a glow all round.
 */
fun Modifier.neonFrame(shape: Shape = CyberPanelShape): Modifier =
    drawWithCache {
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val line = 1.dp.toPx()
        val tickStart = CutSize.toPx() + 10.dp.toPx()
        val tickEnd = (tickStart + 48.dp.toPx()).coerceAtMost(size.width)
        onDrawWithContent {
            drawContent()
            drawPath(path, neonCyan.copy(alpha = 0.28f), style = Stroke(width = line))
            drawLine(neonCyan, Offset(tickStart, line), Offset(tickEnd, line), strokeWidth = 2.dp.toPx())
        }
    }

private val TechStyle: TextStyle
    @Composable get() = TextStyle(fontFamily = monoFontFamily(), fontSize = 11.sp, letterSpacing = 1.sp)

/** Wordmark: a solid accent block and the name in the display face. No glow. */
@Composable
fun CyberSyncLogo(
    collapsed: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 6.dp, height = 22.dp).background(neonCyan))
        Spacer(Modifier.width(10.dp))
        Text(
            if (collapsed) "CS" else "CYBERSYNC",
            style =
                TextStyle(
                    fontFamily = displayFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
        )
    }
}

/**
 * A menu entry: dim two-digit index, label in capitals. Selected = accent bar on the left edge,
 * a faint accent wash and accent text. Restraint does the highlighting, not a full fill.
 */
@Composable
fun CyberMenuEntry(
    index: Int,
    label: String,
    selected: Boolean,
    collapsed: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val fg = if (selected) neonCyan else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .padding(horizontal = 8.dp)
            .then(if (collapsed) Modifier.size(48.dp) else Modifier.fillMaxWidth().height(42.dp))
            .clip(CutCornerShape(topStart = 6.dp))
            .then(if (selected) Modifier.background(neonCyan.copy(alpha = 0.10f)) else Modifier)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
    ) {
        if (collapsed) {
            CompositionLocalProvider(LocalContentColor provides fg) { icon() }
            return@Row
        }
        Box(Modifier.width(3.dp).fillMaxHeight().background(if (selected) neonCyan else Color.Transparent))
        Spacer(Modifier.width(14.dp))
        Text(index.toString().padStart(2, '0'), style = TechStyle, color = fg.copy(alpha = 0.55f))
        Spacer(Modifier.width(14.dp))
        Text(
            label.uppercase(),
            style = TextStyle(fontFamily = displayFontFamily(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 1.5.sp),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The strip across the top of the window. Everything on it is real: what is playing (or IDLE),
 * the date and a running clock.
 */
@Composable
fun HudStatusBar(
    title: String?,
    artist: String?,
    modifier: Modifier = Modifier,
) {
    var clock by remember { mutableStateOf("" to "") }
    LaunchedEffect(Unit) {
        while (true) {
            val t = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            fun Int.p() = toString().padStart(2, '0')
            clock = "${t.day.p()}.${t.month.ordinal.plus(1).p()}.${t.year}" to "${t.hour.p()}:${t.minute.p()}:${t.second.p()}"
            delay(1000)
        }
    }
    val dim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    Box(modifier.fillMaxWidth().height(26.dp).background(desktopWindowDark)) {
        Row(Modifier.align(Alignment.CenterStart).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("CYBERSYNC", style = TechStyle, color = neonCyan)
            Text("  /  ", style = TechStyle, color = dim)
            if (title.isNullOrBlank()) {
                Text("IDLE", style = TechStyle, color = dim)
            } else {
                Text("NOW PLAYING  ", style = TechStyle, color = dim)
                Text(
                    listOfNotNull(title, artist?.takeIf { it.isNotBlank() }).joinToString("  —  "),
                    style = TechStyle,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(Modifier.align(Alignment.CenterEnd).padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(clock.first, style = TechStyle, color = dim)
            Spacer(Modifier.width(14.dp))
            Text(clock.second, style = TechStyle, color = neonCyan)
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(neonCyan.copy(alpha = 0.18f)),
        )
    }
}

/** Section heading: short accent bar, then the title in capitals in the display face. */
@Composable
fun CyberSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (!LocalIsDarkTheme.current) {
        Text(text, style = typo().titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = modifier)
        return
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 3.dp, height = 18.dp).background(neonCyan))
        Spacer(Modifier.width(10.dp))
        Text(
            text.uppercase(),
            style = typo().titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private class CircuitTrace(
    val points: List<Offset>,
    val speed: Float, // px per second
    val phase: Float, // px
) {
    val segLengths = points.zipWithNext { a, b -> (b - a).getDistance() }
    val length = segLengths.sum()

    fun pointAt(distance: Float): Offset {
        var d = distance.coerceIn(0f, length)
        for (i in segLengths.indices) {
            val l = segLengths[i]
            if (d <= l) return points[i] + (points[i + 1] - points[i]) * (if (l == 0f) 0f else d / l)
            d -= l
        }
        return points.last()
    }
}

private const val CHIP_HALF_DP = 22f
private val PIN_OFFSETS_DP = floatArrayOf(-16f, -11f, -6f, 6f, 11f, 16f)

/**
 * PCB-style fan-out from the chip's side pins. Upper pins route to lanes in the top band, lower
 * pins to the bottom band; each pin turns one step later than the one outside it, so the 45 degree
 * jogs run parallel and no two traces ever cross. Pulses start at the pin and run outward.
 */
private fun buildChipTraces(
    w: Float,
    h: Float,
    chip: Offset,
    unit: Float,
): List<CircuitTrace> {
    val rnd = kotlin.random.Random(11)
    val traces = mutableListOf<CircuitTrace>()
    val pinX = CHIP_HALF_DP * unit + 4f * unit // pin tip, from chip centre
    for (side in intArrayOf(-1, 1)) {
        val edge = chip.x + side * pinX
        // Upper group: offsets -16,-11,-6 -> lanes 4, 8, 12dp from the top. Lower mirrors it.
        for ((k, off) in PIN_OFFSETS_DP.withIndex()) {
            val upper = off < 0
            val rank = if (upper) k else PIN_OFFSETS_DP.size - 1 - k // 0 = outermost pin
            val pinY = chip.y + off * unit
            val laneY = if (upper) (4f + 4f * rank) * unit else h - (4f + 4f * rank) * unit
            val stub = (6f + 5f * rank) * unit
            val turnX = edge + side * stub
            val diagX = turnX + side * kotlin.math.abs(pinY - laneY)
            // A narrow bar (phone) can leave no room past the jog; the trace then simply ends at the
            // jog instead of the clamp range going empty and throwing.
            val r = rnd.nextFloat()
            val endX =
                if (side < 0) {
                    val lo = 10f * unit
                    val hi = diagX - 12f * unit
                    if (hi < lo) diagX else (lo + r * (diagX - 50f * unit)).coerceIn(lo, hi)
                } else {
                    val lo = diagX + 12f * unit
                    val hi = w - 12f * unit
                    if (hi < lo) diagX else (diagX + 90f * unit + r * (w - diagX - 110f * unit)).coerceIn(lo, hi)
                }
            traces +=
                CircuitTrace(
                    listOf(Offset(edge, pinY), Offset(turnX, pinY), Offset(diagX, laneY), Offset(endX, laneY)),
                    speed = (16f + rnd.nextFloat() * 26f) * unit, // slow, deliberate drift
                    phase = rnd.nextFloat() * 300f * unit,
                )
        }
    }
    return traces
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTracePart(
    tr: CircuitTrace,
    upTo: Float,
    color: Color,
    width: Float,
) {
    var left = upTo
    for (i in tr.segLengths.indices) {
        if (left <= 0f) return
        val l = tr.segLengths[i]
        val end = if (left >= l) tr.points[i + 1] else tr.points[i] + (tr.points[i + 1] - tr.points[i]) * (left / l)
        drawLine(color, tr.points[i], end, width)
        left -= l
    }
}

/** The socketed chip: cut-corner body, side pins, an inner die frame and a per-song die marking. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChip(
    center: Offset,
    unit: Float,
    accent: Color,
    seed: Int,
    glow: Float,
    alpha: Float,
) {
    if (alpha <= 0f) return
    val half = CHIP_HALF_DP * unit
    val cut = 7f * unit
    val l = center.x - half
    val t = center.y - half
    val r = center.x + half
    val b = center.y + half
    for (off in PIN_OFFSETS_DP) {
        val y = center.y + off * unit
        drawLine(accent.copy(alpha = 0.8f * alpha), Offset(l - 4f * unit, y), Offset(l, y), 2f * unit)
        drawLine(accent.copy(alpha = 0.8f * alpha), Offset(r, y), Offset(r + 4f * unit, y), 2f * unit)
    }
    val body =
        Path().apply {
            moveTo(l + cut, t)
            lineTo(r, t)
            lineTo(r, b)
            lineTo(l, b)
            lineTo(l, t + cut)
            close()
        }
    drawPath(body, Color(0xFF0B0619).copy(alpha = alpha))
    drawPath(body, accent.copy(alpha = 0.10f * glow * alpha))
    drawPath(body, accent.copy(alpha = 0.85f * alpha), style = Stroke(width = 1f * unit))
    val inset = 6f * unit
    drawRect(
        accent.copy(alpha = 0.30f * alpha),
        topLeft = Offset(l + inset, t + inset),
        size = androidx.compose.ui.geometry.Size(2 * (half - inset), 2 * (half - inset)),
        style = Stroke(width = 1f * unit),
    )
    // Die marking: four corner cells lit by the song's id, plus the orientation dot.
    val cell = 3f * unit
    val corners =
        listOf(
            Offset(l + inset + 2f * unit, t + inset + 2f * unit),
            Offset(r - inset - 2f * unit - cell, t + inset + 2f * unit),
            Offset(l + inset + 2f * unit, b - inset - 2f * unit - cell),
            Offset(r - inset - 2f * unit - cell, b - inset - 2f * unit - cell),
        )
    corners.forEachIndexed { i, o ->
        val lit = (seed shr i) and 1 == 1
        drawRect(
            accent.copy(alpha = (if (lit) 0.9f else 0.2f) * alpha),
            topLeft = o,
            size = androidx.compose.ui.geometry.Size(cell, cell),
        )
    }
    drawCircle(accent.copy(alpha = 0.7f * alpha), 1.5f * unit, Offset(l + cut + 3f * unit, t + 3f * unit))
}

/**
 * The player bar's living circuit, running out of a chip seated behind the play button
 * ([chipX] from the bar's left edge, vertically centred).
 *
 * - Playing: pulses fire from the chip's pins outward, end pads flash as pulses land, a scan
 *   line sweeps the bar. Paused: everything dims and the frame clock stops.
 * - [trackKey] changes: the old chip ejects upward, the new one drops into the socket with a
 *   seat flash, then the traces boot outward from its pins.
 */
@Composable
fun Modifier.livingCircuit(
    active: Boolean,
    chipX: Dp,
    trackKey: Any?,
    accent: Color = neonCyan,
    /** Measure [chipX] from the right edge instead (the phone bar's play button sits at the end). */
    fromEnd: Boolean = false,
): Modifier {
    if (CyberLook.focus) return this

    val energy by androidx.compose.animation.core.animateFloatAsState(
        if (active) 1f else 0f,
        androidx.compose.animation.core.tween(600),
        label = "circuitEnergy",
    )
    var time by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var last = androidx.compose.runtime.withFrameNanos { it }
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                time += (now - last) / 1_000_000_000f
                last = now
            }
        }
    }
    // Chip swap: 0 -> 1 over 2.2 s. 1 = seated and running.
    val swap = remember { androidx.compose.animation.core.Animatable(1f) }
    var seated by remember { mutableStateOf(trackKey) }
    var ejected by remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(trackKey) {
        if (trackKey == seated) return@LaunchedEffect
        ejected = seated
        seated = trackKey
        swap.snapTo(0f)
        swap.animateTo(1f, androidx.compose.animation.core.tween(2200, easing = androidx.compose.animation.core.LinearEasing))
    }
    return drawWithCache {
        val unit = 1.dp.toPx()
        val chip = Offset(if (fromEnd) size.width - chipX.toPx() else chipX.toPx(), size.height / 2f)
        val traces = buildChipTraces(size.width, size.height, chip, unit)
        val tail = 34f * unit
        val line = 1f * unit
        onDrawBehind {
            val e = energy
            val t = time
            val sv = swap.value
            val p1 = (sv / 0.3f).coerceIn(0f, 1f) // eject
            val p2 = ((sv - 0.3f) / 0.3f).coerceIn(0f, 1f) // insert
            val p3 = ((sv - 0.6f) / 0.4f).coerceIn(0f, 1f) // boot
            val booting = sv < 1f
            val travel = CHIP_HALF_DP * 2 * unit + 12f * unit

            if (e > 0f && !booting) {
                val sx = (t % 10f) / 10f * size.width
                drawRect(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Color.Transparent, accent.copy(alpha = 0.07f * e), Color.Transparent),
                        startX = sx - 60f * unit,
                        endX = sx + 60f * unit,
                    ),
                )
            }

            traces.forEachIndexed { i, tr ->
                if (booting) {
                    // Fade out during eject, gone during insert, wipe outward during boot.
                    if (p1 < 1f) {
                        drawTracePart(tr, tr.length, accent.copy(alpha = (0.10f + 0.10f * e) * (1f - p1)), line)
                    }
                    if (p3 > 0f) {
                        val reach = ((p3 * 1.25f - i * 0.02f).coerceIn(0f, 1f)) * tr.length
                        drawTracePart(tr, reach, accent.copy(alpha = 0.55f), line * 1.4f)
                        if (reach < tr.length) {
                            drawCircle(accent, 2.2f * unit, tr.pointAt(reach))
                            drawCircle(accent.copy(alpha = 0.3f), 5f * unit, tr.pointAt(reach))
                        } else {
                            drawCircle(accent.copy(alpha = 0.9f), 2.5f * unit, tr.points.last(), style = Stroke(line))
                        }
                    }
                    return@forEachIndexed
                }
                drawTracePart(tr, tr.length, accent.copy(alpha = 0.10f + 0.10f * e), line)
                var padHot = 0f
                if (e > 0f) {
                    val cycle = tr.length + tail
                    val head = (t * tr.speed + tr.phase) % cycle
                    var d = 0f
                    while (d < tail) {
                        val at = head - d
                        if (at in 0f..tr.length) {
                            val fade = 1f - d / tail
                            drawCircle(accent.copy(alpha = 0.85f * fade * e), (1.6f * fade + 0.4f) * unit, tr.pointAt(at))
                        }
                        d += 2f * unit
                    }
                    if (head in 0f..tr.length) drawCircle(accent.copy(alpha = 0.25f * e), 5f * unit, tr.pointAt(head))
                    // The end pad flashes as the pulse lands on it.
                    if (head >= tr.length - 4f * unit) padHot = 1f - ((head - tr.length).coerceAtLeast(0f) / tail)
                }
                drawCircle(accent.copy(alpha = 0.25f + 0.35f * e + 0.4f * padHot * e), 2.5f * unit, tr.points.last(), style = Stroke(line))
                if (padHot > 0f) drawCircle(accent.copy(alpha = 0.25f * padHot * e), 6f * unit, tr.points.last())
            }

            val glow = if (booting) p3 else e * (0.6f + 0.4f * kotlin.math.sin(t * 0.8f))
            if (booting && p1 < 1f) {
                val k = 1f - (1f - p1) * (1f - p1)
                drawChip(chip + Offset(0f, -k * travel), unit, accent, ejected.hashCode(), 0f, 1f - p1 * 0.6f)
            }
            if (!booting || p2 > 0f) {
                val k = if (booting) 1f - (1f - p2) * (1f - p2) * (1f - p2) else 1f
                drawChip(chip + Offset(0f, (1f - k) * travel), unit, accent, seated.hashCode(), glow, if (booting) p2 else 1f)
                // Seat flash: a ring snapping outward the moment the chip lands.
                if (booting && p2 >= 1f && p3 < 0.5f) {
                    val f = p3 / 0.5f
                    drawCircle(
                        accent.copy(alpha = 0.6f * (1f - f)),
                        (CHIP_HALF_DP + 4f + 18f * f) * unit,
                        chip,
                        style = Stroke(width = 2f * unit * (1f - f) + 0.5f),
                    )
                }
            }
        }
    }
}

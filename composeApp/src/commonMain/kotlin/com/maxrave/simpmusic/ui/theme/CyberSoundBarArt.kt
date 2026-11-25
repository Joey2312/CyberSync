package com.maxrave.simpmusic.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.simpmusic.expect.ui.audioBandLevels
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// The current song's artwork in CyberSync: a segmented, mirrored sound bar that is a function of
// four live measurements of the music, plus a timeline of the whole song.
//   VOLUME  (loudness of the track)        -> bar height and brightness, reflection strength
//   PITCH   (spectral centroid, in Hz)      -> colour temperature and the tuner line
//   TEMPO   (BPM estimated from the kicks)  -> beat bounce, tempo-locked sway, peak-cap fall
//   VOCALS  (centre vs. sides, voice band)  -> magenta voice heads on the centre bars, VOX lamp

private const val BARS = 28
private const val TIMELINE_SEGMENTS = 48

private val deepCyan = Color(0xFF0090FF)
private val whiteHot = Color(0xFFE8FDFF)

/**
 * Raw dBFS -> 0..1 per band, measured against the song's own running average (not a floor that
 * creeps up to it), so long steady passages keep moving and real hits still reach the top.
 */
private class BandFollower {
    val env = FloatArray(4)
    private val avg = FloatArray(4)
    private val peak = FloatArray(4) { 0.3f }

    fun update(
        db: FloatArray,
        dt: Float,
        live: Boolean,
    ) {
        for (i in 0 until 4) {
            val lin = if (live) ((db[i] + 60f) / 60f).coerceIn(0f, 1f) else 0f
            avg[i] += (lin - avg[i]) * minOf(1f, dt * 0.6f)
            peak[i] = maxOf(lin, peak[i] - dt * 0.06f, avg[i] + 0.08f)
            val floor = avg[i] * 0.6f
            val norm = ((lin - floor) / (peak[i] - floor)).coerceIn(0f, 1f)
            env[i] = if (norm > env[i]) norm else maxOf(norm, env[i] - dt * 2.4f)
        }
    }
}

/** Kick onsets -> beat period: intervals folded into 60..180 BPM, median of the recent ones. */
private class TempoTracker {
    private val onsets = ArrayDeque<Float>()
    var period = 0.5f
        private set
    var confident = false
        private set
    var lastBeat = -10f
        private set

    fun onset(t: Float) {
        if (onsets.isNotEmpty() && t - onsets.last() < 0.28f) return
        onsets.addLast(t)
        while (onsets.size > 16) onsets.removeFirst()
        val intervals =
            onsets.zipWithNext { a, b ->
                var d = b - a
                while (d < 0.333f) d *= 2f
                while (d > 1.0f) d /= 2f
                d
            }
        if (intervals.size >= 4) {
            val m = intervals.sorted()[intervals.size / 2]
            period += (m - period) * 0.5f
            confident = true
        }
        lastBeat = t
    }

    val bpm: Int get() = (60f / period).roundToInt()

    /** Time since the most recent beat, predicted forward from the last kick by whole periods. */
    fun sinceBeat(now: Float): Float {
        val since = now - lastBeat
        return if (confident && since > 0f) since % period else since.coerceAtLeast(0f)
    }

    fun reset() {
        onsets.clear()
        confident = false
    }
}

private class SoundBarSim {
    val bands = BandFollower()
    val tempo = TempoTracker()
    val level = FloatArray(BARS)
    val peak = FloatArray(BARS)
    private val texture = FloatArray(BARS) { 0.75f + 0.25f * Random(it * 31 + 5).nextFloat() }
    private val phase = FloatArray(BARS) { Random(it * 17 + 3).nextFloat() * 6.28f }
    var clock = 0f
    var volume = 0f // 0..1, absolute loudness
    var pitch = 0.5f // 0..1, log scale 150 Hz .. 5 kHz
    var pitchHz = 0f
    var vocals = 0f // 0..1
    var bounce = 0f
    var volumeDb = -120f
    private var prevBass = 0f
    private var idleTime = 0f

    fun step(
        dt: Float,
        m: FloatArray,
        playing: Boolean,
    ) {
        bands.update(m, dt, playing)
        clock += dt
        val b = bands.env

        // VOLUME: absolute, so a quiet song looks quieter than a loud one.
        volumeDb = if (playing) m[4] else -120f
        val volTarget = ((volumeDb + 48f) / 42f).coerceIn(0f, 1f)
        volume += (volTarget - volume) * minOf(1f, dt * (if (volTarget > volume) 12f else 2f))

        // PITCH: the measured centroid; without it, the band balance stands in.
        val hz = if (playing) m[7] else 0f
        val pTarget =
            if (hz > 0f) {
                pitchHz += (hz - pitchHz) * minOf(1f, dt * 4f)
                ((ln(hz) - ln(150f)) / (ln(5000f) - ln(150f))).coerceIn(0f, 1f)
            } else {
                val sum = b.sum()
                if (sum > 0.01f) (b[1] * 0.33f + b[2] * 0.66f + b[3]) / sum else pitch
            }
        pitch += (pTarget - pitch) * minOf(1f, dt * 4f)

        // VOCALS: the voice band in the centre of the image standing above the same band at the sides.
        val mid = m[5]
        val side = m[6]
        val vTarget = if (playing && mid > -45f) ((mid - side - 3f) / 9f).coerceIn(0f, 1f) else 0f
        vocals += (vTarget - vocals) * minOf(1f, dt * (if (vTarget > vocals) 10f else 2.5f))

        // TEMPO: kicks are sharp rises of the bass band.
        if (playing && b[0] - prevBass > 0.18f) tempo.onset(clock)
        prevBass = b[0]
        if (!playing) {
            idleTime += dt
            if (idleTime > 3f) tempo.reset()
        } else {
            idleTime = 0f
        }
        bounce = if (playing) exp(-tempo.sinceBeat(clock) * 7f) else 0f

        // Bars: spectrum shape x volume, swaying in time with the beat.
        val beatHz = 1f / tempo.period
        val centre = (BARS - 1) / 2f
        for (j in 0 until BARS) {
            val x = abs(j - centre) / centre * 3f
            val lo = minOf(2, x.toInt())
            val f = x - lo
            val v = b[lo] * (1 - f) + b[lo + 1] * f
            val sway = 0.85f + 0.15f * sin(2f * PI.toFloat() * beatHz * (1f + (j % 3) * 0.5f) * clock + phase[j])
            val idle = if (playing) 0.05f + 0.03f * sin(clock * 1.7f + phase[j]) else 0f
            val shaped = (v * texture[j] * sway).coerceIn(0f, 1f).let { it * it }
            val target = maxOf(idle, shaped * (0.35f + 0.65f * volume) + bounce * 0.12f * volume).coerceIn(0f, 1f)
            level[j] = if (target > level[j]) target else maxOf(target, level[j] - dt * 2.8f)
            // Caps fall the bar's full height in about two beats.
            peak[j] = maxOf(level[j], peak[j] - dt / (tempo.period * 2f))
        }
    }

    fun idle() = level.all { it < 0.005f } && peak.all { it < 0.005f } && vocals < 0.01f
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "${(s / 60).toString().padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}"
}

/** One angled LED segment: a parallelogram, the same cut as every other CyberSync edge. */
private fun DrawScope.segment(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    slant: Float,
    color: Color,
) {
    drawPath(
        Path().apply {
            moveTo(x + slant, y)
            lineTo(x + w, y)
            lineTo(x + w - slant, y + h)
            lineTo(x, y + h)
            close()
        },
        color,
    )
}

/**
 * The live sound bar. Runs its frame clock while [playing] (and until it settles after a pause),
 * so a paused or closed player costs nothing. [timeline] feeds the song-length strip.
 */
@Composable
fun CyberSoundBarArt(
    playing: Boolean,
    timeline: StateFlow<TimeLine>,
    modifier: Modifier = Modifier,
) {
    val sim = remember { SoundBarSim() }
    val levels = remember { audioBandLevels() }
    val measurer = rememberTextMeasurer()
    val mono = monoFontFamily()
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playing) {
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                last = now
                sim.step(dt, levels.value, playing)
                frame++
            }
            if (!playing && sim.idle()) break
        }
    }

    fun DrawScope.paint(glow: Boolean) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h) / 320f
        if (!glow) {
            val step = 16f * unit
            var g = step
            while (g < w) {
                drawLine(neonCyan.copy(alpha = 0.035f), Offset(g, 0f), Offset(g, h), 1f)
                g += step
            }
            g = step
            while (g < h) {
                drawLine(neonCyan.copy(alpha = 0.035f), Offset(0f, g), Offset(w, g), 1f)
                g += step
            }
        }

        // Pitch sets the colour temperature: deep blue-cyan for low, white-hot for high.
        val p = sim.pitch
        val tone = if (p < 0.5f) lerp(deepCyan, neonCyan, p * 2f) else lerp(neonCyan, whiteHot, (p - 0.5f) * 2f)
        val bright = 0.6f + 0.4f * sim.volume

        // ── Bars ──
        val padX = 18f * unit
        val centreY = h * 0.42f
        val maxHalf = h * 0.31f
        val gap = 3f * unit
        val barW = (w - 2 * padX - gap * (BARS - 1)) / BARS
        val segH = 5f * unit
        val segGap = 2f * unit
        val segCount = (maxHalf / (segH + segGap)).toInt()
        val slant = minOf(barW * 0.35f, 3f * unit)
        val centre = (BARS - 1) / 2f
        if (!glow) drawLine(tone.copy(alpha = 0.22f), Offset(padX, centreY), Offset(w - padX, centreY), 1f)
        for (j in 0 until BARS) {
            val x = padX + j * (barW + gap)
            val lit = (sim.level[j] * segCount).toInt().coerceAtLeast(if (playing) 1 else 0)
            // Vocals: the centre bars' heads turn magenta while someone sings.
            val voiceWeight = (1f - abs(j - centre) / 5f).coerceIn(0f, 1f) * sim.vocals
            for (s in 0 until segCount) {
                val on = s < lit
                if (glow && !on) continue
                val isHead = on && s >= lit - 2
                val alpha =
                    when {
                        !on -> 0.07f
                        isHead -> 1f
                        else -> (0.5f + 0.3f * sim.bounce) * bright
                    }
                val base = if (isHead && voiceWeight > 0f) lerp(tone, neonMagenta, voiceWeight) else tone
                val c = base.copy(alpha = alpha)
                val offY = s * (segH + segGap) + segGap
                segment(x, centreY - offY - segH, barW, segH, slant, c)
                segment(x, centreY + offY, barW, segH, slant, c.copy(alpha = alpha * (0.3f + 0.3f * sim.volume)))
            }
            val capS = (sim.peak[j] * segCount).toInt()
            if (capS > lit) {
                val offY = capS * (segH + segGap) + segGap
                segment(x, centreY - offY - segH * 0.5f, barW, segH * 0.5f, slant * 0.5f, tone.copy(alpha = 0.9f))
            }
        }
        // Tuner line: rides up with the pitch across the whole bar field.
        if (playing) {
            val ty = centreY - 4f * unit - p * (maxHalf - 4f * unit)
            drawLine(whiteHot.copy(alpha = 0.35f), Offset(padX - 8f * unit, ty), Offset(w - padX + 8f * unit, ty), 1f)
            segment(padX - 14f * unit, ty - 2.5f * unit, 8f * unit, 5f * unit, 2f * unit, whiteHot.copy(alpha = 0.9f))
        }

        // ── Readouts (all measured) ──
        val readY = h * 0.80f
        if (!glow) {
            val style = TextStyle(fontFamily = mono, fontSize = (10f * unit / density).coerceIn(9f, 13f).sp, letterSpacing = 1.sp)
            val dim = neonCyan.copy(alpha = 0.55f)
            val vol = if (sim.volumeDb <= -90f) "-inf" else "${sim.volumeDb.roundToInt()}dB"
            val pitchText =
                when {
                    !playing || sim.pitchHz <= 0f -> "---"
                    sim.pitchHz >= 1000f -> "${(sim.pitchHz / 100f).roundToInt() / 10f}kHz"
                    else -> "${sim.pitchHz.roundToInt()}Hz"
                }
            val bpm = if (playing && sim.tempo.confident) "${sim.tempo.bpm}" else "---"
            val items = listOf("VOL $vol", "PITCH $pitchText", "$bpm BPM")
            var x = padX
            items.forEach { text ->
                val layout = measurer.measure(text, style.copy(color = dim))
                drawText(layout, topLeft = Offset(x, readY))
                x += layout.size.width + 14f * unit
            }
            val vox = measurer.measure("VOX", style.copy(color = if (sim.vocals > 0.35f) neonMagenta else neonCyan.copy(alpha = 0.2f)))
            drawText(vox, topLeft = Offset(w - padX - vox.size.width, readY))
        }

        // ── Timeline: the whole song as a segmented strip ──
        val t = timeline.value
        val progress = if (t.total > 0 && !t.isLive) (t.current.toFloat() / t.total).coerceIn(0f, 1f) else 0f
        val stripY = h * 0.87f
        val stripH = 6f * unit
        val sGap = 2f * unit
        val sW = (w - 2 * padX - sGap * (TIMELINE_SEGMENTS - 1)) / TIMELINE_SEGMENTS
        val headIdx = (progress * TIMELINE_SEGMENTS).toInt().coerceAtMost(TIMELINE_SEGMENTS - 1)
        // The playhead pulses on the beat.
        val pulse = if (playing) 0.45f + 0.55f * sim.bounce else 0.9f
        for (i in 0 until TIMELINE_SEGMENTS) {
            val alpha =
                when {
                    i < headIdx -> 0.75f
                    i == headIdx -> pulse
                    else -> 0.10f
                }
            if (glow && i > headIdx) continue
            segment(padX + i * (sW + sGap), stripY, sW, stripH, minOf(sW * 0.4f, 2f * unit), tone.copy(alpha = alpha))
        }
        if (!glow) {
            val style = TextStyle(fontFamily = mono, fontSize = (11f * unit / density).coerceIn(9f, 14f).sp, color = neonCyan.copy(alpha = 0.8f))
            val left = if (t.isLive) "LIVE" else formatTime(t.current)
            val right = if (t.isLive || t.total <= 0) "--:--" else formatTime(t.total)
            drawText(measurer, left, Offset(padX, stripY + stripH + 6f * unit), style)
            val rightLayout = measurer.measure(right, style)
            drawText(rightLayout, topLeft = Offset(w - padX - rightLayout.size.width, stripY + stripH + 6f * unit))
        }
    }

    Box(modifier.background(neonVoid)) {
        Canvas(Modifier.matchParentSize().blur(8.dp, BlurredEdgeTreatment.Unbounded)) {
            frame
            paint(glow = true)
        }
        Canvas(Modifier.matchParentSize()) {
            frame
            paint(glow = false)
        }
    }
}

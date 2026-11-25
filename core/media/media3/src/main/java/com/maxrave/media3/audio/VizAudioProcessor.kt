package com.maxrave.media3.audio

import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Live levels for the CyberSync sound bar, same layout as the desktop mpv tap (MpvPlayer VIZ_*):
 * 0 bass, 1 low-mid, 2 high-mid, 3 treble, 4 volume, 5 vocal mid, 6 vocal side (dBFS, -120 = silent),
 * 7 spectral centroid Hz (0 = none).
 */
object VizBands {
    const val SIZE = 8
    val SILENT = FloatArray(SIZE) { if (it == 7) 0f else -120f }
    internal val mutable = MutableStateFlow(SILENT)
    val levels: StateFlow<FloatArray> = mutable
}

/**
 * Pass-through tap at the end of the sink chain: copies audio unchanged and measures the same
 * bands as the desktop lavfi graph (lowpass 160 / bandpass 500 / bandpass 2000 / highpass 5000 on
 * the mid signal, a wide 1 kHz band on mid and side for vocals, RMS per ~33 ms window, centroid
 * from a 1024-point FFT).
 */
@UnstableApi
class VizAudioProcessor : BaseAudioProcessor() {
    private class Biquad {
        private var b0 = 1.0
        private var b1 = 0.0
        private var b2 = 0.0
        private var a1 = 0.0
        private var a2 = 0.0
        private var x1 = 0.0
        private var x2 = 0.0
        private var y1 = 0.0
        private var y2 = 0.0

        /** RBJ cookbook. kind: 0 lowpass, 1 bandpass (0 dB peak), 2 highpass. */
        fun set(kind: Int, f: Double, q: Double, rate: Int) {
            val w = 2.0 * PI * f.coerceAtMost(rate * 0.45) / rate
            val alpha = sin(w) / (2.0 * q)
            val c = cos(w)
            val a0 = 1.0 + alpha
            when (kind) {
                0 -> { b0 = (1 - c) / 2; b1 = 1 - c; b2 = (1 - c) / 2 }
                1 -> { b0 = alpha; b1 = 0.0; b2 = -alpha }
                else -> { b0 = (1 + c) / 2; b1 = -(1 + c); b2 = (1 + c) / 2 }
            }
            b0 /= a0; b1 /= a0; b2 /= a0
            a1 = -2 * c / a0
            a2 = (1 - alpha) / a0
            x1 = 0.0; x2 = 0.0; y1 = 0.0; y2 = 0.0
        }

        fun run(x: Double): Double {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x; y2 = y1; y1 = y
            return y
        }
    }

    // bass, low-mid, high-mid, treble, vocal-mid, vocal-side
    private val filters = Array(6) { Biquad() }
    private val sums = DoubleArray(SUMS) // 6 bands + L + R
    private var frames = 0
    private var window = 1470
    private val fft = Fft(FFT_N)
    private val ring = DoubleArray(FFT_N)
    private var ringPos = 0
    private val re = DoubleArray(FFT_N)
    private val im = DoubleArray(FFT_N)
    private val main = Handler(Looper.getMainLooper())

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val ok = inputAudioFormat.channelCount in 1..2 &&
            (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT || inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT)
        if (!ok) return AudioProcessor.AudioFormat.NOT_SET
        val rate = inputAudioFormat.sampleRate
        filters[0].set(0, 160.0, 0.707, rate)
        filters[1].set(1, 500.0, 0.707, rate)
        filters[2].set(1, 2000.0, 0.707, rate)
        filters[3].set(2, 5000.0, 0.707, rate)
        filters[4].set(1, 1000.0, 0.33, rate) // ~3.5 octaves wide
        filters[5].set(1, 1000.0, 0.33, rate)
        window = rate / 30
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val scan = inputBuffer.duplicate().order(ByteOrder.nativeOrder())
        val stereo = inputAudioFormat.channelCount == 2
        val float = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val frameBytes = (if (float) 4 else 2) * inputAudioFormat.channelCount
        while (scan.remaining() >= frameBytes) {
            val l = if (float) scan.float.toDouble() else scan.short / 32768.0
            val r = if (!stereo) l else if (float) scan.float.toDouble() else scan.short / 32768.0
            analyze(l, r)
        }
        replaceOutputBuffer(inputBuffer.remaining()).put(inputBuffer).flip()
    }

    private fun analyze(l: Double, r: Double) {
        val mid = (l + r) * 0.5
        val side = (l - r) * 0.5
        for (i in 0 until 5) {
            val y = filters[i].run(mid)
            sums[i] += y * y
        }
        val s = filters[5].run(side)
        sums[5] += s * s
        sums[6] += l * l
        sums[7] += r * r
        ring[ringPos] = mid
        ringPos = (ringPos + 1) and (FFT_N - 1)
        if (++frames >= window) publish()
    }

    private fun publish() {
        fun db(sum: Double) = if (sum <= 1e-12 * frames) -120f else (10 * log10(sum / frames)).toFloat()
        val out = FloatArray(VizBands.SIZE)
        for (i in 0 until 4) out[i] = db(sums[i])
        out[4] = maxOf(db(sums[6]), db(sums[7]))
        out[5] = db(sums[4])
        out[6] = db(sums[5])
        out[7] = if (out[4] > -60f) centroid() else 0f
        sums.fill(0.0)
        frames = 0
        // ponytail: fixed delay for the AudioTrack buffer (DefaultAudioSink holds ~250 ms of PCM
        // after this stage). Tune LATENCY_MS if the bars lead or trail the sound on a device.
        main.postDelayed({ VizBands.mutable.value = out }, LATENCY_MS)
    }

    private fun centroid(): Float {
        for (i in 0 until FFT_N) {
            val hann = 0.5 - 0.5 * cos(2 * PI * i / (FFT_N - 1))
            re[i] = ring[(ringPos + i) and (FFT_N - 1)] * hann
            im[i] = 0.0
        }
        fft.forward(re, im)
        var num = 0.0
        var den = 0.0
        val binHz = inputAudioFormat.sampleRate.toDouble() / FFT_N
        for (k in 1 until FFT_N / 2) {
            val mag = sqrt(re[k] * re[k] + im[k] * im[k])
            num += k * binHz * mag
            den += mag
        }
        return if (den > 0) (num / den).toFloat() else 0f
    }

    override fun onReset() {
        sums.fill(0.0)
        frames = 0
        main.post { VizBands.mutable.value = VizBands.SILENT }
    }

    private companion object {
        const val SUMS = 8
        const val FFT_N = 1024
        const val LATENCY_MS = 250L
    }
}

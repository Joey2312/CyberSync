package com.simpmusic.media_jvm.mpv

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Live measurements of what is playing, for the CyberSync visualizer, laid out by
 * `MpvPlayer.VIZ_*`: bands, volume and vocal-band levels in dBFS (-120 = silence), pitch in Hz
 * (0 = not measured). Written by [MpvPlayerAdapter], read by the UI.
 */
object MpvAudioBands {
    private val silent = FloatArray(MpvPlayer.VIZ_SIZE) { if (it == MpvPlayer.VIZ_PITCH_HZ) 0f else -120f }
    private val _levels = MutableStateFlow(silent)
    val levels: StateFlow<FloatArray> = _levels.asStateFlow()

    internal fun publish(bands: FloatArray) {
        _levels.value = bands
    }

    internal fun silence() {
        if (_levels.value !== silent) _levels.value = silent
    }
}

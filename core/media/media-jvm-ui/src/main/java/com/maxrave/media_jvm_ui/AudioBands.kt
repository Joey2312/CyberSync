package com.maxrave.media_jvm_ui

import com.simpmusic.media_jvm.mpv.MpvAudioBands
import kotlinx.coroutines.flow.StateFlow

/** Live measurements for the CyberSync visualizer; layout in `expect/ui/AudioBands.kt`. */
val audioBandLevels: StateFlow<FloatArray> get() = MpvAudioBands.levels

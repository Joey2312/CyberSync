package com.maxrave.simpmusic.expect.ui

import com.maxrave.media3.audio.VizBands
import kotlinx.coroutines.flow.StateFlow

actual fun audioBandLevels(): StateFlow<FloatArray> = VizBands.levels

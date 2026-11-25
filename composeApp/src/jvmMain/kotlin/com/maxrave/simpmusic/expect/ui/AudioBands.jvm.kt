package com.maxrave.simpmusic.expect.ui

import kotlinx.coroutines.flow.StateFlow

actual fun audioBandLevels(): StateFlow<FloatArray> = com.maxrave.media_jvm_ui.audioBandLevels

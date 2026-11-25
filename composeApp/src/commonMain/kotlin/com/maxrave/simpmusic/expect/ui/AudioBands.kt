package com.maxrave.simpmusic.expect.ui

import kotlinx.coroutines.flow.StateFlow

/**
 * Live measurements of what is playing. Indices: 0 bass, 1 low-mid, 2 high-mid, 3 treble,
 * 4 volume, 5 vocal band (centre), 6 vocal band (sides): dBFS, -120 = silence. 7 pitch: spectral
 * centroid in Hz, 0 = not measured.
 */
expect fun audioBandLevels(): StateFlow<FloatArray>

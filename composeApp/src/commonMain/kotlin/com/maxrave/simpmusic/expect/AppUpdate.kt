package com.maxrave.simpmusic.expect

/**
 * Downloads the release APK at [url] and hands it to the system installer, reporting 0..1 through
 * [onProgress]. Returns false, having opened the system screen for it, when the app may not install
 * packages yet. Desktop has no APK to install and just opens the link.
 */
expect suspend fun installUpdate(
    url: String,
    onProgress: (Float) -> Unit,
): Boolean

package com.maxrave.simpmusic.expect

actual suspend fun installUpdate(
    url: String,
    onProgress: (Float) -> Unit,
): Boolean {
    openUrl(url)
    return true
}

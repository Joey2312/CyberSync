package com.maxrave.simpmusic

import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult

/**
 * CyberSync shows no artwork. Every picture but the app's own resources fails here without touching
 * the network, so whatever asks for one (the media notification, a widget, Android Auto) goes without.
 */
class NoArtworkInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult =
        if (chain.request.data is Int) {
            chain.proceed()
        } else {
            ErrorResult(image = null, request = chain.request, throwable = IllegalStateException("CyberSync shows no artwork"))
        }
}

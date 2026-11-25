package com.maxrave.simpmusic.ui.navigation.destination.home

import kotlinx.serialization.Serializable

/** Choosing the artists and songs Home is built from: asked on first launch, edited from Settings. */
@Serializable
data class HomePicksDestination(
    val firstRun: Boolean,
)

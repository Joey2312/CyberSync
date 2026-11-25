package com.maxrave.simpmusic.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** Set by AppTheme: true while the app runs its dark (cyber) theme. Read at draw time by [cyber]. */
object CyberLook {
    @kotlin.concurrent.Volatile
    var on: Boolean = true
}

/**
 * A rounded shape that turns into the cyber chamfer while the dark theme is on: the top-start and
 * bottom-end corners are cut by the size they were rounded by (capped at 30%, so circles become
 * slanted hexagons rather than slivers); the other two go square. Light theme draws [base] as is.
 */
private data class CyberizedShape(
    private val base: CornerBasedShape,
) : Shape {
    private val cut: Shape =
        run {
            // The first rounded corner leads; a shape rounded only along its top (sheets) keeps a square bottom.
            val lead = listOf(base.topStart, base.topEnd, base.bottomStart, base.bottomEnd).firstOrNull { it != ZERO } ?: ZERO
            val tail = listOf(base.bottomEnd, base.bottomStart).firstOrNull { it != ZERO } ?: ZERO
            CutCornerShape(topStart = Capped(lead), topEnd = ZERO, bottomEnd = Capped(tail), bottomStart = ZERO)
        }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = (if (CyberLook.on) cut else base).createOutline(size, layoutDirection, density)

    private class Capped(
        private val inner: CornerSize,
    ) : CornerSize {
        override fun toPx(
            shapeSize: Size,
            density: Density,
        ): Float = inner.toPx(shapeSize, density).coerceAtMost(shapeSize.minDimension * 0.3f)
    }

    private companion object {
        val ZERO = CornerSize(0)
    }
}

/** See [CyberizedShape]. */
fun CornerBasedShape.cyber(): Shape = CyberizedShape(this)

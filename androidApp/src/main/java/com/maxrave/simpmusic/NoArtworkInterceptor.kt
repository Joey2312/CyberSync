package com.maxrave.simpmusic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import coil3.asImage
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import kotlin.random.Random
import coil3.Image as CoilImage

/**
 * Android twin of the desktop NoArtworkInterceptor (composeApp jvmMain): every web image is
 * answered, without touching the network, with a generated circuit "data plate" seeded by the
 * URL, so one album always gets the same plate on both platforms. Local files load as normal.
 */
class NoArtworkInterceptor : Interceptor {
    // ponytail: plain LRU of ~200 plates (192px, ~150 KB each, ~30 MB worst case).
    private val cache =
        object : LinkedHashMap<String, CoilImage>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CoilImage>) = size > 200
        }

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val url = chain.request.data.toString()
        if (!url.startsWith("http://") && !url.startsWith("https://")) return chain.proceed()
        val image = synchronized(cache) { cache.getOrPut(url) { CyberPlate.draw(url).asImage() } }
        return SuccessResult(image = image, request = chain.request, dataSource = DataSource.MEMORY)
    }
}

private object CyberPlate {
    private const val S = 192
    private const val GRID = 16f
    private const val VOID = 0xFF05010D.toInt()
    private const val PANEL = 0xFF140B2A.toInt()
    private const val CYAN = 0xFF00F0FF.toInt()
    private const val MAGENTA = 0xFFFF2A6D.toInt()
    private const val YELLOW = 0xFFFCEE0A.toInt()

    private fun alpha(
        color: Int,
        a: Float,
    ) = ((a * 255).toInt() shl 24) or (color and 0x00FFFFFF)

    private fun paint(
        color: Int,
        stroke: Float? = null,
    ) = Paint().apply {
        isAntiAlias = true
        this.color = color
        if (stroke != null) {
            style = Paint.Style.STROKE
            strokeWidth = stroke
        }
    }

    fun draw(seed: String): Bitmap {
        val rnd = Random(seed.hashCode())
        val accent =
            when (rnd.nextInt(10)) {
                in 0..6 -> CYAN
                in 7..8 -> MAGENTA
                else -> YELLOW
            }
        val bmp = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val s = S.toFloat()

        c.drawColor(VOID)
        c.save()
        c.translate(s, 0f)
        c.rotate(45f)
        c.drawRect(0f, 0f, s * 2, s * 2, paint(PANEL))
        c.restore()

        val grid = paint(alpha(accent, 0.06f), 1f)
        var g = GRID
        while (g < s) {
            c.drawLine(g, 0f, g, s, grid)
            c.drawLine(0f, g, s, g, grid)
            g += GRID
        }

        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1)
        repeat(6 + rnd.nextInt(5)) {
            var x = GRID * rnd.nextInt(1, 12)
            var y = GRID * rnd.nextInt(1, 12)
            val col = alpha(accent, 0.25f + rnd.nextFloat() * 0.35f)
            val trace = paint(col, 1.5f)
            val pad = paint(col)
            c.drawCircle(x, y, 2.5f, pad)
            repeat(2 + rnd.nextInt(4)) {
                val (dx, dy) = dirs[rnd.nextInt(dirs.size)]
                val len = GRID * rnd.nextInt(1, 4)
                val nx = (x + dx * len).coerceIn(GRID, s - GRID)
                val ny = (y + dy * len).coerceIn(GRID, s - GRID)
                c.drawLine(x, y, nx, ny, trace)
                x = nx
                y = ny
            }
            if (rnd.nextBoolean()) c.drawRect(x - 3f, y - 3f, x + 3f, y + 3f, trace) else c.drawCircle(x, y, 3f, pad)
        }

        val cx = s / 2 + GRID * rnd.nextInt(-1, 2)
        val cy = s / 2 + GRID * rnd.nextInt(-1, 2)
        val line = paint(accent, 2f)
        val fill = paint(PANEL)
        when (rnd.nextInt(3)) {
            0 -> {
                val half = GRID * 2
                c.drawRect(cx - half, cy - half, cx + half, cy + half, fill)
                c.drawRect(cx - half, cy - half, cx + half, cy + half, line)
                val pin = paint(alpha(accent, 0.7f), 2f)
                var p = cx - half + 6f
                while (p < cx + half - 4f) {
                    c.drawLine(p, cy - half - 6f, p, cy - half, pin)
                    c.drawLine(p, cy + half, p, cy + half + 6f, pin)
                    c.drawLine(cx - half - 6f, p - cx + cy, cx - half, p - cx + cy, pin)
                    c.drawLine(cx + half, p - cx + cy, cx + half + 6f, p - cx + cy, pin)
                    p += 8f
                }
                c.drawRect(cx - 8f, cy - 8f, cx + 8f, cy + 8f, paint(alpha(accent, 0.8f)))
            }
            1 -> {
                c.drawCircle(cx, cy, 34f, fill)
                c.drawCircle(cx, cy, 34f, line)
                c.drawCircle(cx, cy, 22f, paint(alpha(accent, 0.6f), 1f))
                c.drawCircle(cx, cy, 6f, paint(accent))
                c.drawLine(cx - 44f, cy, cx - 28f, cy, line)
                c.drawLine(cx + 28f, cy, cx + 44f, cy, line)
            }
            else -> {
                val l = cx - 30f
                val t = cy - 30f
                val r = cx + 30f
                val b = cy + 30f
                c.drawRect(l, t, r, b, fill)
                c.drawLine(l + 12f, t, r, t, line)
                c.drawLine(r, t, r, b, line)
                c.drawLine(r, b, l, b, line)
                c.drawLine(l, b, l, t + 12f, line)
                c.drawLine(l, t + 12f, l + 12f, t, line)
                c.drawLine(l + 14f, b - 14f, r - 14f, t + 14f, line)
            }
        }

        c.drawLine(10f, 4f, 40f, 4f, paint(accent, 2f))
        val id = "0x" + (seed.hashCode().toUInt().toString(16).uppercase().padStart(8, '0')).take(6)
        c.drawText(id, 10f, s - 10f, paint(alpha(accent, 0.75f)).apply { textSize = 12f })
        return bmp
    }
}

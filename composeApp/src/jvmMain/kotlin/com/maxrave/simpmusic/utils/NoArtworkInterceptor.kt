package com.maxrave.simpmusic.utils

import coil3.asImage
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Rect
import kotlin.random.Random
import coil3.Image as CoilImage

/**
 * CyberSync shows no real artwork. Every web image (song, album, playlist, artist covers,
 * backdrops) is answered, without touching the network, with a generated "data plate": circuit
 * traces, a central motif and a hex id, seeded by the URL so one album always gets the same plate
 * and two albums get different ones. Bundled resources and local files load as normal.
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

internal object CyberPlate {
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

    fun draw(seed: String): Bitmap {
        val rnd = Random(seed.hashCode())
        // Cyan most of the time; magenta and yellow are rarer so they stay special.
        val accent =
            when (rnd.nextInt(10)) {
                in 0..6 -> CYAN
                in 7..8 -> MAGENTA
                else -> YELLOW
            }
        val bmp = Bitmap().apply { allocN32Pixels(S, S) }
        val c = Canvas(bmp)
        val s = S.toFloat()

        // Base: void, with the lower-right half a shade lighter (a flat diagonal split).
        c.clear(VOID)
        // Rotating 45 degrees about the top-right corner puts a rect's area below the anti-diagonal.
        c.save()
        c.translate(s, 0f)
        c.rotate(45f)
        c.drawRect(Rect.makeWH(s * 2, s * 2), Paint().apply { color = PANEL })
        c.restore()

        // Faint grid.
        val grid = Paint().apply { color = alpha(accent, 0.06f); strokeWidth = 1f }
        var g = GRID
        while (g < s) {
            c.drawLine(g, 0f, g, s, grid)
            c.drawLine(0f, g, s, g, grid)
            g += GRID
        }

        // Circuit traces: orthogonal/diagonal walks on the grid, each ending in a pad.
        val trace =
            Paint().apply {
                isAntiAlias = true
                mode = PaintMode.STROKE
                strokeWidth = 1.5f
            }
        val pad = Paint().apply { isAntiAlias = true }
        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1)
        repeat(6 + rnd.nextInt(5)) {
            var x = GRID * rnd.nextInt(1, 12)
            var y = GRID * rnd.nextInt(1, 12)
            trace.color = alpha(accent, 0.25f + rnd.nextFloat() * 0.35f)
            pad.color = trace.color
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
            if (rnd.nextBoolean()) {
                c.drawRect(Rect.makeXYWH(x - 3f, y - 3f, 6f, 6f), trace)
            } else {
                c.drawCircle(x, y, 3f, pad)
            }
        }

        // Central motif.
        val cx = s / 2 + GRID * rnd.nextInt(-1, 2)
        val cy = s / 2 + GRID * rnd.nextInt(-1, 2)
        val line =
            Paint().apply {
                isAntiAlias = true
                mode = PaintMode.STROKE
                strokeWidth = 2f
                color = accent
            }
        val fill = Paint().apply { color = PANEL }
        when (rnd.nextInt(3)) {
            0 -> { // chip with pins
                val half = GRID * 2
                c.drawRect(Rect.makeLTRB(cx - half, cy - half, cx + half, cy + half), fill)
                c.drawRect(Rect.makeLTRB(cx - half, cy - half, cx + half, cy + half), line)
                val pin = Paint().apply { color = alpha(accent, 0.7f); strokeWidth = 2f }
                var p = cx - half + 6f
                while (p < cx + half - 4f) {
                    c.drawLine(p, cy - half - 6f, p, cy - half, pin)
                    c.drawLine(p, cy + half, p, cy + half + 6f, pin)
                    c.drawLine(cx - half - 6f, p - cx + cy, cx - half, p - cx + cy, pin)
                    c.drawLine(cx + half, p - cx + cy, cx + half + 6f, p - cx + cy, pin)
                    p += 8f
                }
                c.drawRect(Rect.makeXYWH(cx - 8f, cy - 8f, 16f, 16f), Paint().apply { color = alpha(accent, 0.8f) })
            }
            1 -> { // target rings
                c.drawCircle(cx, cy, 34f, fill)
                c.drawCircle(cx, cy, 34f, line)
                c.drawCircle(cx, cy, 22f, Paint().apply { isAntiAlias = true; mode = PaintMode.STROKE; strokeWidth = 1f; color = alpha(accent, 0.6f) })
                c.drawCircle(cx, cy, 6f, Paint().apply { isAntiAlias = true; color = accent })
                c.drawLine(cx - 44f, cy, cx - 28f, cy, line)
                c.drawLine(cx + 28f, cy, cx + 44f, cy, line)
            }
            else -> { // angular glyph: a cut-corner frame with a slash
                val r = Rect.makeLTRB(cx - 30f, cy - 30f, cx + 30f, cy + 30f)
                c.drawRect(r, fill)
                c.drawLine(r.left + 12f, r.top, r.right, r.top, line)
                c.drawLine(r.right, r.top, r.right, r.bottom, line)
                c.drawLine(r.right, r.bottom, r.left, r.bottom, line)
                c.drawLine(r.left, r.bottom, r.left, r.top + 12f, line)
                c.drawLine(r.left, r.top + 12f, r.left + 12f, r.top, line)
                c.drawLine(r.left + 14f, r.bottom - 14f, r.right - 14f, r.top + 14f, line)
            }
        }

        // Cut-corner tick and hex id, the same marks the UI panels carry.
        c.drawLine(10f, 4f, 40f, 4f, Paint().apply { color = accent; strokeWidth = 2f })
        val id = "0x" + (seed.hashCode().toUInt().toString(16).uppercase().padStart(8, '0')).take(6)
        val font = Font().apply { size = 12f }
        c.drawString(id, 10f, s - 10f, font, Paint().apply { isAntiAlias = true; color = alpha(accent, 0.75f) })
        bmp.setImmutable()
        return bmp
    }
}

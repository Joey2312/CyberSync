package com.maxrave.simpmusic.ui.component

import androidx.compose.ui.awt.ComposeWindow
import com.sun.jna.Function
import com.sun.jna.Memory
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer

/**
 * Paints the native Windows title bar in the CyberSync palette: dark mode, void-black caption,
 * light caption text and a dim cyan border. Caption/text/border colours need Windows 11; on Windows 10
 * those calls simply fail and only the dark-mode flag takes. Never throws.
 */
fun applyCyberTitleBar(window: ComposeWindow) {
    if (!System.getProperty("os.name").orEmpty().startsWith("Windows")) return
    runCatching {
        val hwnd = Pointer(window.windowHandle)
        val set: Function = NativeLibrary.getInstance("dwmapi").getFunction("DwmSetWindowAttribute")
        fun attr(id: Int, value: Int) {
            val mem = Memory(4).apply { setInt(0, value) }
            set.invokeInt(arrayOf(hwnd, id, mem, 4))
        }
        // COLORREF is 0x00BBGGRR.
        attr(20, 1) // DWMWA_USE_IMMERSIVE_DARK_MODE
        attr(34, 0x00403A1A) // DWMWA_BORDER_COLOR: #1A3A40 (dim cyan)
        attr(35, 0x000D0105) // DWMWA_CAPTION_COLOR: #05010D
        attr(36, 0x00E5D6C8) // DWMWA_TEXT_COLOR: #C8D6E5
    }
}

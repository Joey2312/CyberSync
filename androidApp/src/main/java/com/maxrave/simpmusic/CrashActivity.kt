package com.maxrave.simpmusic

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import com.maxrave.common.SETTINGS_FILENAME

/**
 * Shown by CustomActivityOnCrash after a crash, in its own process. Plain Views, not Compose, so a
 * crash in the app's UI cannot take this screen down too. Besides restarting it can reset the
 * settings, for a crash that a setting brings back on every launch.
 */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = CustomActivityOnCrash.getConfigFromIntent(intent)
        val details = CustomActivityOnCrash.getAllErrorDetailsFromIntent(this, intent)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val cyan = Color.rgb(0, 240, 255)

        fun button(label: String, onClick: () -> Unit) =
            Button(this).apply {
                text = label
                setOnClickListener { onClick() }
            }

        val column =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, pad * 3, pad, pad)
                setBackgroundColor(Color.BLACK)
            }
        column.addView(
            TextView(this).apply {
                text = "CyberSync stopped"
                textSize = 22f
                setTextColor(cyan)
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
            },
        )
        column.addView(
            TextView(this).apply {
                text = "If it keeps crashing when it opens, reset the settings. Your library, downloads and playlists stay."
                setTextColor(Color.LTGRAY)
                setPadding(0, pad, 0, pad)
            },
        )
        column.addView(button("Restart") { CustomActivityOnCrash.restartApplication(this, config ?: return@button) })
        column.addView(
            button("Reset settings and restart") {
                AlertDialog
                    .Builder(this)
                    .setTitle("Reset settings?")
                    .setMessage(
                        "Every setting goes back to its default, and you will need to sign in to YouTube, Spotify and other accounts again. Your library, downloads and playlists are kept.",
                    ).setNegativeButton("Cancel", null)
                    .setPositiveButton("Reset") { _, _ ->
                        val ok = resetSettings()
                        Toast.makeText(this, if (ok) "Settings reset" else "Could not reset settings", Toast.LENGTH_SHORT).show()
                        CustomActivityOnCrash.restartApplication(this, config ?: return@setPositiveButton)
                    }.show()
            },
        )
        column.addView(
            button("Copy error details") {
                getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("CyberSync crash", details))
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            },
        )
        column.addView(button("Close") { CustomActivityOnCrash.closeApplication(this, config ?: return@button) })
        column.addView(
            TextView(this).apply {
                text = details
                textSize = 11f
                typeface = Typeface.MONOSPACE
                setTextColor(Color.GRAY)
                setPadding(0, pad, 0, 0)
                setTextIsSelectable(true)
            },
        )
        setContentView(ScrollView(this).apply { addView(column) })
    }

    /** Deletes the settings store; the app recreates it with defaults on the next start. */
    private fun resetSettings(): Boolean {
        val dir = filesDir.resolve("datastore")
        val files = listOf("$SETTINGS_FILENAME.preferences_pb", "$SETTINGS_FILENAME.preferences_pb.tmp").map { dir.resolve(it) }
        files.forEach { it.delete() }
        return files.none { it.exists() }
    }
}

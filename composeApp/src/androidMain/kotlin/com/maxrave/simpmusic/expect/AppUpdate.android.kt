package com.maxrave.simpmusic.expect

import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform.getKoin
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

actual suspend fun installUpdate(
    url: String,
    onProgress: (Float) -> Unit,
): Boolean {
    val activity: AppCompatActivity = getKoin().get()
    // Asked first, so the user is not made to wait through a 50 MB download only to be stopped here.
    if (!activity.packageManager.canRequestPackageInstalls()) {
        activity.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${activity.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return false
    }
    val apk =
        withContext(Dispatchers.IO) {
            val dir = File(activity.cacheDir, "updates").apply { mkdirs() }
            val part = File(dir, "CyberSync.apk.part")
            val apk = File(dir, "CyberSync.apk")
            // github.com redirects to its asset CDN; both are https, which HttpURLConnection follows.
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            try {
                check(conn.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${conn.responseCode}" }
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    part.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (total > 0) onProgress(done.toFloat() / total)
                        }
                        check(total <= 0 || done == total) { "Download cut short" }
                    }
                }
            } finally {
                conn.disconnect()
            }
            apk.delete()
            check(part.renameTo(apk)) { "Could not save the update" }
            apk
        }
    // The app context, not the activity: the activity may have been recreated during the download.
    val app = activity.applicationContext
    val uri = FileProvider.getUriForFile(app, "${app.packageName}.FileProvider", apk)
    app.startActivity(
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
    )
    return true
}

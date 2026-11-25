package com.maxrave.simpmusic.service.test.notification

import android.widget.RemoteViews
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.maxrave.simpmusic.MainActivity
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.utils.ComposeResUtils

object NotificationHandler {
    private const val CHANNEL_ID = "transactions_reminder_channel"

    suspend fun createReminderNotification(
        context: Context,
        noti: NotificationModel,
    ) {
        //  No back-stack when launched
        val action = Intent(context, MainActivity::class.java)
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        action.data = "simpmusic://notification".toUri()
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                action,
                PendingIntent.FLAG_IMMUTABLE,
            )

        val body =
            if (noti.single.isNotEmpty()) {
                "${ComposeResUtils.getResString(ComposeResUtils.StringType.NEW_SINGLES)}: ${noti.single.joinToString { it.title }}"
            } else {
                "${ComposeResUtils.getResString(ComposeResUtils.StringType.NEW_ALBUMS)}: ${noti.album.joinToString { it.title }}"
            }
        val builder =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.mono)
                .setContentTitle(noti.name)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent) // For launching the MainActivity
                .setAutoCancel(true) // Remove notification when tapped
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
                .cyber(context, "// NEW DROP", noti.name, body)
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(noti.hashCode(), builder.build())
        }
    }

    /**
     * Required on Android O+
     */
    fun createNotificationChannel(context: Context) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val name = "Update Followed Artists"
            val descriptionText =
                "This channel sends notification when followed artists release new music"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel =
                NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
            // Register the channel with the system

            notificationManager.createNotificationChannel(channel)
        }
    }

    private const val BLOG_CHANNEL_ID = "blog_updates_channel"

    /**
     * Separate channel so users can silence blog updates without affecting artist-release
     * notifications.
     */
    fun createBlogNotificationChannel(context: Context) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(BLOG_CHANNEL_ID) == null) {
            val channel =
                NotificationChannel(
                    BLOG_CHANNEL_ID,
                    "Blog updates",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Notifies when a new blog post is published"
                }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a local notification for a new blog post. Tapping opens [url] in the browser
     * (ACTION_VIEW). No image is loaded — the feed carries no per-item thumbnail.
     * The notification id is derived from [url] so the same post never stacks duplicates.
     */
    fun createBlogNotification(
        context: Context,
        title: String,
        text: String?,
        url: String,
    ) {
        val action =
            Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                url.hashCode(),
                action,
                PendingIntent.FLAG_IMMUTABLE,
            )
        val builder =
            NotificationCompat
                .Builder(context, BLOG_CHANNEL_ID)
                .setSmallIcon(R.drawable.mono)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .cyber(context, "// TRANSMISSION", title, text)
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(url.hashCode(), builder.build())
        }
    }

    /**
     * CyberSync's notification card: a dark panel with a neon frame in place of the plain system
     * body. The system still draws the header (icon, app name, time) above it.
     */
    private fun NotificationCompat.Builder.cyber(
        context: Context,
        tag: String,
        title: String,
        text: String?,
    ): NotificationCompat.Builder {
        fun views(layout: Int) =
            RemoteViews(context.packageName, layout).apply {
                setTextViewText(R.id.cyber_title, title.uppercase())
                setTextViewText(R.id.cyber_text, text.orEmpty())
            }
        return setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(views(R.layout.notification_cyber_small))
            .setCustomBigContentView(views(R.layout.notification_cyber_big).apply { setTextViewText(R.id.cyber_tag, tag) })
            .setColor(0xFF00F0FF.toInt())
    }
}

package io.github.hatemselim94.salat.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.hatemselim94.salat.MainActivity
import io.github.hatemselim94.salat.R
import io.github.hatemselim94.salat.core.Engine
import io.github.hatemselim94.salat.core.Prayer
import io.github.hatemselim94.salat.core.Settings

object Notifications {
    // Channel sound cannot be changed after creation, so the sound setting picks between two channels.
    // v2: the chime channel now stores a name-based resource URI (numeric ids change between builds,
    // and a dangling URI silently falls back to the system tone). Old channels are deleted below.
    private const val CHANNEL_PRAYER_SILENT = "prayer_silent_v3"
    private const val CHANNEL_PRAYER_DEFAULT = "prayer_default_v3"
    private const val CHANNEL_PRAYER_CHIME = "prayer_chime_v3"
    private const val CHANNEL_PRE = "pre_reminder_v3"
    private val LEGACY_CHANNELS = listOf(
        "prayer_silent", "prayer_default", "prayer_chime", "pre_reminder",
        "prayer_silent_v2", "prayer_default_v2", "prayer_chime_v2", "pre_reminder_v2",
    )
    private const val NOTIFICATION_ID = 100
    const val ACTION_SNOOZE_CLICKED = "io.github.hatemselim94.salat.SNOOZE_CLICKED"

    /** False when the user denied the permission, switched the app's notifications off, or disabled the prayer channel. */
    fun enabled(context: Context): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (!nm.areNotificationsEnabled()) return false
        val channel = nm.getNotificationChannel(CHANNEL_PRAYER_CHIME) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** Opens the system page for this app's notifications (the switch is at the top). */
    fun openSystemSettings(context: Context) {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: Exception) {
            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + context.packageName)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        for (id in LEGACY_CHANNELS) nm.deleteNotificationChannel(id)
        // Name-based URI (stable across builds) built from the real resource so the shrinker keeps it; OGG, as Android's own tones are.
        val chime = Uri.parse("android.resource://${context.packageName}/raw/${context.resources.getResourceEntryName(R.raw.chime)}")
        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        nm.createNotificationChannel(NotificationChannel(CHANNEL_PRAYER_SILENT, context.getString(R.string.notif_channelPrayer), NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null)
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(CHANNEL_PRAYER_DEFAULT, context.getString(R.string.notif_channelPrayerDefault), NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, audio)
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(CHANNEL_PRAYER_CHIME, context.getString(R.string.notif_channelPrayerChime), NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(chime, audio)
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(CHANNEL_PRE, context.getString(R.string.notif_channelPre), NotificationManager.IMPORTANCE_DEFAULT).apply {
            setSound(null, null)
        })
    }

    fun show(context: Context, settings: Settings, engine: Engine, prayer: Prayer, kind: String, epoch: Long, snoozed: Boolean = false, quiet: Boolean = false) {
        val pre = kind == "pre"
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensureChannels(context)

        val date = java.time.Instant.ofEpochSecond(if (epoch > 0) epoch else System.currentTimeMillis() / 1000)
            .atZone(java.time.ZoneId.of(settings.timezone)).toLocalDate()
        val label = engine.label(context, prayer, date)
        val pre_n = settings.preReminderMinutes.toString()
        val (title, text) = when {
            kind == "jumuah" -> context.getString(R.string.notif_jumuahTitle, settings.jumuahReminderMinutes.toString()) to
                context.getString(R.string.notif_jumuahBody, engine.formatTime(epoch + settings.jumuahReminderMinutes * 60))
            pre -> context.getString(R.string.notif_preTitle, label, pre_n) to
                context.getString(R.string.notif_preBody, label, engine.formatTime(epoch + settings.preReminderMinutes * 60))
            snoozed -> context.getString(R.string.notif_snoozedTitle, label) to context.getString(R.string.notif_snoozedBody)
            else -> context.getString(R.string.notif_prayerTitle, label) to context.getString(R.string.notif_prayerBody, label, engine.formatTime(epoch))
        }
        val channel = when {
            pre || kind == "jumuah" -> CHANNEL_PRE
            quiet || settings.soundMode == "silent" -> CHANNEL_PRAYER_SILENT
            settings.soundMode == "chime" -> CHANNEL_PRAYER_CHIME
            else -> CHANNEL_PRAYER_DEFAULT
        }

        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (pre) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setTimeoutAfter(60 * 60 * 1000L) // disappears after an hour on its own
        if (!pre && kind != "jumuah") {
            val snooze = PendingIntent.getBroadcast(
                context, 3,
                Intent(context, AlarmReceiver::class.java).setAction(ACTION_SNOOZE_CLICKED).putExtra(Scheduler.EXTRA_PRAYER, prayer.key),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, context.getString(R.string.notif_snooze, settings.snoozeMinutes.toString()), snooze)
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
    }
}

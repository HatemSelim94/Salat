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
    private const val CHANNEL_PRAYER_SILENT = "prayer_silent"
    private const val CHANNEL_PRAYER_CHIME = "prayer_chime"
    private const val CHANNEL_PRE = "pre_reminder"
    private const val NOTIFICATION_ID = 100
    const val ACTION_SNOOZE_CLICKED = "io.github.hatemselim94.salat.SNOOZE_CLICKED"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chime = Uri.parse("android.resource://${context.packageName}/${R.raw.chime}")
        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        nm.createNotificationChannel(NotificationChannel(CHANNEL_PRAYER_SILENT, "Prayer times", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "One notification at each prayer time"
            setSound(null, null)
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(CHANNEL_PRAYER_CHIME, "Prayer times (with chime)", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "One notification at each prayer time, with a short chime"
            setSound(chime, audio)
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(CHANNEL_PRE, "Heads-up before prayer", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Optional reminder a few minutes before each prayer"
            setSound(null, null)
        })
    }

    fun show(context: Context, settings: Settings, engine: Engine, prayer: Prayer, pre: Boolean, epoch: Long, snoozed: Boolean = false) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensureChannels(context)

        val label = prayer.label
        val (title, text) = when {
            pre -> "$label in ${settings.preReminderMinutes} min" to "$label prayer is at ${engine.formatTime(epoch + settings.preReminderMinutes * 60)}."
            snoozed -> "$label — snoozed reminder" to "You asked to be reminded again."
            else -> "$label time" to "It is time for $label prayer (${engine.formatTime(epoch)})."
        }
        val channel = when {
            pre -> CHANNEL_PRE
            settings.sound -> CHANNEL_PRAYER_CHIME
            else -> CHANNEL_PRAYER_SILENT
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
        if (!pre) {
            val snooze = PendingIntent.getBroadcast(
                context, 3,
                Intent(context, AlarmReceiver::class.java).setAction(ACTION_SNOOZE_CLICKED).putExtra(Scheduler.EXTRA_PRAYER, prayer.key),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, "Snooze ${settings.snoozeMinutes} min", snooze)
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
    }
}

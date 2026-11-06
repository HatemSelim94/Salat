package io.github.hatemselim94.salat.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.hatemselim94.salat.core.Engine
import io.github.hatemselim94.salat.core.Prayer
import io.github.hatemselim94.salat.core.Settings

/**
 * Schedules exactly ONE exact alarm: the next event. When it fires,
 * [AlarmReceiver] shows the notification and schedules the following one.
 * A single pending alarm is the cheapest and most robust shape on Android;
 * boot / time-change receivers simply call [reschedule] again.
 */
object Scheduler {
    private const val REQUEST_NEXT = 1
    private const val REQUEST_SNOOZE = 2
    const val ACTION_FIRE = "io.github.hatemselim94.salat.FIRE"
    const val ACTION_SNOOZE = "io.github.hatemselim94.salat.SNOOZE"
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_PRE = "pre"
    const val EXTRA_EPOCH = "epoch"
    /** Events older than this at fire time are considered missed (device was off) and skipped. */
    const val STALE_SECONDS = 15 * 60

    fun reschedule(context: Context) {
        val settings = Settings(context)
        val engine = Engine(settings)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, REQUEST_NEXT, Intent(context, AlarmReceiver::class.java).setAction(ACTION_FIRE))
        am.cancel(pi)

        val now = System.currentTimeMillis() / 1000
        val next = engine.upcomingEvents(now).firstOrNull() ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_PRAYER, next.prayer.key)
            .putExtra(EXTRA_PRE, next.pre)
            .putExtra(EXTRA_EPOCH, next.epoch)
        val fire = pendingIntent(context, REQUEST_NEXT, intent)
        setExact(am, next.epoch * 1000, fire)
    }

    fun scheduleSnooze(context: Context, prayer: Prayer) {
        val settings = Settings(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_SNOOZE)
            .putExtra(EXTRA_PRAYER, prayer.key)
        val at = System.currentTimeMillis() + settings.snoozeMinutes * 60_000L
        setExact(am, at, pendingIntent(context, REQUEST_SNOOZE, intent))
    }

    /** True when the OS will let us schedule exact alarms (always true on 33+ with USE_EXACT_ALARM). */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    private fun setExact(am: AlarmManager, atMillis: Long, pi: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            else am.setExact(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } catch (e: SecurityException) {
            // Exact alarm permission revoked (Android 12/13 without USE_EXACT_ALARM): degrade to inexact.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    private fun pendingIntent(context: Context, request: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(context, request, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

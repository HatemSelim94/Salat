package io.github.hatemselim94.salat.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.hatemselim94.salat.core.Engine
import io.github.hatemselim94.salat.core.Prayer
import io.github.hatemselim94.salat.core.Settings

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = Prayer.entries.firstOrNull { it.key == intent.getStringExtra(Scheduler.EXTRA_PRAYER) } ?: Prayer.DHUHR
        when (intent.action) {
            Scheduler.ACTION_FIRE -> {
                val pre = intent.getBooleanExtra(Scheduler.EXTRA_PRE, false)
                val epoch = intent.getLongExtra(Scheduler.EXTRA_EPOCH, 0L)
                val now = System.currentTimeMillis() / 1000
                if (now - epoch <= Scheduler.STALE_SECONDS) {
                    val settings = Settings(context)
                    Notifications.show(context, settings, Engine(settings), prayer, pre, epoch)
                }
                Scheduler.reschedule(context)
            }
            Scheduler.ACTION_SNOOZE -> {
                val settings = Settings(context)
                Notifications.show(context, settings, Engine(settings), prayer, pre = false, epoch = 0L, snoozed = true)
            }
            Notifications.ACTION_SNOOZE_CLICKED -> {
                Notifications.cancel(context)
                Scheduler.scheduleSnooze(context, prayer)
            }
        }
    }
}

/** Re-arms the single alarm after reboot, app update, time or time-zone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Scheduler.reschedule(context)
        }
    }
}

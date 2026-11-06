package io.github.hatemselim94.salat.core

import io.github.hatemselim94.salat.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.HijrahChronology
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Prayer(val key: String, val labelRes: Int, val obligatory: Boolean) {
    FAJR("fajr", R.string.prayer_fajr, true),
    SUNRISE("sunrise", R.string.prayer_sunrise, false),
    DHUHR("dhuhr", R.string.prayer_dhuhr, true),
    ASR("asr", R.string.prayer_asr, true),
    MAGHRIB("maghrib", R.string.prayer_maghrib, true),
    ISHA("isha", R.string.prayer_isha, true);

    fun label(context: android.content.Context): String = context.getString(labelRes)

    companion object { val OBLIGATORY = entries.filter { it.obligatory } }
}

data class DayTimes(val date: LocalDate, val times: Map<Prayer, Long>) {
    operator fun get(p: Prayer): Long = times.getValue(p)
}

data class PrayerEvent(val prayer: Prayer, val epoch: Long)

/** A notification to schedule. [kind]: "prayer" | "pre" (heads-up) | "jumuah" (Friday reminder). */
data class ScheduledEvent(val kind: String, val prayer: Prayer, val epoch: Long, val quiet: Boolean) {
    val pre get() = kind == "pre"
}

/** Pure computation on top of [Adhan]; no Android dependencies beyond java.time. */
class Engine(private val settings: Settings) {
    private val zone: ZoneId get() = runCatching { ZoneId.of(settings.timezone) }.getOrElse { ZoneId.systemDefault() }

    fun computeDay(instant: Long, dayOffset: Int = 0): DayTimes {
        val local = Instant.ofEpochSecond(instant).atZone(zone).toLocalDate().plusDays(dayOffset.toLong())
        val t = Adhan.compute(
            Coordinates(settings.latitude, settings.longitude),
            CivilDate(local.year, local.monthValue, local.dayOfMonth),
            settings.calculationParameters(),
        )
        val day = DayTimes(local, mapOf(
            Prayer.FAJR to t.fajr, Prayer.SUNRISE to t.sunrise, Prayer.DHUHR to t.dhuhr,
            Prayer.ASR to t.asr, Prayer.MAGHRIB to t.maghrib, Prayer.ISHA to t.isha,
        ))
        return if (settings.timetable.containsKey(local.toString())) Features.applyTimetable(settings, day) else day
    }

    /** Next obligatory prayer strictly after [now] (wraps to tomorrow's Fajr). */
    fun nextPrayer(now: Long): PrayerEvent {
        for (off in 0..2) {
            val d = computeDay(now, off)
            Prayer.OBLIGATORY.firstOrNull { d[it] > now }?.let { return PrayerEvent(it, d[it]) }
        }
        val d = computeDay(now, 1)
        return PrayerEvent(Prayer.FAJR, d[Prayer.FAJR])
    }

    /** Most recent obligatory prayer at or before [now] (yesterday's Isha before Fajr). */
    fun currentPrayer(now: Long): PrayerEvent {
        for (off in 0 downTo -1) {
            val d = computeDay(now, off)
            Prayer.OBLIGATORY.lastOrNull { d[it] <= now }?.let { return PrayerEvent(it, d[it]) }
        }
        val d = computeDay(now, -1)
        return PrayerEvent(Prayer.ISHA, d[Prayer.ISHA])
    }

    /** Every notification due in (now, now + horizon], sorted. */
    fun upcomingEvents(now: Long, horizonSeconds: Long = 36 * 3600): List<ScheduledEvent> {
        if (!settings.notificationsEnabled) return emptyList()
        val pre = settings.preReminderMinutes
        val jm = if (settings.jumuahEnabled) settings.jumuahReminderMinutes else 0
        val out = mutableListOf<ScheduledEvent>()
        for (off in 0..2) {
            val d = computeDay(now, off)
            val friday = Features.isFriday(d.date)
            for (p in Prayer.OBLIGATORY) {
                val t = d[p]
                if (t <= now || t > now + horizonSeconds) continue
                if (pre > 0 && t - pre * 60 > now) out += ScheduledEvent("pre", p, t - pre * 60, Features.inQuietHours(settings, t - pre * 60))
                if (friday && p == Prayer.DHUHR && jm > 0 && t - jm * 60 > now) out += ScheduledEvent("jumuah", p, t - jm * 60, Features.inQuietHours(settings, t - jm * 60))
                out += ScheduledEvent("prayer", p, t, Features.inQuietHours(settings, t))
            }
        }
        return out.sortedBy { it.epoch }
    }

    fun formatTime(epoch: Long): String {
        val dt = Instant.ofEpochSecond(epoch).atZone(zone)
        val fmt = if (settings.hour12) DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH) else DateTimeFormatter.ofPattern("HH:mm")
        return dt.format(fmt)
    }

    /** Always 24 h, Latin digits — for CSV export. */
    fun formatTime24(epoch: Long): String = Instant.ofEpochSecond(epoch).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))

    /** Display label for a prayer on a given day (Jumu'ah on Fridays). */
    fun label(context: android.content.Context, prayer: Prayer, date: LocalDate): String =
        if (prayer == Prayer.DHUHR && settings.jumuahEnabled && Features.isFriday(date)) context.getString(R.string.prayer_jumuah) else prayer.label(context)

    fun countdown(now: Long, target: Long): String {
        val total = Math.max(0L, Math.round((target - now) / 60.0))
        if (total < 1) return "<1m"
        val h = total / 60; val m = total % 60
        return if (h == 0L) "${m}m" else "${h}h ${m.toString().padStart(2, '0')}m"
    }

    fun gregorianDate(now: Long): String =
        ZonedDateTime.ofInstant(Instant.ofEpochSecond(now), zone).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))

    /** Umm al-Qura (java.time's HijrahChronology default). */
    fun hijriDate(now: Long): String = try {
        val local = Instant.ofEpochSecond(now).atZone(zone).toLocalDate()
        val h = HijrahChronology.INSTANCE.date(local)
        h.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
    } catch (e: Exception) { "" }
}

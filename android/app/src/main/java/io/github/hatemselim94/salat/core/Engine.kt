package io.github.hatemselim94.salat.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.HijrahChronology
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Prayer(val key: String, val label: String, val obligatory: Boolean) {
    FAJR("fajr", "Fajr", true),
    SUNRISE("sunrise", "Sunrise", false),
    DHUHR("dhuhr", "Dhuhr", true),
    ASR("asr", "Asr", true),
    MAGHRIB("maghrib", "Maghrib", true),
    ISHA("isha", "Isha", true);

    companion object { val OBLIGATORY = entries.filter { it.obligatory } }
}

data class DayTimes(val date: LocalDate, val times: Map<Prayer, Long>) {
    operator fun get(p: Prayer): Long = times.getValue(p)
}

data class PrayerEvent(val prayer: Prayer, val epoch: Long)

/** A notification to schedule. [pre] = heads-up before the prayer. */
data class ScheduledEvent(val pre: Boolean, val prayer: Prayer, val epoch: Long)

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
        return DayTimes(local, mapOf(
            Prayer.FAJR to t.fajr, Prayer.SUNRISE to t.sunrise, Prayer.DHUHR to t.dhuhr,
            Prayer.ASR to t.asr, Prayer.MAGHRIB to t.maghrib, Prayer.ISHA to t.isha,
        ))
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
        val out = mutableListOf<ScheduledEvent>()
        for (off in 0..2) {
            val d = computeDay(now, off)
            for (p in Prayer.OBLIGATORY) {
                val t = d[p]
                if (t <= now || t > now + horizonSeconds) continue
                if (pre > 0 && t - pre * 60 > now) out += ScheduledEvent(true, p, t - pre * 60)
                out += ScheduledEvent(false, p, t)
            }
        }
        return out.sortedBy { it.epoch }
    }

    fun formatTime(epoch: Long): String {
        val dt = Instant.ofEpochSecond(epoch).atZone(zone)
        val fmt = if (settings.hour12) DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH) else DateTimeFormatter.ofPattern("HH:mm")
        return dt.format(fmt)
    }

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
        h.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH))
    } catch (e: Exception) { "" }
}

package io.github.hatemselim94.salat.core

import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Mirrors core/src/features.ts. Pure computation. */
object Features {
    private const val KAABA_LAT = 21.4225
    private const val KAABA_LNG = 39.8262

    /** Initial great-circle bearing to the Kaaba, degrees clockwise from true north [0, 360). */
    fun qiblaBearing(latitude: Double, longitude: Double): Double {
        val p1 = Math.toRadians(latitude)
        val p2 = Math.toRadians(KAABA_LAT)
        val dl = Math.toRadians(KAABA_LNG - longitude)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    fun distanceToKaabaKm(latitude: Double, longitude: Double): Double {
        val p1 = Math.toRadians(latitude); val p2 = Math.toRadians(KAABA_LAT)
        val dp = p2 - p1; val dl = Math.toRadians(KAABA_LNG - longitude)
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 6371 * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun hijri(local: LocalDate): HijrahDate = HijrahDate.from(local)

    fun isRamadan(settings: Settings, local: LocalDate): Boolean = when (settings.ramadan) {
        "on" -> true
        "off" -> false
        else -> runCatching { hijri(local).get(java.time.temporal.ChronoField.MONTH_OF_YEAR) == 9 }.getOrDefault(false)
    }

    fun isFriday(local: LocalDate) = local.dayOfWeek == DayOfWeek.FRIDAY

    /** Minutes since midnight for "HH:MM"; null if malformed. */
    fun minutesOfDay(hhmm: String): Int? {
        val m = Regex("^(\\d{1,2}):(\\d{2})$").find(hhmm.trim()) ?: return null
        val h = m.groupValues[1].toInt(); val min = m.groupValues[2].toInt()
        return if (h in 0..23 && min in 0..59) h * 60 + min else null
    }

    fun inQuietHours(settings: Settings, epoch: Long): Boolean {
        if (!settings.quietHoursEnabled) return false
        val from = minutesOfDay(settings.quietFrom) ?: return false
        val to = minutesOfDay(settings.quietTo) ?: return false
        val t = Instant.ofEpochSecond(epoch).atZone(ZoneId.of(settings.timezone)).toLocalTime()
        val now = t.hour * 60 + t.minute
        return if (from <= to) now >= from && now < to else now >= from || now < to
    }

    // ---- month table + CSV ---------------------------------------------------

    data class MonthRow(val date: LocalDate, val times: DayTimes, val friday: Boolean, val imported: Boolean)

    fun monthTable(settings: Settings, engine: Engine, ym: YearMonth): List<MonthRow> {
        val zone = ZoneId.of(settings.timezone)
        return (1..ym.lengthOfMonth()).map { d ->
            val local = ym.atDay(d)
            val noon = local.atTime(12, 0).atZone(zone).toEpochSecond()
            val day = engine.computeDay(noon)
            MonthRow(local, day, isFriday(local), settings.timetable.containsKey(local.toString()))
        }
    }

    fun monthCsv(engine: Engine, rows: List<MonthRow>): String {
        val sb = StringBuilder("date,fajr,sunrise,dhuhr,asr,maghrib,isha\n")
        for (r in rows) {
            sb.append(r.date).append(',')
            sb.append(Prayer.entries.joinToString(",") { engine.formatTime24(r.times[it]) }).append('\n')
        }
        return sb.toString()
    }

    // ---- timetable import -------------------------------------------------------

    data class ImportResult(val entries: Map<String, Map<String, String>>, val errors: List<String>) {
        val days get() = entries.size
    }

    private val aliases = mapOf(
        "date" to "date", "datum" to "date", "day" to "date", "تاريخ" to "date", "التاريخ" to "date",
        "fajr" to "fajr", "fajer" to "fajr", "fadschr" to "fajr", "الفجر" to "fajr", "فجر" to "fajr",
        "sunrise" to "sunrise", "shuruq" to "sunrise", "shurooq" to "sunrise", "sonnenaufgang" to "sunrise", "الشروق" to "sunrise", "شروق" to "sunrise",
        "dhuhr" to "dhuhr", "zuhr" to "dhuhr", "duhr" to "dhuhr", "dhuhur" to "dhuhr", "الظهر" to "dhuhr", "ظهر" to "dhuhr",
        "asr" to "asr", "العصر" to "asr", "عصر" to "asr",
        "maghrib" to "maghrib", "magrib" to "maghrib", "المغرب" to "maghrib", "مغرب" to "maghrib",
        "isha" to "isha", "ishaa" to "isha", "العشاء" to "isha", "عشاء" to "isha",
    )

    fun parseTimetableCsv(text: String): ImportResult {
        val entries = linkedMapOf<String, Map<String, String>>()
        val errors = mutableListOf<String>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return ImportResult(entries, listOf("empty file"))
        val sep = when { lines[0].contains(';') -> ';'; lines[0].contains('\t') -> '\t'; else -> ',' }
        val first = lines[0].split(sep).map { it.trim().lowercase().replace("\"", "").replace("'", "") }
        val columns: List<String?>
        val start: Int
        if (first.any { aliases[it] == "date" }) { columns = first.map { aliases[it] }; start = 1 }
        else { columns = listOf("date", "fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"); start = 0 }
        val dateIdx = columns.indexOf("date")
        for (i in start until lines.size) {
            val cells = lines[i].split(sep).map { it.trim().replace("\"", "").replace("'", "") }
            val iso = normaliseDate(cells.getOrNull(dateIdx) ?: "")
            if (iso == null) { errors += "line ${i + 1}: unreadable date"; continue }
            val row = linkedMapOf<String, String>()
            columns.forEachIndexed { idx, col ->
                if (col == null || col == "date") return@forEachIndexed
                val raw = cells.getOrNull(idx) ?: ""
                val tm = normaliseTime(raw)
                if (tm != null) row[col] = tm else if (raw.isNotEmpty()) errors += "line ${i + 1}: unreadable time \"$raw\""
            }
            if (row.isNotEmpty()) entries[iso] = row
        }
        return ImportResult(entries, errors)
    }

    private fun normaliseDate(s: String): String? {
        Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$").find(s)?.let { m ->
            return "%s-%02d-%02d".format(m.groupValues[1], m.groupValues[2].toInt(), m.groupValues[3].toInt())
        }
        Regex("^(\\d{1,2})[./](\\d{1,2})[./](\\d{4})$").find(s)?.let { m ->
            return "%s-%02d-%02d".format(m.groupValues[3], m.groupValues[2].toInt(), m.groupValues[1].toInt())
        }
        return null
    }

    private fun normaliseTime(s: String): String? {
        val m = Regex("^(\\d{1,2}):(\\d{2})(?::\\d{2})?\\s*(am|pm)?$", RegexOption.IGNORE_CASE).find(s) ?: return null
        var h = m.groupValues[1].toInt(); val min = m.groupValues[2].toInt()
        val ampm = m.groupValues[3].lowercase()
        if (ampm.isNotEmpty()) { if (ampm == "pm" && h < 12) h += 12; if (ampm == "am" && h == 12) h = 0 }
        if (h > 23 || min > 59) return null
        return "%02d:%02d".format(h, min)
    }

    /** Replace calculated times with imported ones for that civil date. */
    fun applyTimetable(settings: Settings, day: DayTimes): DayTimes {
        val row = settings.timetable[day.date.toString()] ?: return day
        val zone = ZoneId.of(settings.timezone)
        val times = day.times.toMutableMap()
        for (p in Prayer.entries) {
            val hhmm = row[p.key] ?: continue
            val lt = runCatching { LocalTime.parse(hhmm, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull() ?: continue
            times[p] = day.date.atTime(lt).atZone(zone).toEpochSecond()
        }
        return DayTimes(day.date, times)
    }

    // JSON (de)serialisation of the timetable for storage
    fun timetableToJson(entries: Map<String, Map<String, String>>): String {
        val root = JSONObject()
        for ((date, row) in entries) root.put(date, JSONObject(row as Map<*, *>))
        return root.toString()
    }

    fun timetableFromJson(json: String): Map<String, Map<String, String>> = runCatching {
        val root = JSONObject(json)
        val out = linkedMapOf<String, Map<String, String>>()
        for (date in root.keys()) {
            val o = root.getJSONObject(date)
            out[date] = o.keys().asSequence().associateWith { o.getString(it) }
        }
        out as Map<String, Map<String, String>>
    }.getOrDefault(emptyMap())
}

package io.github.hatemselim94.salat.core

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.util.TimeZone

/** Settings mirroring @salat/core's schema, stored in SharedPreferences. */
class Settings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("salat", Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

    init {
        if (!prefs.contains(KEY_LAT)) {
            val d = Cities.load(appContext).firstOrNull { it.tz == TimeZone.getDefault().id }
            prefs.edit()
                .putLong(KEY_LAT, (d?.lat ?: 21.39).toRawBits())
                .putLong(KEY_LNG, (d?.lng ?: 39.86).toRawBits())
                .putString(KEY_TZ, d?.tz ?: "Asia/Riyadh")
                .putString(KEY_LABEL, d?.let { "${it.name}, ${it.country}" } ?: "Makkah, SA")
                .putBoolean(KEY_CONFIGURED, false)
                .apply()
        }
    }

    var latitude: Double
        get() = getDouble(KEY_LAT, 21.39)
        set(v) = prefs.edit().putLong(KEY_LAT, v.toRawBits()).apply()
    var longitude: Double
        get() = getDouble(KEY_LNG, 39.86)
        set(v) = prefs.edit().putLong(KEY_LNG, v.toRawBits()).apply()

    private fun getDouble(key: String, default: Double): Double =
        if (prefs.contains(key)) Double.fromBits(prefs.getLong(key, 0L)) else default
    var timezone: String
        get() = prefs.getString(KEY_TZ, "Asia/Riyadh")!!
        set(v) { if (runCatching { java.time.ZoneId.of(v) }.isSuccess) prefs.edit().putString(KEY_TZ, v).apply() }
    var label: String
        get() = prefs.getString(KEY_LABEL, "")!!
        set(v) = prefs.edit().putString(KEY_LABEL, v).apply()
    val locationConfigured: Boolean get() = prefs.getBoolean(KEY_CONFIGURED, false)

    var method: Method
        get() = Method.fromId(prefs.getString("method", "MuslimWorldLeague")!!)
        set(v) = prefs.edit().putString("method", v.id).apply()
    var madhab: Madhab
        get() = if (prefs.getString("madhab", "shafi") == "hanafi") Madhab.HANAFI else Madhab.SHAFI
        set(v) = prefs.edit().putString("madhab", if (v == Madhab.HANAFI) "hanafi" else "shafi").apply()
    /** "recommended" | "middleOfTheNight" | "seventhOfTheNight" | "twilightAngle" */
    var highLatitudeRule: String
        get() = prefs.getString("highLatitudeRule", "recommended")!!
        set(v) = prefs.edit().putString("highLatitudeRule", v).apply()

    var customFajrAngle: Double
        get() = prefs.getFloat("custom_fajr", 18f).toDouble()
        set(v) { if (v in 5.0..25.0) prefs.edit().putFloat("custom_fajr", v.toFloat()).apply() }
    var customIshaAngle: Double
        get() = prefs.getFloat("custom_isha", 17f).toDouble()
        set(v) { if (v in 5.0..25.0) prefs.edit().putFloat("custom_isha", v.toFloat()).apply() }
    var customIshaInterval: Int
        get() = prefs.getInt("custom_isha_interval", 0)
        set(v) = prefs.edit().putInt("custom_isha_interval", v.coerceIn(0, 180)).apply()

    fun adjustment(prayer: String): Int = prefs.getInt("adjust_$prayer", 0)
    fun setAdjustment(prayer: String, minutes: Int) = prefs.edit().putInt("adjust_$prayer", minutes.coerceIn(-60, 60)).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notify_enabled", true)
        set(v) = prefs.edit().putBoolean("notify_enabled", v).apply()
    var preReminderMinutes: Int
        get() = prefs.getInt("notify_pre", 0)
        set(v) = prefs.edit().putInt("notify_pre", v.coerceIn(0, 60)).apply()
    /** "silent" | "default" (system notification sound) | "chime". The chime is the default: distinctive, and on by default. */
    var soundMode: String
        get() = prefs.getString("notify_soundMode", "chime")!!
        set(v) = prefs.edit().putString("notify_soundMode", v).apply()
    var snoozeMinutes: Int
        get() = prefs.getInt("notify_snooze", 10)
        set(v) = prefs.edit().putInt("notify_snooze", v.coerceIn(1, 60)).apply()
    var hour12: Boolean
        get() = prefs.getBoolean("hour12", false)
        set(v) = prefs.edit().putBoolean("hour12", v).apply()

    fun iqamah(prayer: String): Int = prefs.getInt("iqamah_$prayer", 0)
    fun setIqamah(prayer: String, minutes: Int) = prefs.edit().putInt("iqamah_$prayer", minutes.coerceIn(0, 120)).apply()
    var jumuahEnabled: Boolean
        get() = prefs.getBoolean("jumuah_enabled", true)
        set(v) = prefs.edit().putBoolean("jumuah_enabled", v).apply()
    var jumuahReminderMinutes: Int
        get() = prefs.getInt("jumuah_reminder", 0)
        set(v) = prefs.edit().putInt("jumuah_reminder", v.coerceIn(0, 180)).apply()
    /** "auto" | "on" | "off" */
    var ramadan: String
        get() = prefs.getString("ramadan", "auto")!!
        set(v) = prefs.edit().putString("ramadan", v).apply()
    var quietHoursEnabled: Boolean
        get() = prefs.getBoolean("quiet_enabled", false)
        set(v) = prefs.edit().putBoolean("quiet_enabled", v).apply()
    var quietFrom: String
        get() = prefs.getString("quiet_from", "22:00")!!
        set(v) { if (Features.minutesOfDay(v) != null) prefs.edit().putString("quiet_from", v).apply() }
    var quietTo: String
        get() = prefs.getString("quiet_to", "06:00")!!
        set(v) { if (Features.minutesOfDay(v) != null) prefs.edit().putString("quiet_to", v).apply() }

    /** Imported mosque timetable (date → prayer → HH:MM), kept in a file; the prefs only hold a revision counter. */
    private val timetableFile = java.io.File(appContext.filesDir, "timetable.json")
    @Volatile private var timetableCache: Map<String, Map<String, String>>? = null
    val timetable: Map<String, Map<String, String>>
        get() = timetableCache ?: synchronized(this) {
            timetableCache ?: (if (timetableFile.exists()) Features.timetableFromJson(timetableFile.readText()) else emptyMap()).also { timetableCache = it }
        }
    fun setTimetable(entries: Map<String, Map<String, String>>) {
        synchronized(this) {
            if (entries.isEmpty()) timetableFile.delete() else timetableFile.writeText(Features.timetableToJson(entries))
            timetableCache = entries
        }
        prefs.edit().putInt("timetable_rev", prefs.getInt("timetable_rev", 0) + 1).apply() // notifies listeners
    }

    fun setLocation(lat: Double, lng: Double, tz: String, label: String) {
        prefs.edit()
            .putLong(KEY_LAT, lat.toRawBits()).putLong(KEY_LNG, lng.toRawBits())
            .putString(KEY_TZ, tz).putString(KEY_LABEL, label)
            .putBoolean(KEY_CONFIGURED, true)
            .apply()
    }

    fun registerListener(l: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.registerOnSharedPreferenceChangeListener(l)
    fun unregisterListener(l: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(l)

    fun calculationParameters(): CalculationParameters {
        val coords = Coordinates(latitude, longitude)
        val rule = when (highLatitudeRule) {
            "middleOfTheNight" -> HighLatitudeRule.MIDDLE_OF_THE_NIGHT
            "seventhOfTheNight" -> HighLatitudeRule.SEVENTH_OF_THE_NIGHT
            "twilightAngle" -> HighLatitudeRule.TWILIGHT_ANGLE
            else -> CalculationParameters.recommendedHighLatitudeRule(coords)
        }
        val base = CalculationParameters.forMethod(method)
        val withCustom = if (method == Method.CUSTOM) base.copy(fajrAngle = customFajrAngle, ishaAngle = customIshaAngle, ishaInterval = customIshaInterval) else base
        return withCustom.copy(
            madhab = madhab,
            highLatitudeRule = rule,
            adjustments = Adjustments(
                fajr = adjustment("fajr"), sunrise = adjustment("sunrise"), dhuhr = adjustment("dhuhr"),
                asr = adjustment("asr"), maghrib = adjustment("maghrib"), isha = adjustment("isha"),
            ),
        )
    }

    companion object {
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
        private const val KEY_TZ = "tz"
        private const val KEY_LABEL = "label"
        private const val KEY_CONFIGURED = "configured"
    }
}

data class City(val name: String, val country: String, val lat: Double, val lng: Double, val tz: String) {
    val label get() = "$name, $country"
}

object Cities {
    @Volatile private var cache: List<City>? = null

    fun load(context: Context): List<City> = cache ?: synchronized(this) {
        cache ?: parse(context.assets.open("cities.json").bufferedReader().readText()).also { cache = it }
    }

    private fun parse(json: String): List<City> {
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            City(o.getString("name"), o.getString("country"), o.getDouble("lat"), o.getDouble("lng"), o.getString("tz"))
        }
    }

    fun search(context: Context, query: String, limit: Int = 8): List<City> {
        val q = normalise(query)
        if (q.isEmpty()) return emptyList()
        val all = load(context)
        val starts = all.filter { normalise(it.name).startsWith(q) }
        val contains = all.filter { it !in starts && (normalise(it.name).contains(q) || it.country.lowercase() == q) }
        return (starts + contains).take(limit)
    }

    private fun normalise(s: String): String =
        java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase().trim()
}

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
                .putFloat(KEY_LAT, (d?.lat ?: 21.39).toFloat())
                .putFloat(KEY_LNG, (d?.lng ?: 39.86).toFloat())
                .putString(KEY_TZ, d?.tz ?: "Asia/Riyadh")
                .putString(KEY_LABEL, d?.let { "${it.name}, ${it.country}" } ?: "Makkah, SA")
                .putBoolean(KEY_CONFIGURED, false)
                .apply()
        }
    }

    var latitude: Double
        get() = prefs.getFloat(KEY_LAT, 21.39f).toDouble()
        set(v) = prefs.edit().putFloat(KEY_LAT, v.toFloat()).apply()
    var longitude: Double
        get() = prefs.getFloat(KEY_LNG, 39.86f).toDouble()
        set(v) = prefs.edit().putFloat(KEY_LNG, v.toFloat()).apply()
    var timezone: String
        get() = prefs.getString(KEY_TZ, "Asia/Riyadh")!!
        set(v) { if (TimeZone.getAvailableIDs().contains(v)) prefs.edit().putString(KEY_TZ, v).apply() }
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

    fun adjustment(prayer: String): Int = prefs.getInt("adjust_$prayer", 0)
    fun setAdjustment(prayer: String, minutes: Int) = prefs.edit().putInt("adjust_$prayer", minutes.coerceIn(-60, 60)).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notify_enabled", true)
        set(v) = prefs.edit().putBoolean("notify_enabled", v).apply()
    var preReminderMinutes: Int
        get() = prefs.getInt("notify_pre", 0)
        set(v) = prefs.edit().putInt("notify_pre", v.coerceIn(0, 60)).apply()
    var sound: Boolean
        get() = prefs.getBoolean("notify_sound", false)
        set(v) = prefs.edit().putBoolean("notify_sound", v).apply()
    var snoozeMinutes: Int
        get() = prefs.getInt("notify_snooze", 10)
        set(v) = prefs.edit().putInt("notify_snooze", v.coerceIn(1, 60)).apply()
    var hour12: Boolean
        get() = prefs.getBoolean("hour12", false)
        set(v) = prefs.edit().putBoolean("hour12", v).apply()

    fun setLocation(lat: Double, lng: Double, tz: String, label: String) {
        prefs.edit()
            .putFloat(KEY_LAT, lat.toFloat()).putFloat(KEY_LNG, lng.toFloat())
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
        return CalculationParameters.forMethod(method).copy(
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

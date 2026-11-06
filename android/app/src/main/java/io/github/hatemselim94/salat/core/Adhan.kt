// Kotlin port of the adhan-js prayer-time algorithm (batoulapps/adhan-js, MIT — see THIRD_PARTY_NOTICES.md).
// Line-by-line translation of desktop/src/adhan/adhan.cpp, which is verified
// against @salat/core. AdhanTest checks this file against the same vectors.
package io.github.hatemselim94.salat.core

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.truncate

data class Coordinates(val latitude: Double, val longitude: Double)
data class CivilDate(val year: Int, val month: Int, val day: Int)

enum class Madhab { SHAFI, HANAFI }
enum class HighLatitudeRule { MIDDLE_OF_THE_NIGHT, SEVENTH_OF_THE_NIGHT, TWILIGHT_ANGLE }
enum class Rounding { NEAREST, UP, NONE }
enum class Shafaq { GENERAL, AHMER, ABYAD }

enum class Method(val id: String, val label: String) {
    MUSLIM_WORLD_LEAGUE("MuslimWorldLeague", "Muslim World League"),
    EGYPTIAN("Egyptian", "Egyptian General Authority"),
    KARACHI("Karachi", "Karachi"),
    UMM_AL_QURA("UmmAlQura", "Umm al-Qura (Makkah)"),
    DUBAI("Dubai", "Dubai"),
    MOONSIGHTING_COMMITTEE("MoonsightingCommittee", "Moonsighting Committee"),
    NORTH_AMERICA("NorthAmerica", "ISNA (North America)"),
    KUWAIT("Kuwait", "Kuwait"),
    QATAR("Qatar", "Qatar"),
    SINGAPORE("Singapore", "Singapore"),
    TEHRAN("Tehran", "Tehran"),
    TURKEY("Turkey", "Diyanet (Turkey)"),
    OTHER("Other", "Other");

    companion object {
        fun fromId(id: String): Method = entries.firstOrNull { it.id == id } ?: MUSLIM_WORLD_LEAGUE
    }
}

data class Adjustments(
    val fajr: Int = 0, val sunrise: Int = 0, val dhuhr: Int = 0,
    val asr: Int = 0, val maghrib: Int = 0, val isha: Int = 0,
)

data class CalculationParameters(
    val method: Method = Method.OTHER,
    val fajrAngle: Double = 0.0,
    val ishaAngle: Double = 0.0,
    val ishaInterval: Int = 0,
    val maghribAngle: Double = 0.0,
    val madhab: Madhab = Madhab.SHAFI,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
    val adjustments: Adjustments = Adjustments(),
    val methodAdjustments: Adjustments = Adjustments(),
    val rounding: Rounding = Rounding.NEAREST,
    val shafaq: Shafaq = Shafaq.GENERAL,
) {
    /** fajr portion to isha portion of the night. */
    fun nightPortions(): Pair<Double, Double> = when (highLatitudeRule) {
        HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> 0.5 to 0.5
        HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> 1.0 / 7 to 1.0 / 7
        HighLatitudeRule.TWILIGHT_ANGLE -> fajrAngle / 60 to ishaAngle / 60
    }

    companion object {
        fun forMethod(m: Method): CalculationParameters = when (m) {
            Method.MUSLIM_WORLD_LEAGUE -> CalculationParameters(m, 18.0, 17.0, methodAdjustments = Adjustments(dhuhr = 1))
            Method.EGYPTIAN -> CalculationParameters(m, 19.5, 17.5, methodAdjustments = Adjustments(dhuhr = 1))
            Method.KARACHI -> CalculationParameters(m, 18.0, 18.0, methodAdjustments = Adjustments(dhuhr = 1))
            Method.UMM_AL_QURA -> CalculationParameters(m, 18.5, 0.0, ishaInterval = 90)
            Method.DUBAI -> CalculationParameters(m, 18.2, 18.2, methodAdjustments = Adjustments(sunrise = -3, dhuhr = 3, asr = 3, maghrib = 3))
            Method.MOONSIGHTING_COMMITTEE -> CalculationParameters(m, 18.0, 18.0, methodAdjustments = Adjustments(dhuhr = 5, maghrib = 3))
            Method.NORTH_AMERICA -> CalculationParameters(m, 15.0, 15.0, methodAdjustments = Adjustments(dhuhr = 1))
            Method.KUWAIT -> CalculationParameters(m, 18.0, 17.5)
            Method.QATAR -> CalculationParameters(m, 18.0, 0.0, ishaInterval = 90)
            Method.SINGAPORE -> CalculationParameters(m, 20.0, 18.0, methodAdjustments = Adjustments(dhuhr = 1), rounding = Rounding.UP)
            Method.TEHRAN -> CalculationParameters(m, 17.7, 14.0, maghribAngle = 4.5)
            Method.TURKEY -> CalculationParameters(m, 18.0, 17.0, methodAdjustments = Adjustments(sunrise = -7, dhuhr = 5, asr = 4, maghrib = 7))
            Method.OTHER -> CalculationParameters(m)
        }

        fun recommendedHighLatitudeRule(c: Coordinates): HighLatitudeRule =
            if (c.latitude > 48) HighLatitudeRule.SEVENTH_OF_THE_NIGHT else HighLatitudeRule.MIDDLE_OF_THE_NIGHT
    }
}

/** UTC epoch seconds for each prayer. */
data class PrayerTimes(
    val fajr: Long, val sunrise: Long, val dhuhr: Long,
    val asr: Long, val sunset: Long, val maghrib: Long, val isha: Long,
)

object Adhan {
    private const val HIGH_LATITUDE_THRESHOLD = 55.0

    // ---- date helpers (proleptic Gregorian, UTC) ----------------------------

    fun epochFromCivilUtc(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0): Long {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (month + (if (month > 2) -3 else 9)) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        val days = era * 146097L + doe - 719468L
        return days * 86400 + hour * 3600L + minute * 60L + second
    }

    fun civilFromEpochUtc(epoch: Long): CivilDate {
        var z = Math.floorDiv(epoch, 86400L) + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = (z - era * 146097).toInt()
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + if (mp < 10) 3 else -9
        return CivilDate((y + if (m <= 2) 1 else 0).toInt(), m, d)
    }

    fun addDays(d: CivilDate, days: Int): CivilDate =
        civilFromEpochUtc(epochFromCivilUtc(d.year, d.month, d.day) + days * 86400L)

    private fun isLeapYear(y: Int) = y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)

    private fun dayOfYear(d: CivilDate): Int {
        val months = intArrayOf(31, if (isLeapYear(d.year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var n = 0
        for (i in 0 until d.month - 1) n += months[i]
        return n + d.day
    }

    // ---- math ---------------------------------------------------------------

    private fun deg2rad(d: Double) = d * Math.PI / 180.0
    private fun rad2deg(r: Double) = r * 180.0 / Math.PI
    private fun normalizeToScale(num: Double, max: Double) = num - max * floor(num / max)
    private fun unwindAngle(a: Double) = normalizeToScale(a, 360.0)
    private fun quadrantShiftAngle(a: Double) = if (a >= -180 && a <= 180) a else a - 360 * floor(a / 360 + 0.5)

    // ---- astronomical (Meeus) ---------------------------------------------

    private fun julianDay(year: Int, month: Int, day: Int, hours: Double = 0.0): Double {
        val y = (if (month > 2) year else year - 1).toDouble()
        val m = (if (month > 2) month else month + 12).toDouble()
        val d = day + hours / 24.0
        val a = truncate(y / 100)
        val b = truncate(2 - a + truncate(a / 4))
        val i0 = truncate(365.25 * (y + 4716))
        val i1 = truncate(30.6001 * (m + 1))
        return i0 + i1 + d + b - 1524.5
    }

    private fun julianCentury(jd: Double) = (jd - 2451545.0) / 36525
    private fun meanSolarLongitude(t: Double) = unwindAngle(280.4664567 + 36000.76983 * t + 0.0003032 * t * t)
    private fun meanLunarLongitude(t: Double) = unwindAngle(218.3165 + 481267.8813 * t)
    private fun ascendingLunarNodeLongitude(t: Double) =
        unwindAngle(125.04452 - 1934.136261 * t + 0.0020708 * t * t + t * t * t / 450000)
    private fun meanSolarAnomaly(t: Double) = unwindAngle(357.52911 + 35999.05029 * t - 0.0001537 * t * t)
    private fun solarEquationOfTheCenter(t: Double, m: Double): Double {
        val mrad = deg2rad(m)
        return (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mrad) + (0.019993 - 0.000101 * t) * sin(2 * mrad) + 0.000289 * sin(3 * mrad)
    }
    private fun apparentSolarLongitude(t: Double, l0: Double): Double {
        val longitude = l0 + solarEquationOfTheCenter(t, meanSolarAnomaly(t))
        val omega = 125.04 - 1934.136 * t
        return unwindAngle(longitude - 0.00569 - 0.00478 * sin(deg2rad(omega)))
    }
    private fun meanObliquityOfTheEcliptic(t: Double) = 23.439291 - 0.013004167 * t - 0.0000001639 * t * t + 0.0000005036 * t * t * t
    private fun apparentObliquityOfTheEcliptic(t: Double, eps0: Double) = eps0 + 0.00256 * cos(deg2rad(125.04 - 1934.136 * t))
    private fun meanSiderealTime(t: Double): Double {
        val jd = t * 36525 + 2451545.0
        return unwindAngle(280.46061837 + 360.98564736629 * (jd - 2451545) + 0.000387933 * t * t - t * t * t / 38710000)
    }
    private fun nutationInLongitude(l0: Double, lp: Double, omega: Double) =
        -17.2 / 3600 * sin(deg2rad(omega)) - 1.32 / 3600 * sin(2 * deg2rad(l0)) - 0.23 / 3600 * sin(2 * deg2rad(lp)) + 0.21 / 3600 * sin(2 * deg2rad(omega))
    private fun nutationInObliquity(l0: Double, lp: Double, omega: Double) =
        9.2 / 3600 * cos(deg2rad(omega)) + 0.57 / 3600 * cos(2 * deg2rad(l0)) + 0.1 / 3600 * cos(2 * deg2rad(lp)) - 0.09 / 3600 * cos(2 * deg2rad(omega))
    private fun altitudeOfCelestialBody(phi: Double, delta: Double, h: Double) =
        rad2deg(asin(sin(deg2rad(phi)) * sin(deg2rad(delta)) + cos(deg2rad(phi)) * cos(deg2rad(delta)) * cos(deg2rad(h))))
    private fun interpolate(y2: Double, y1: Double, y3: Double, n: Double): Double {
        val a = y2 - y1; val b = y3 - y2; val c = b - a
        return y2 + n / 2 * (a + b + n * c)
    }
    private fun interpolateAngles(y2: Double, y1: Double, y3: Double, n: Double): Double {
        val a = unwindAngle(y2 - y1); val b = unwindAngle(y3 - y2); val c = b - a
        return y2 + n / 2 * (a + b + n * c)
    }
    private fun approximateTransit(l: Double, theta0: Double, a2: Double): Double {
        val lw = -l
        val m0 = normalizeToScale((a2 + lw - theta0) / 360, 1.0)
        val expected = normalizeToScale((12.0 - l / 15.0) / 24.0, 1.0)
        return when {
            m0 - expected > 0.5 -> m0 - 1.0
            expected - m0 > 0.5 -> m0 + 1.0
            else -> m0
        }
    }
    private fun correctedTransit(m0: Double, l: Double, theta0: Double, a2: Double, a1: Double, a3: Double): Double {
        val lw = -l
        val theta = unwindAngle(theta0 + 360.985647 * m0)
        val a = unwindAngle(interpolateAngles(a2, a1, a3, m0))
        val h = quadrantShiftAngle(theta - lw - a)
        return (m0 + h / -360) * 24
    }
    private fun correctedHourAngle(
        m0: Double, h0: Double, c: Coordinates, afterTransit: Boolean, theta0: Double,
        a2: Double, a1: Double, a3: Double, d2: Double, d1: Double, d3: Double,
    ): Double {
        val lw = -c.longitude
        val term1 = sin(deg2rad(h0)) - sin(deg2rad(c.latitude)) * sin(deg2rad(d2))
        val term2 = cos(deg2rad(c.latitude)) * cos(deg2rad(d2))
        val hh0 = rad2deg(acos(term1 / term2))  // NaN when the sun never reaches h0
        val m = if (afterTransit) m0 + hh0 / 360 else m0 - hh0 / 360
        val theta = unwindAngle(theta0 + 360.985647 * m)
        val a = unwindAngle(interpolateAngles(a2, a1, a3, m))
        val delta = interpolate(d2, d1, d3, m)
        val h = theta - lw - a
        val alt = altitudeOfCelestialBody(c.latitude, delta, h)
        val dm = (alt - h0) / (360 * cos(deg2rad(delta)) * cos(deg2rad(c.latitude)) * sin(deg2rad(h)))
        return (m + dm) * 24
    }

    private fun daysSinceSolstice(doy: Int, year: Int, latitude: Double): Int {
        val daysInYear = if (isLeapYear(year)) 366 else 365
        return if (latitude >= 0) {
            var d = doy + 10
            if (d >= daysInYear) d -= daysInYear
            d
        } else {
            var d = doy - (if (isLeapYear(year)) 173 else 172)
            if (d < 0) d += daysInYear
            d
        }
    }

    private fun seasonAdjustment(a: Double, b: Double, c: Double, d: Double, dyy: Int): Double = when {
        dyy < 91 -> a + (b - a) / 91.0 * dyy
        dyy < 137 -> b + (c - b) / 46.0 * (dyy - 91)
        dyy < 183 -> c + (d - c) / 46.0 * (dyy - 137)
        dyy < 229 -> d + (c - d) / 46.0 * (dyy - 183)
        dyy < 275 -> c + (b - c) / 46.0 * (dyy - 229)
        else -> b + (a - b) / 91.0 * (dyy - 275)
    }

    private fun seasonAdjustedMorningTwilight(lat: Double, doy: Int, year: Int, sunrise: Long): Long {
        val al = abs(lat)
        val adj = seasonAdjustment(75 + 28.65 / 55.0 * al, 75 + 19.44 / 55.0 * al, 75 + 32.74 / 55.0 * al, 75 + 48.1 / 55.0 * al, daysSinceSolstice(doy, year, lat))
        return sunrise + Math.round(adj * -60.0)
    }

    private fun seasonAdjustedEveningTwilight(lat: Double, doy: Int, year: Int, sunset: Long, shafaq: Shafaq): Long {
        val al = abs(lat)
        val (a, b, c, d) = when (shafaq) {
            Shafaq.AHMER -> listOf(62 + 17.4 / 55.0 * al, 62 - 7.16 / 55.0 * al, 62 + 5.12 / 55.0 * al, 62 + 19.44 / 55.0 * al)
            Shafaq.ABYAD -> listOf(75 + 25.6 / 55.0 * al, 75 + 7.16 / 55.0 * al, 75 + 36.84 / 55.0 * al, 75 + 81.84 / 55.0 * al)
            Shafaq.GENERAL -> listOf(75 + 25.6 / 55.0 * al, 75 + 2.05 / 55.0 * al, 75 - 9.21 / 55.0 * al, 75 + 6.14 / 55.0 * al)
        }
        val adj = seasonAdjustment(a, b, c, d, daysSinceSolstice(doy, year, lat))
        return sunset + Math.round(adj * 60.0)
    }

    // ---- solar coordinates / solar time -----------------------------------

    private class SolarCoordinates(jd: Double) {
        val declination: Double
        val rightAscension: Double
        val apparentSiderealTime: Double
        init {
            val t = julianCentury(jd)
            val l0 = meanSolarLongitude(t)
            val lp = meanLunarLongitude(t)
            val omega = ascendingLunarNodeLongitude(t)
            val lambda = deg2rad(apparentSolarLongitude(t, l0))
            val theta0 = meanSiderealTime(t)
            val dPsi = nutationInLongitude(l0, lp, omega)
            val dEps = nutationInObliquity(l0, lp, omega)
            val eps0 = meanObliquityOfTheEcliptic(t)
            val epsApp = deg2rad(apparentObliquityOfTheEcliptic(t, eps0))
            declination = rad2deg(asin(sin(epsApp) * sin(lambda)))
            rightAscension = unwindAngle(rad2deg(atan2(cos(epsApp) * sin(lambda), cos(lambda))))
            apparentSiderealTime = theta0 + dPsi * 3600 * cos(deg2rad(eps0 + dEps)) / 3600
        }
    }

    private class SolarTime(date: CivilDate, val observer: Coordinates) {
        val solar = SolarCoordinates(julianDay(date.year, date.month, date.day))
        val prevSolar = SolarCoordinates(julianDay(date.year, date.month, date.day) - 1)
        val nextSolar = SolarCoordinates(julianDay(date.year, date.month, date.day) + 1)
        val approxTransit = approximateTransit(observer.longitude, solar.apparentSiderealTime, solar.rightAscension)
        val transit = correctedTransit(approxTransit, observer.longitude, solar.apparentSiderealTime, solar.rightAscension, prevSolar.rightAscension, nextSolar.rightAscension)
        val sunrise = hourAngle(-50.0 / 60.0, false)
        val sunset = hourAngle(-50.0 / 60.0, true)

        fun hourAngle(angle: Double, afterTransit: Boolean) = correctedHourAngle(
            approxTransit, angle, observer, afterTransit, solar.apparentSiderealTime, solar.rightAscension,
            prevSolar.rightAscension, nextSolar.rightAscension, solar.declination, prevSolar.declination, nextSolar.declination,
        )
        fun afternoon(shadowLength: Double): Double {
            val tangent = abs(observer.latitude - solar.declination)
            val inverse = shadowLength + tan(deg2rad(tangent))
            return hourAngle(rad2deg(atan(1.0 / inverse)), true)
        }
    }

    /** TimeComponents(num).utcDate(y, m, d): floor to h/m/s. NaN → null. */
    private fun utcDateFromHours(hours: Double, d: CivilDate): Long? {
        if (hours.isNaN()) return null
        val h = floor(hours)
        val m = floor((hours - h) * 60)
        val s = floor((hours - (h + m / 60)) * 60 * 60)
        return epochFromCivilUtc(d.year, d.month, d.day) + h.toLong() * 3600 + m.toLong() * 60 + s.toLong()
    }

    private fun roundedMinute(t: Long, r: Rounding): Long {
        val seconds = Math.floorMod(t, 60L)
        var offset = if (seconds >= 30) 60 - seconds else -seconds
        if (r == Rounding.UP) offset = 60 - seconds else if (r == Rounding.NONE) offset = 0
        return t + offset
    }

    // ---- prayer times -------------------------------------------------------

    fun compute(coords: Coordinates, date: CivilDate, p: CalculationParameters): PrayerTimes {
        val solarTime = SolarTime(date, coords)
        val tomorrow = addDays(date, 1)
        val tomorrowSolarTime = SolarTime(tomorrow, coords)

        val dhuhrOpt = utcDateFromHours(solarTime.transit, date)
        val sunriseOpt = utcDateFromHours(solarTime.sunrise, date)
        val sunsetOpt = utcDateFromHours(solarTime.sunset, date)
        val tomorrowSunriseOpt = utcDateFromHours(tomorrowSolarTime.sunrise, tomorrow)

        val dhuhrTime = dhuhrOpt ?: 0L
        val sunriseTime = sunriseOpt ?: 0L
        val sunsetTime = sunsetOpt ?: 0L
        val night: Double = if (sunriseOpt != null && sunsetOpt != null && tomorrowSunriseOpt != null)
            (tomorrowSunriseOpt - sunsetTime).toDouble() else Double.NaN

        val shadow = if (p.madhab == Madhab.HANAFI) 2.0 else 1.0
        val asrTime = utcDateFromHours(solarTime.afternoon(shadow), date) ?: dhuhrTime

        // Fajr
        var fajrRaw: Long? = utcDateFromHours(solarTime.hourAngle(-p.fajrAngle, false), date)
        if (p.method == Method.MOONSIGHTING_COMMITTEE && coords.latitude >= HIGH_LATITUDE_THRESHOLD) {
            fajrRaw = sunriseTime - ceil(night / 7).toLong()
        }
        val safeFajr = if (p.method == Method.MOONSIGHTING_COMMITTEE) {
            seasonAdjustedMorningTwilight(coords.latitude, dayOfYear(date), date.year, sunriseTime)
        } else {
            sunriseTime - ceil(p.nightPortions().first * night).toLong()
        }
        val fajr: Long = if (fajrRaw == null || safeFajr > fajrRaw) safeFajr else fajrRaw

        // Isha
        val isha: Long = if (p.ishaInterval > 0) {
            sunsetTime + p.ishaInterval * 60L
        } else {
            var ishaRaw: Long? = utcDateFromHours(solarTime.hourAngle(-p.ishaAngle, true), date)
            if (p.method == Method.MOONSIGHTING_COMMITTEE && coords.latitude >= HIGH_LATITUDE_THRESHOLD) {
                ishaRaw = sunsetTime + floor(night / 7).toLong()
            }
            val safeIsha = if (p.method == Method.MOONSIGHTING_COMMITTEE) {
                seasonAdjustedEveningTwilight(coords.latitude, dayOfYear(date), date.year, sunsetTime, p.shafaq)
            } else {
                sunsetTime + floor(p.nightPortions().second * night).toLong()
            }
            if (ishaRaw == null || safeIsha < ishaRaw) safeIsha else ishaRaw
        }

        // Maghrib
        var maghribTime = sunsetTime
        if (p.maghribAngle != 0.0) {
            val angleBased = utcDateFromHours(solarTime.hourAngle(-p.maghribAngle, true), date)
            if (angleBased != null && sunsetTime < angleBased && isha > angleBased) maghribTime = angleBased
        }

        fun adj(user: Int, method: Int) = (user + method) * 60L
        return PrayerTimes(
            fajr = roundedMinute(fajr + adj(p.adjustments.fajr, p.methodAdjustments.fajr), p.rounding),
            sunrise = roundedMinute(sunriseTime + adj(p.adjustments.sunrise, p.methodAdjustments.sunrise), p.rounding),
            dhuhr = roundedMinute(dhuhrTime + adj(p.adjustments.dhuhr, p.methodAdjustments.dhuhr), p.rounding),
            asr = roundedMinute(asrTime + adj(p.adjustments.asr, p.methodAdjustments.asr), p.rounding),
            sunset = roundedMinute(sunsetTime, p.rounding),
            maghrib = roundedMinute(maghribTime + adj(p.adjustments.maghrib, p.methodAdjustments.maghrib), p.rounding),
            isha = roundedMinute(isha + adj(p.adjustments.isha, p.methodAdjustments.isha), p.rounding),
        )
    }
}

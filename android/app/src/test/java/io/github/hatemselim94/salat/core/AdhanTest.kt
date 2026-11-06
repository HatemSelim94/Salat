package io.github.hatemselim94.salat.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** Compares the Kotlin port against vectors generated from @salat/core (adhan-js). */
class AdhanTest {
    @Test
    fun matchesCoreVectors() {
        val stream = javaClass.classLoader!!.getResourceAsStream("vectors.csv")!!
        val lines = stream.bufferedReader().readLines().drop(1).filter { it.isNotBlank() }
        var compared = 0
        for (line in lines) {
            val f = line.split(',')
            val coords = Coordinates(f[1].toDouble(), f[2].toDouble())
            val (y, m, d) = f[3].split('-').map { it.toInt() }
            val base = CalculationParameters.forMethod(Method.fromId(f[4]))
            val rule = when (f[6]) {
                "middleOfTheNight" -> HighLatitudeRule.MIDDLE_OF_THE_NIGHT
                "seventhOfTheNight" -> HighLatitudeRule.SEVENTH_OF_THE_NIGHT
                "twilightAngle" -> HighLatitudeRule.TWILIGHT_ANGLE
                else -> CalculationParameters.recommendedHighLatitudeRule(coords)
            }
            val params = base.copy(
                madhab = if (f[5] == "hanafi") Madhab.HANAFI else Madhab.SHAFI,
                highLatitudeRule = rule,
                adjustments = Adjustments(fajr = 2, isha = -3),
            )
            val t = Adhan.compute(coords, CivilDate(y, m, d), params)
            val got = longArrayOf(t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha)
            val names = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")
            for (i in 0 until 6) {
                assertEquals("${f[0]} ${f[3]} ${f[4]} ${f[6]} ${names[i]}", f[7 + i].toLong(), got[i])
                compared++
            }
        }
        assert(compared > 40_000) { "only $compared times compared" }
    }
}

// C++17 port of the adhan-js prayer-time algorithm (batoulapps/adhan-js, MIT).
// Dependency-free: dates are civil (year/month/day) in, UTC epoch seconds out.
// Verified against @salat/core's output in tests/adhan_test.cpp.
#pragma once

#include <cstdint>
#include <optional>
#include <string>

namespace adhan {

struct Coordinates {
    double latitude = 0;
    double longitude = 0;
};

struct CivilDate {
    int year = 1970;
    int month = 1;  // 1..12
    int day = 1;    // 1..31
};

enum class Madhab { Shafi, Hanafi };
enum class HighLatitudeRule { MiddleOfTheNight, SeventhOfTheNight, TwilightAngle };
enum class Rounding { Nearest, Up, None };
enum class Shafaq { General, Ahmer, Abyad };

enum class Method {
    MuslimWorldLeague,
    Egyptian,
    Karachi,
    UmmAlQura,
    Dubai,
    MoonsightingCommittee,
    NorthAmerica,
    Kuwait,
    Qatar,
    Singapore,
    Tehran,
    Turkey,
    Other,
};

struct Adjustments {
    int fajr = 0, sunrise = 0, dhuhr = 0, asr = 0, maghrib = 0, isha = 0;
};

struct CalculationParameters {
    Method method = Method::Other;
    double fajrAngle = 0;
    double ishaAngle = 0;
    int ishaInterval = 0;      // minutes after maghrib; >0 overrides ishaAngle
    double maghribAngle = 0;   // Tehran only
    Madhab madhab = Madhab::Shafi;
    HighLatitudeRule highLatitudeRule = HighLatitudeRule::MiddleOfTheNight;
    Adjustments adjustments;        // user
    Adjustments methodAdjustments;  // set by the method preset
    Rounding rounding = Rounding::Nearest;
    Shafaq shafaq = Shafaq::General;

    struct NightPortions { double fajr, isha; };
    NightPortions nightPortions() const;
};

/// Preset parameters for a method (mirrors adhan-js CalculationMethod).
CalculationParameters parametersFor(Method method);

/// HighLatitudeRule.recommended(): SeventhOfTheNight above 48° N/S, else MiddleOfTheNight.
HighLatitudeRule recommendedHighLatitudeRule(const Coordinates& c);

/// Method <-> string ids as used by @salat/core ("MuslimWorldLeague", …).
std::optional<Method> methodFromId(const std::string& id);
const char* methodId(Method m);

struct PrayerTimes {
    // UTC epoch seconds, rounded per `rounding`.
    int64_t fajr = 0, sunrise = 0, dhuhr = 0, asr = 0, sunset = 0, maghrib = 0, isha = 0;
};

/// Prayer times for the civil day `date` at `coords`.
PrayerTimes computePrayerTimes(const Coordinates& coords, const CivilDate& date, const CalculationParameters& params);

/// Days-from-civil helpers (proleptic Gregorian, UTC).
int64_t epochFromCivilUtc(int year, int month, int day, int hour = 0, int minute = 0, int second = 0);
CivilDate civilFromEpochUtc(int64_t epoch);
CivilDate addDays(const CivilDate& d, int days);

}  // namespace adhan

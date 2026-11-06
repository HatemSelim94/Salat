#include "adhan.h"

#include <cmath>
#include <limits>

namespace adhan {
namespace {

constexpr double kPi = 3.14159265358979323846;
constexpr double kNaN = std::numeric_limits<double>::quiet_NaN();

double deg2rad(double d) { return d * kPi / 180.0; }
double rad2deg(double r) { return r * 180.0 / kPi; }
double normalizeToScale(double num, double max) { return num - max * std::floor(num / max); }
double unwindAngle(double a) { return normalizeToScale(a, 360.0); }
double quadrantShiftAngle(double a) {
    if (a >= -180 && a <= 180) return a;
    return a - 360 * std::round(a / 360);
}

bool isLeapYear(int y) {
    if (y % 4 != 0) return false;
    if (y % 100 == 0 && y % 400 != 0) return false;
    return true;
}

int dayOfYear(const CivilDate& d) {
    const int feb = isLeapYear(d.year) ? 29 : 28;
    const int months[12] = {31, feb, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    int n = 0;
    for (int i = 0; i < d.month - 1; ++i) n += months[i];
    return n + d.day;
}

// ---- Astronomical (Meeus, "Astronomical Algorithms") ------------------------

double julianDay(int year, int month, int day, double hours = 0) {
    const double Y = month > 2 ? year : year - 1;
    const double M = month > 2 ? month : month + 12;
    const double D = day + hours / 24.0;
    const double A = std::trunc(Y / 100);
    const double B = std::trunc(2 - A + std::trunc(A / 4));
    const double i0 = std::trunc(365.25 * (Y + 4716));
    const double i1 = std::trunc(30.6001 * (M + 1));
    return i0 + i1 + D + B - 1524.5;
}

double julianCentury(double jd) { return (jd - 2451545.0) / 36525; }

double meanSolarLongitude(double T) { return unwindAngle(280.4664567 + 36000.76983 * T + 0.0003032 * T * T); }
double meanLunarLongitude(double T) { return unwindAngle(218.3165 + 481267.8813 * T); }
double ascendingLunarNodeLongitude(double T) {
    return unwindAngle(125.04452 - 1934.136261 * T + 0.0020708 * T * T + T * T * T / 450000);
}
double meanSolarAnomaly(double T) { return unwindAngle(357.52911 + 35999.05029 * T - 0.0001537 * T * T); }
double solarEquationOfTheCenter(double T, double M) {
    const double Mrad = deg2rad(M);
    return (1.914602 - 0.004817 * T - 0.000014 * T * T) * std::sin(Mrad) + (0.019993 - 0.000101 * T) * std::sin(2 * Mrad) +
           0.000289 * std::sin(3 * Mrad);
}
double apparentSolarLongitude(double T, double L0) {
    const double longitude = L0 + solarEquationOfTheCenter(T, meanSolarAnomaly(T));
    const double Omega = 125.04 - 1934.136 * T;
    return unwindAngle(longitude - 0.00569 - 0.00478 * std::sin(deg2rad(Omega)));
}
double meanObliquityOfTheEcliptic(double T) {
    return 23.439291 - 0.013004167 * T - 0.0000001639 * T * T + 0.0000005036 * T * T * T;
}
double apparentObliquityOfTheEcliptic(double T, double eps0) { return eps0 + 0.00256 * std::cos(deg2rad(125.04 - 1934.136 * T)); }
double meanSiderealTime(double T) {
    const double JD = T * 36525 + 2451545.0;
    return unwindAngle(280.46061837 + 360.98564736629 * (JD - 2451545) + 0.000387933 * T * T - T * T * T / 38710000);
}
double nutationInLongitude(double L0, double Lp, double Omega) {
    return -17.2 / 3600 * std::sin(deg2rad(Omega)) - 1.32 / 3600 * std::sin(2 * deg2rad(L0)) -
           0.23 / 3600 * std::sin(2 * deg2rad(Lp)) + 0.21 / 3600 * std::sin(2 * deg2rad(Omega));
}
double nutationInObliquity(double L0, double Lp, double Omega) {
    return 9.2 / 3600 * std::cos(deg2rad(Omega)) + 0.57 / 3600 * std::cos(2 * deg2rad(L0)) +
           0.1 / 3600 * std::cos(2 * deg2rad(Lp)) - 0.09 / 3600 * std::cos(2 * deg2rad(Omega));
}
double altitudeOfCelestialBody(double phi, double delta, double H) {
    return rad2deg(std::asin(std::sin(deg2rad(phi)) * std::sin(deg2rad(delta)) +
                             std::cos(deg2rad(phi)) * std::cos(deg2rad(delta)) * std::cos(deg2rad(H))));
}
double interpolate(double y2, double y1, double y3, double n) {
    const double a = y2 - y1, b = y3 - y2, c = b - a;
    return y2 + n / 2 * (a + b + n * c);
}
double interpolateAngles(double y2, double y1, double y3, double n) {
    const double a = unwindAngle(y2 - y1), b = unwindAngle(y3 - y2), c = b - a;
    return y2 + n / 2 * (a + b + n * c);
}
double approximateTransit(double L, double Theta0, double a2) {
    const double Lw = -L;
    const double m0 = normalizeToScale((a2 + Lw - Theta0) / 360, 1);
    const double expected = normalizeToScale((12.0 - L / 15.0) / 24.0, 1);
    if (m0 - expected > 0.5) return m0 - 1.0;
    if (expected - m0 > 0.5) return m0 + 1.0;
    return m0;
}
double correctedTransit(double m0, double L, double Theta0, double a2, double a1, double a3) {
    const double Lw = -L;
    const double Theta = unwindAngle(Theta0 + 360.985647 * m0);
    const double a = unwindAngle(interpolateAngles(a2, a1, a3, m0));
    const double H = quadrantShiftAngle(Theta - Lw - a);
    return (m0 + H / -360) * 24;
}
double correctedHourAngle(double m0, double h0, const Coordinates& c, bool afterTransit, double Theta0, double a2, double a1,
                          double a3, double d2, double d1, double d3) {
    const double Lw = -c.longitude;
    const double term1 = std::sin(deg2rad(h0)) - std::sin(deg2rad(c.latitude)) * std::sin(deg2rad(d2));
    const double term2 = std::cos(deg2rad(c.latitude)) * std::cos(deg2rad(d2));
    const double H0 = rad2deg(std::acos(term1 / term2));  // NaN when the sun never reaches h0
    const double m = afterTransit ? m0 + H0 / 360 : m0 - H0 / 360;
    const double Theta = unwindAngle(Theta0 + 360.985647 * m);
    const double a = unwindAngle(interpolateAngles(a2, a1, a3, m));
    const double delta = interpolate(d2, d1, d3, m);
    const double H = Theta - Lw - a;
    const double h = altitudeOfCelestialBody(c.latitude, delta, H);
    const double dm = (h - h0) / (360 * std::cos(deg2rad(delta)) * std::cos(deg2rad(c.latitude)) * std::sin(deg2rad(H)));
    return (m + dm) * 24;
}

int daysSinceSolstice(int doy, int year, double latitude) {
    const int daysInYear = isLeapYear(year) ? 366 : 365;
    int d;
    if (latitude >= 0) {
        d = doy + 10;
        if (d >= daysInYear) d -= daysInYear;
    } else {
        d = doy - (isLeapYear(year) ? 173 : 172);
        if (d < 0) d += daysInYear;
    }
    return d;
}

double seasonAdjustment(double a, double b, double c, double d, int dyy) {
    if (dyy < 91) return a + (b - a) / 91.0 * dyy;
    if (dyy < 137) return b + (c - b) / 46.0 * (dyy - 91);
    if (dyy < 183) return c + (d - c) / 46.0 * (dyy - 137);
    if (dyy < 229) return d + (c - d) / 46.0 * (dyy - 183);
    if (dyy < 275) return c + (b - c) / 46.0 * (dyy - 229);
    return b + (a - b) / 91.0 * (dyy - 275);
}

int64_t seasonAdjustedMorningTwilight(double lat, int doy, int year, int64_t sunrise) {
    const double al = std::abs(lat);
    const double adj = seasonAdjustment(75 + 28.65 / 55.0 * al, 75 + 19.44 / 55.0 * al, 75 + 32.74 / 55.0 * al,
                                        75 + 48.1 / 55.0 * al, daysSinceSolstice(doy, year, lat));
    return sunrise + static_cast<int64_t>(std::round(adj * -60.0));
}

int64_t seasonAdjustedEveningTwilight(double lat, int doy, int year, int64_t sunset, Shafaq shafaq) {
    const double al = std::abs(lat);
    double a, b, c, d;
    if (shafaq == Shafaq::Ahmer) {
        a = 62 + 17.4 / 55.0 * al; b = 62 - 7.16 / 55.0 * al; c = 62 + 5.12 / 55.0 * al; d = 62 + 19.44 / 55.0 * al;
    } else if (shafaq == Shafaq::Abyad) {
        a = 75 + 25.6 / 55.0 * al; b = 75 + 7.16 / 55.0 * al; c = 75 + 36.84 / 55.0 * al; d = 75 + 81.84 / 55.0 * al;
    } else {
        a = 75 + 25.6 / 55.0 * al; b = 75 + 2.05 / 55.0 * al; c = 75 - 9.21 / 55.0 * al; d = 75 + 6.14 / 55.0 * al;
    }
    const double adj = seasonAdjustment(a, b, c, d, daysSinceSolstice(doy, year, lat));
    return sunset + static_cast<int64_t>(std::round(adj * 60.0));
}

// ---- Solar coordinates / solar time ---------------------------------------

struct SolarCoordinates {
    double declination, rightAscension, apparentSiderealTime;
    explicit SolarCoordinates(double jd) {
        const double T = julianCentury(jd);
        const double L0 = meanSolarLongitude(T);
        const double Lp = meanLunarLongitude(T);
        const double Omega = ascendingLunarNodeLongitude(T);
        const double Lambda = deg2rad(apparentSolarLongitude(T, L0));
        const double Theta0 = meanSiderealTime(T);
        const double dPsi = nutationInLongitude(L0, Lp, Omega);
        const double dEps = nutationInObliquity(L0, Lp, Omega);
        const double Eps0 = meanObliquityOfTheEcliptic(T);
        const double EpsApp = deg2rad(apparentObliquityOfTheEcliptic(T, Eps0));
        declination = rad2deg(std::asin(std::sin(EpsApp) * std::sin(Lambda)));
        rightAscension = unwindAngle(rad2deg(std::atan2(std::cos(EpsApp) * std::sin(Lambda), std::cos(Lambda))));
        apparentSiderealTime = Theta0 + dPsi * 3600 * std::cos(deg2rad(Eps0 + dEps)) / 3600;
    }
};

struct SolarTime {
    Coordinates observer;
    SolarCoordinates solar, prevSolar, nextSolar;
    double approxTransit, transit, sunrise, sunset;

    SolarTime(const CivilDate& date, const Coordinates& c)
        : observer(c),
          solar(julianDay(date.year, date.month, date.day)),
          prevSolar(julianDay(date.year, date.month, date.day) - 1),
          nextSolar(julianDay(date.year, date.month, date.day) + 1) {
        const double m0 = approximateTransit(c.longitude, solar.apparentSiderealTime, solar.rightAscension);
        const double solarAltitude = -50.0 / 60.0;
        approxTransit = m0;
        transit = correctedTransit(m0, c.longitude, solar.apparentSiderealTime, solar.rightAscension, prevSolar.rightAscension,
                                   nextSolar.rightAscension);
        sunrise = hourAngle(solarAltitude, false);
        sunset = hourAngle(solarAltitude, true);
    }
    double hourAngle(double angle, bool afterTransit) const {
        return correctedHourAngle(approxTransit, angle, observer, afterTransit, solar.apparentSiderealTime, solar.rightAscension,
                                  prevSolar.rightAscension, nextSolar.rightAscension, solar.declination, prevSolar.declination,
                                  nextSolar.declination);
    }
    double afternoon(double shadowLength) const {
        const double tangent = std::abs(observer.latitude - solar.declination);
        const double inverse = shadowLength + std::tan(deg2rad(tangent));
        return hourAngle(rad2deg(std::atan(1.0 / inverse)), true);
    }
};

// TimeComponents(num).utcDate(y, m, d): floor to h/m/s then build a UTC instant. NaN → nullopt.
std::optional<int64_t> utcDateFromHours(double hours, const CivilDate& d) {
    if (std::isnan(hours)) return std::nullopt;
    const double h = std::floor(hours);
    const double m = std::floor((hours - h) * 60);
    const double s = std::floor((hours - (h + m / 60)) * 60 * 60);
    return epochFromCivilUtc(d.year, d.month, d.day) + static_cast<int64_t>(h) * 3600 + static_cast<int64_t>(m) * 60 +
           static_cast<int64_t>(s);
}

int64_t roundedMinute(int64_t t, Rounding r) {
    const int64_t seconds = ((t % 60) + 60) % 60;
    int64_t offset = seconds >= 30 ? 60 - seconds : -seconds;
    if (r == Rounding::Up) offset = 60 - seconds;
    else if (r == Rounding::None) offset = 0;
    return t + offset;
}

}  // namespace

// ---- public helpers ---------------------------------------------------------

int64_t epochFromCivilUtc(int y, int m, int d, int hour, int minute, int second) {
    // Howard Hinnant's days_from_civil.
    y -= m <= 2;
    const int era = (y >= 0 ? y : y - 399) / 400;
    const unsigned yoe = static_cast<unsigned>(y - era * 400);
    const unsigned doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
    const unsigned doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
    const int64_t days = era * 146097LL + static_cast<int64_t>(doe) - 719468;
    return days * 86400 + hour * 3600 + minute * 60 + second;
}

CivilDate civilFromEpochUtc(int64_t epoch) {
    int64_t z = epoch / 86400;
    if (epoch % 86400 < 0) --z;
    z += 719468;
    const int64_t era = (z >= 0 ? z : z - 146096) / 146097;
    const unsigned doe = static_cast<unsigned>(z - era * 146097);
    const unsigned yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
    const int64_t y = static_cast<int64_t>(yoe) + era * 400;
    const unsigned doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    const unsigned mp = (5 * doy + 2) / 153;
    const unsigned d = doy - (153 * mp + 2) / 5 + 1;
    const unsigned m = mp + (mp < 10 ? 3 : -9);
    return CivilDate{static_cast<int>(y + (m <= 2)), static_cast<int>(m), static_cast<int>(d)};
}

CivilDate addDays(const CivilDate& d, int days) {
    return civilFromEpochUtc(epochFromCivilUtc(d.year, d.month, d.day) + static_cast<int64_t>(days) * 86400);
}

CalculationParameters::NightPortions CalculationParameters::nightPortions() const {
    switch (highLatitudeRule) {
        case HighLatitudeRule::MiddleOfTheNight: return {1.0 / 2, 1.0 / 2};
        case HighLatitudeRule::SeventhOfTheNight: return {1.0 / 7, 1.0 / 7};
        case HighLatitudeRule::TwilightAngle: return {fajrAngle / 60, ishaAngle / 60};
    }
    return {1.0 / 2, 1.0 / 2};
}

HighLatitudeRule recommendedHighLatitudeRule(const Coordinates& c) {
    return c.latitude > 48 ? HighLatitudeRule::SeventhOfTheNight : HighLatitudeRule::MiddleOfTheNight;
}

CalculationParameters parametersFor(Method method) {
    CalculationParameters p;
    p.method = method;
    switch (method) {
        case Method::MuslimWorldLeague: p.fajrAngle = 18; p.ishaAngle = 17; p.methodAdjustments.dhuhr = 1; break;
        case Method::Egyptian: p.fajrAngle = 19.5; p.ishaAngle = 17.5; p.methodAdjustments.dhuhr = 1; break;
        case Method::Karachi: p.fajrAngle = 18; p.ishaAngle = 18; p.methodAdjustments.dhuhr = 1; break;
        case Method::UmmAlQura: p.fajrAngle = 18.5; p.ishaInterval = 90; break;
        case Method::Dubai:
            p.fajrAngle = 18.2; p.ishaAngle = 18.2;
            p.methodAdjustments.sunrise = -3; p.methodAdjustments.dhuhr = 3; p.methodAdjustments.asr = 3; p.methodAdjustments.maghrib = 3;
            break;
        case Method::MoonsightingCommittee:
            p.fajrAngle = 18; p.ishaAngle = 18; p.methodAdjustments.dhuhr = 5; p.methodAdjustments.maghrib = 3;
            break;
        case Method::NorthAmerica: p.fajrAngle = 15; p.ishaAngle = 15; p.methodAdjustments.dhuhr = 1; break;
        case Method::Kuwait: p.fajrAngle = 18; p.ishaAngle = 17.5; break;
        case Method::Qatar: p.fajrAngle = 18; p.ishaInterval = 90; break;
        case Method::Singapore: p.fajrAngle = 20; p.ishaAngle = 18; p.methodAdjustments.dhuhr = 1; p.rounding = Rounding::Up; break;
        case Method::Tehran: p.fajrAngle = 17.7; p.ishaAngle = 14; p.maghribAngle = 4.5; break;
        case Method::Turkey:
            p.fajrAngle = 18; p.ishaAngle = 17;
            p.methodAdjustments.sunrise = -7; p.methodAdjustments.dhuhr = 5; p.methodAdjustments.asr = 4; p.methodAdjustments.maghrib = 7;
            break;
        case Method::Other: break;
    }
    return p;
}

namespace {
struct MethodName { Method m; const char* id; };
constexpr MethodName kMethodNames[] = {
    {Method::MuslimWorldLeague, "MuslimWorldLeague"}, {Method::Egyptian, "Egyptian"}, {Method::Karachi, "Karachi"},
    {Method::UmmAlQura, "UmmAlQura"}, {Method::Dubai, "Dubai"}, {Method::MoonsightingCommittee, "MoonsightingCommittee"},
    {Method::NorthAmerica, "NorthAmerica"}, {Method::Kuwait, "Kuwait"}, {Method::Qatar, "Qatar"},
    {Method::Singapore, "Singapore"}, {Method::Tehran, "Tehran"}, {Method::Turkey, "Turkey"}, {Method::Other, "Other"},
};
}  // namespace

std::optional<Method> methodFromId(const std::string& id) {
    for (const auto& n : kMethodNames) if (id == n.id) return n.m;
    return std::nullopt;
}
const char* methodId(Method m) {
    for (const auto& n : kMethodNames) if (n.m == m) return n.id;
    return "Other";
}

// ---- prayer times -----------------------------------------------------------

PrayerTimes computePrayerTimes(const Coordinates& coords, const CivilDate& date, const CalculationParameters& p) {
    constexpr double kHighLatitudeThreshold = 55;
    const SolarTime solarTime(date, coords);
    const CivilDate tomorrow = addDays(date, 1);
    const SolarTime tomorrowSolarTime(tomorrow, coords);

    // Polar-circle resolution is not supported; inside the polar circle the
    // safe fajr/isha fallbacks below still produce times when sunrise/sunset exist.
    const auto dhuhrOpt = utcDateFromHours(solarTime.transit, date);
    const auto sunriseOpt = utcDateFromHours(solarTime.sunrise, date);
    const auto sunsetOpt = utcDateFromHours(solarTime.sunset, date);
    const auto tomorrowSunriseOpt = utcDateFromHours(tomorrowSolarTime.sunrise, tomorrow);

    const int64_t dhuhrTime = dhuhrOpt.value_or(0);
    const int64_t sunriseTime = sunriseOpt.value_or(0);
    const int64_t sunsetTime = sunsetOpt.value_or(0);
    const double night = (sunriseOpt && sunsetOpt && tomorrowSunriseOpt)
                             ? static_cast<double>(*tomorrowSunriseOpt - sunsetTime)
                             : kNaN;

    const double shadow = p.madhab == Madhab::Hanafi ? 2 : 1;
    const int64_t asrTime = utcDateFromHours(solarTime.afternoon(shadow), date).value_or(dhuhrTime);

    // Fajr
    std::optional<int64_t> fajr = utcDateFromHours(solarTime.hourAngle(-p.fajrAngle, false), date);
    if (p.method == Method::MoonsightingCommittee && coords.latitude >= kHighLatitudeThreshold) {
        fajr = sunriseTime - static_cast<int64_t>(std::ceil(night / 7));
    }
    {
        int64_t safeFajr;
        if (p.method == Method::MoonsightingCommittee) {
            safeFajr = seasonAdjustedMorningTwilight(coords.latitude, dayOfYear(date), date.year, sunriseTime);
        } else {
            safeFajr = sunriseTime - static_cast<int64_t>(std::ceil(p.nightPortions().fajr * night));
        }
        if (!fajr || safeFajr > *fajr) fajr = safeFajr;
    }

    // Isha
    std::optional<int64_t> isha;
    if (p.ishaInterval > 0) {
        isha = sunsetTime + static_cast<int64_t>(p.ishaInterval) * 60;
    } else {
        isha = utcDateFromHours(solarTime.hourAngle(-p.ishaAngle, true), date);
        if (p.method == Method::MoonsightingCommittee && coords.latitude >= kHighLatitudeThreshold) {
            isha = sunsetTime + static_cast<int64_t>(std::floor(night / 7));
        }
        int64_t safeIsha;
        if (p.method == Method::MoonsightingCommittee) {
            safeIsha = seasonAdjustedEveningTwilight(coords.latitude, dayOfYear(date), date.year, sunsetTime, p.shafaq);
        } else {
            safeIsha = sunsetTime + static_cast<int64_t>(std::floor(p.nightPortions().isha * night));
        }
        if (!isha || safeIsha < *isha) isha = safeIsha;
    }

    // Maghrib
    int64_t maghribTime = sunsetTime;
    if (p.maghribAngle != 0) {
        const auto angleBased = utcDateFromHours(solarTime.hourAngle(-p.maghribAngle, true), date);
        if (angleBased && sunsetTime < *angleBased && *isha > *angleBased) maghribTime = *angleBased;
    }

    const auto adj = [&](int user, int method) { return static_cast<int64_t>(user + method) * 60; };
    PrayerTimes t;
    t.fajr = roundedMinute(*fajr + adj(p.adjustments.fajr, p.methodAdjustments.fajr), p.rounding);
    t.sunrise = roundedMinute(sunriseTime + adj(p.adjustments.sunrise, p.methodAdjustments.sunrise), p.rounding);
    t.dhuhr = roundedMinute(dhuhrTime + adj(p.adjustments.dhuhr, p.methodAdjustments.dhuhr), p.rounding);
    t.asr = roundedMinute(asrTime + adj(p.adjustments.asr, p.methodAdjustments.asr), p.rounding);
    t.sunset = roundedMinute(sunsetTime, p.rounding);
    t.maghrib = roundedMinute(maghribTime + adj(p.adjustments.maghrib, p.methodAdjustments.maghrib), p.rounding);
    t.isha = roundedMinute(*isha + adj(p.adjustments.isha, p.methodAdjustments.isha), p.rounding);
    return t;
}

}  // namespace adhan

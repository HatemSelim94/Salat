// Compares the C++ port against vectors generated from @salat/core (adhan-js).
// Usage: adhan_test path/to/vectors.csv
#include <cstdio>
#include <cstdlib>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>

#include "../src/adhan/adhan.h"

using namespace adhan;

static std::vector<std::string> split(const std::string& s, char sep) {
    std::vector<std::string> out;
    std::string cur;
    std::istringstream in(s);
    while (std::getline(in, cur, sep)) out.push_back(cur);
    return out;
}

int main(int argc, char** argv) {
    if (argc < 2) { std::fprintf(stderr, "usage: adhan_test vectors.csv\n"); return 2; }
    std::ifstream in(argv[1]);
    if (!in) { std::fprintf(stderr, "cannot open %s\n", argv[1]); return 2; }

    std::string line;
    std::getline(in, line);  // header
    long total = 0, exact = 0, within60 = 0, bad = 0;
    long maxDiff = 0;
    std::string worst;

    while (std::getline(in, line)) {
        if (line.empty()) continue;
        const auto f = split(line, ',');
        if (f.size() != 13) { std::fprintf(stderr, "bad line: %s\n", line.c_str()); return 2; }
        Coordinates c{std::stod(f[1]), std::stod(f[2])};
        const auto dp = split(f[3], '-');
        CivilDate d{std::stoi(dp[0]), std::stoi(dp[1]), std::stoi(dp[2])};
        auto params = parametersFor(*methodFromId(f[4]));
        params.madhab = f[5] == "hanafi" ? Madhab::Hanafi : Madhab::Shafi;
        const std::string& rule = f[6];
        params.highLatitudeRule = rule == "middleOfTheNight"  ? HighLatitudeRule::MiddleOfTheNight
                                : rule == "seventhOfTheNight" ? HighLatitudeRule::SeventhOfTheNight
                                : rule == "twilightAngle"     ? HighLatitudeRule::TwilightAngle
                                                              : recommendedHighLatitudeRule(c);
        params.adjustments.fajr = 2;
        params.adjustments.isha = -3;

        const PrayerTimes t = computePrayerTimes(c, d, params);
        const long long expected[6] = {std::stoll(f[7]), std::stoll(f[8]), std::stoll(f[9]), std::stoll(f[10]), std::stoll(f[11]), std::stoll(f[12])};
        const long long got[6] = {t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha};
        static const char* names[6] = {"fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"};
        for (int i = 0; i < 6; ++i) {
            ++total;
            const long long diff = std::llabs(got[i] - expected[i]);
            if (diff == 0) ++exact;
            else if (diff <= 60) ++within60;
            else {
                ++bad;
                if (bad <= 10) std::printf("MISMATCH %s %s %s %s %s: got %lld expected %lld (diff %lld s)\n", f[0].c_str(), f[3].c_str(), f[4].c_str(), f[6].c_str(), names[i], got[i], expected[i], got[i] - expected[i]);
            }
            if (diff > maxDiff) { maxDiff = diff; worst = f[0] + " " + f[3] + " " + f[4] + " " + names[i]; }
        }
    }
    std::printf("%ld times compared: %ld exact, %ld within 60 s, %ld worse. max diff %ld s (%s)\n", total, exact, within60, bad, maxDiff, worst.c_str());
    return bad == 0 ? 0 : 1;
}

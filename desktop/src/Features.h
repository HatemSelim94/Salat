#pragma once

#include <QDate>
#include <QJsonObject>
#include <QMap>
#include <QString>
#include <QStringList>

#include <optional>

class Settings;

/// Mirrors core/src/features.ts: Qibla, Hijri, Jumu'ah, quiet hours, month table, timetable import.
namespace Features {

double qiblaBearing(double latitude, double longitude);
double distanceToKaabaKm(double latitude, double longitude);

/// Tabular Islamic calendar (Qt has no Umm al-Qura); month 9 = Ramadan.
struct Hijri { int year, month, day; };
std::optional<Hijri> hijri(const QDate& local);
bool isRamadan(const Settings& s, const QDate& local);
inline bool isFriday(const QDate& local) { return local.dayOfWeek() == Qt::Friday; }

/// Minutes since midnight for "HH:MM"; nullopt when malformed.
std::optional<int> minutesOfDay(const QString& hhmm);
bool inQuietHours(const Settings& s, qint64 epoch);

/// Imported mosque timetable: ISO date → { prayer key → "HH:MM" }.
using Timetable = QMap<QString, QMap<QString, QString>>;
struct ImportResult { Timetable entries; QStringList errors; int days() const { return entries.size(); } };
ImportResult parseTimetableCsv(const QString& text);
QJsonObject timetableToJson(const Timetable& t);
Timetable timetableFromJson(const QJsonObject& o);

}  // namespace Features

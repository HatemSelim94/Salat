#include "Cities.h"
#include <algorithm>

#include <QFile>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QVariantMap>

namespace {
QString normalise(const QString& s) {
    QString out;
    for (const QChar c : s.normalized(QString::NormalizationForm_D)) {
        if (c.category() == QChar::Mark_NonSpacing) continue;
        out += c.toLower();
    }
    return out.trimmed();
}
}  // namespace

Cities::Cities(QObject* parent) : QObject(parent) {
    QFile f(QStringLiteral(":/Salat/resources/cities.json"));
    if (!f.open(QIODevice::ReadOnly)) return;
    for (const auto& v : QJsonDocument::fromJson(f.readAll()).array()) {
        const auto o = v.toObject();
        const QString name = o.value("name").toString();
        // Searchable forms: the name plus any alternative spellings ("Köln|Koeln").
        QStringList norms{normalise(name)};
        for (const QString& a : o.value("alt").toString().split(QLatin1Char('|'), Qt::SkipEmptyParts)) norms.push_back(normalise(a));
        m_cities.push_back({name, o.value("country").toString(), o.value("tz").toString(), norms, o.value("lat").toDouble(), o.value("lng").toDouble()});
    }
}

QVariantList Cities::search(const QString& query, int limit) const {
    const QString q = normalise(query);
    QVariantList starts, contains;
    if (q.isEmpty()) return {};
    for (const auto& c : m_cities) {
        QVariantMap row{{"name", c.name}, {"country", c.country}, {"lat", c.lat}, {"lng", c.lng}, {"tz", c.tz}};
        const bool st = std::any_of(c.norms.begin(), c.norms.end(), [&](const QString& n) { return n.startsWith(q); });
        const bool ct = !st && std::any_of(c.norms.begin(), c.norms.end(), [&](const QString& n) { return n.contains(q); });
        if (st) starts.push_back(row);
        else if (ct || c.country.toLower() == q) contains.push_back(row);
    }
    QVariantList out = starts + contains;
    while (out.size() > limit) out.removeLast();
    return out;
}

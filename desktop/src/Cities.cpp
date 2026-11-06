#include "Cities.h"

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
        m_cities.push_back({name, o.value("country").toString(), o.value("tz").toString(), normalise(name), o.value("lat").toDouble(), o.value("lng").toDouble()});
    }
}

QVariantList Cities::search(const QString& query, int limit) const {
    const QString q = normalise(query);
    QVariantList starts, contains;
    if (q.isEmpty()) return {};
    for (const auto& c : m_cities) {
        QVariantMap row{{"name", c.name}, {"country", c.country}, {"lat", c.lat}, {"lng", c.lng}, {"tz", c.tz}};
        if (c.norm.startsWith(q)) starts.push_back(row);
        else if (c.norm.contains(q) || c.country.toLower() == q) contains.push_back(row);
    }
    QVariantList out = starts + contains;
    while (out.size() > limit) out.removeLast();
    return out;
}

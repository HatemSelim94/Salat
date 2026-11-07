#pragma once

#include <QObject>
#include <QStringList>
#include <QVariantList>
#include <qqml.h>

#include <vector>

/// Bundled city list (exported from core/src/cities.ts by tools/export_cities.mjs).
class Cities : public QObject {
    Q_OBJECT
    QML_ELEMENT
    QML_SINGLETON
public:
    explicit Cities(QObject* parent = nullptr);
    /// Case/diacritic-insensitive search; [{ name, country, lat, lng, tz }].
    Q_INVOKABLE QVariantList search(const QString& query, int limit = 8) const;

private:
    struct City { QString name, country, tz; QStringList norms; double lat, lng; };
    std::vector<City> m_cities;
};

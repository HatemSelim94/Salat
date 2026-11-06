#include "Tr.h"

#include <QFile>
#include <QJsonDocument>
#include <QJsonObject>
#include <QLocale>

Tr& Tr::instance() {
    static Tr tr;
    return tr;
}

Tr::Tr() {
    QFile f(QStringLiteral(":/Salat/resources/translations.json"));
    if (f.open(QIODevice::ReadOnly)) {
        const QJsonObject root = QJsonDocument::fromJson(f.readAll()).object();
        for (auto it = root.begin(); it != root.end(); ++it) {
            if (it.key().startsWith('_')) continue;
            const QJsonObject langs = it.value().toObject();
            QHash<QString, QString> row;
            for (auto l = langs.begin(); l != langs.end(); ++l) row.insert(l.key(), l.value().toString());
            m_table.insert(it.key(), row);
        }
    }
    setPreference(QStringLiteral("auto"));
}

void Tr::setPreference(const QString& pref) {
    QString lang = pref;
    if (pref == "auto" || pref.isEmpty()) {
        const QString sys = QLocale::system().name().section('_', 0, 0).toLower();  // "de_DE" → "de"
        lang = (sys == "ar" || sys == "de") ? sys : QStringLiteral("en");
    }
    if (lang != "en" && lang != "ar" && lang != "de") lang = QStringLiteral("en");
    if (lang == m_lang && m_rev > 0) return;
    m_lang = lang;
    ++m_rev;
    emit changed();
}

QString Tr::t(const QString& key, int) const {
    const auto row = m_table.find(key);
    if (row == m_table.end()) return key;
    const QString s = row->value(m_lang);
    return s.isEmpty() ? row->value(QStringLiteral("en"), key) : s;
}

QString Tr::f(const QString& key, const QVariantMap& params, int) const {
    QString s = t(key);
    for (auto it = params.begin(); it != params.end(); ++it) s.replace('{' + it.key() + '}', it.value().toString());
    return s;
}

QLocale Tr::locale() const {
    if (m_lang == "ar") return QLocale(QLocale::Arabic, QLocale::Egypt);
    if (m_lang == "de") return QLocale(QLocale::German, QLocale::Germany);
    return QLocale(QLocale::English, QLocale::UnitedKingdom);
}

#pragma once

#include <QHash>
#include <QObject>
#include <QString>
#include <QVariantMap>

/// Runtime translations from resources/translations.json (generated from
/// i18n/strings.json). Used instead of Qt's .ts/.qm chain so all four apps
/// share one string table. QML: `Tr.t("home.nextPrayer", Tr.rev)` — the
/// second argument makes the binding re-evaluate when the language changes.
class Tr : public QObject {
    Q_OBJECT
    Q_PROPERTY(QString language READ language NOTIFY changed)
    Q_PROPERTY(bool rtl READ rtl NOTIFY changed)
    Q_PROPERTY(int rev READ rev NOTIFY changed)

public:
    static Tr& instance();

    /// "auto" | "en" | "ar" | "de" → resolved to en/ar/de using the system locale for auto.
    void setPreference(const QString& pref);
    QString language() const { return m_lang; }
    bool rtl() const { return m_lang == "ar"; }
    int rev() const { return m_rev; }

    Q_INVOKABLE QString t(const QString& key, int = 0) const;
    Q_INVOKABLE QString f(const QString& key, const QVariantMap& params, int = 0) const;
    QString prayer(const QString& key) const { return t("prayer." + key); }
    /// QLocale for date formatting in the current language.
    QLocale locale() const;

signals:
    void changed();

private:
    Tr();
    QString m_lang = QStringLiteral("en");
    int m_rev = 0;
    QHash<QString, QHash<QString, QString>> m_table;  // key → lang → text
};

#pragma once
#include <QObject>
#include <QString>

/// Public identity shown in the footer (mirrors core/src/about.ts).
class About : public QObject {
    Q_OBJECT
    Q_PROPERTY(QString authorName READ authorName CONSTANT)
    Q_PROPERTY(QString authorUrl READ authorUrl CONSTANT)
    Q_PROPERTY(QString contactEmail READ contactEmail CONSTANT)
public:
    using QObject::QObject;
    QString authorName() const { return QStringLiteral("Hatem Selim"); }
    QString authorUrl() const { return QStringLiteral("https://www.linkedin.com/in/hatemselim94/"); }
    QString contactEmail() const { return QString(); }  // empty hides the Contact link
};

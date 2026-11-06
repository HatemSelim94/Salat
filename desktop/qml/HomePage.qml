import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import Salat

Page {
    id: page
    signal openSettings()
    signal openMonth()
    signal openQibla()
    background: Rectangle { color: Theme.bg }

    ColumnLayout {
        anchors.fill: parent
        anchors.margins: 20
        spacing: 14

        // Header: location + dates
        RowLayout {
            Layout.fillWidth: true
            Label {
                text: settingsObj.label
                font.pixelSize: 17
                font.weight: Font.DemiBold
                color: Theme.fg
                elide: Text.ElideRight
                Layout.fillWidth: true
            }
            ColumnLayout {
                spacing: 0
                Label { text: engine.hijriDate; color: Theme.muted; font.pixelSize: 12; horizontalAlignment: Text.AlignRight; Layout.alignment: Qt.AlignRight }
                Label { text: engine.gregorianDate; color: Theme.muted; font.pixelSize: 12; horizontalAlignment: Text.AlignRight; Layout.alignment: Qt.AlignRight }
            }
        }

        // Ramadan: Suhoor ends at Fajr, Iftar at Maghrib
        Rectangle {
            Layout.fillWidth: true
            visible: engine.ramadan
            implicitHeight: 52
            radius: 12
            color: Theme.card
            border.color: Theme.line
            RowLayout {
                anchors { fill: parent; leftMargin: 14; rightMargin: 14 }
                ColumnLayout {
                    spacing: 0
                    Layout.fillWidth: true
                    Label { text: Tr.t("home.ramadan", Tr.rev).toUpperCase(); color: Theme.accent; font.pixelSize: 10; font.weight: Font.DemiBold; font.letterSpacing: 1 }
                    Label { text: engine.ramadanLabel; color: Theme.fg; font.pixelSize: 15; font.weight: Font.DemiBold }
                }
                Label { text: engine.ramadanTime + " · " + engine.ramadanCountdown; color: Theme.fg; font.pixelSize: 13 }
            }
        }

        // Next prayer card
        Rectangle {
            Layout.fillWidth: true
            implicitHeight: nextCol.implicitHeight + 28
            radius: Theme.radius
            color: Theme.accentSoft
            ColumnLayout {
                id: nextCol
                anchors { left: parent.left; right: parent.right; top: parent.top; margins: 14 }
                spacing: 2
                Label { text: Tr.t("home.nextPrayer", Tr.rev); color: Theme.muted; font.pixelSize: 12 }
                Label { text: engine.nextLabel; color: Theme.fg; font.pixelSize: 26; font.weight: Font.DemiBold }
                RowLayout {
                    spacing: 6
                    Label { text: engine.nextTime + (engine.nextIsTomorrow ? " " + Tr.t("home.tomorrow", Tr.rev) : "") + " · " + Tr.t("home.in", Tr.rev); color: Theme.muted; font.pixelSize: 13 }
                    Label { text: engine.countdown; color: Theme.fg; font.pixelSize: 13; font.weight: Font.DemiBold }
                }
            }
        }

        // Today's times
        ColumnLayout {
            Layout.fillWidth: true
            spacing: 0
            Rectangle { Layout.fillWidth: true; height: 1; color: Theme.line }
            Repeater {
                model: engine.today
                delegate: Rectangle {
                    required property var modelData
                    Layout.fillWidth: true
                    implicitHeight: 38
                    radius: modelData.state === "current" ? 8 : 0
                    color: modelData.state === "current" ? Theme.card : "transparent"
                    RowLayout {
                        anchors { fill: parent; leftMargin: 8; rightMargin: 8 }
                        Label {
                            text: modelData.label
                            Layout.fillWidth: true
                            font.pixelSize: modelData.sunrise ? 13 : 14
                            font.weight: modelData.state === "current" ? Font.DemiBold : Font.Normal
                            color: modelData.state === "current" ? Theme.fg : (modelData.sunrise || modelData.state === "passed" ? Theme.muted : Theme.fg)
                        }
                        Label { text: modelData.iqamah ? Tr.t("home.iqamah", Tr.rev).substring(0, 3) + " " + modelData.iqamah : ""; visible: modelData.iqamah !== ""; color: Theme.muted; font.pixelSize: 11 }
                        Rectangle { width: 7; height: 7; radius: 4; color: Theme.accent; visible: modelData.state === "current" }
                        Label {
                            text: modelData.time
                            font.pixelSize: modelData.sunrise ? 13 : 14
                            font.weight: modelData.state === "current" ? Font.DemiBold : Font.Normal
                            color: modelData.state === "current" ? Theme.fg : (modelData.sunrise || modelData.state === "passed" ? Theme.muted : Theme.fg)
                        }
                    }
                    Rectangle {
                        anchors { left: parent.left; right: parent.right; bottom: parent.bottom }
                        height: 1
                        color: Theme.line
                        visible: modelData.state !== "current"
                    }
                }
            }
        }

        // Nudge until a city has been chosen
        Rectangle {
            Layout.fillWidth: true
            visible: !settingsObj.locationConfigured
            implicitHeight: nudge.implicitHeight + 20
            radius: 8
            color: Theme.card
            border.color: Theme.line
            Label {
                id: nudge
                anchors { left: parent.left; right: parent.right; top: parent.top; margins: 10 }
                wrapMode: Text.WordWrap
                font.pixelSize: 12
                color: Theme.fg
                text: Tr.t("home.locationGuessed", Tr.rev) + " <a href='settings'>" + Tr.t("home.setCity", Tr.rev) + "</a> " + Tr.t("home.forExactTimes", Tr.rev)
                linkColor: Theme.accent
                onLinkActivated: page.openSettings()
            }
        }

        Item { Layout.fillHeight: true }

        Label {
            Layout.fillWidth: true
            text: Tr.t("home.qibla", Tr.rev) + ": " + Tr.f("home.qiblaBearing", { deg: Math.round(engine.qiblaBearing) }, Tr.rev)
                  + " · " + Tr.f("home.qiblaDistance", { km: Number(Math.round(engine.qiblaDistanceKm)).toLocaleString(Qt.locale(), "f", 0) }, Tr.rev)
            color: Theme.muted; font.pixelSize: 12; elide: Text.ElideRight
        }
        RowLayout {
            Layout.fillWidth: true
            spacing: 12
            Label { text: engine.methodLabel; color: Theme.muted; font.pixelSize: 12; elide: Text.ElideRight; Layout.fillWidth: true }
            Repeater {
                model: [["month", "home.month"], ["qibla", "home.qibla"], ["settings", "home.settings"]]
                delegate: Label {
                    required property var modelData
                    text: "<a href='" + modelData[0] + "'>" + Tr.t(modelData[1], Tr.rev) + "</a>"
                    font.pixelSize: 12
                    linkColor: Theme.accent
                    onLinkActivated: (link) => { if (link === "month") page.openMonth(); else if (link === "qibla") page.openQibla(); else page.openSettings() }
                    MouseArea { anchors.fill: parent; cursorShape: Qt.PointingHandCursor; acceptedButtons: Qt.NoButton }
                }
            }
        }
    }
}

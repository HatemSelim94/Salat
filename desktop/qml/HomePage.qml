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

    readonly property var pc: Theme.period(engine.period)

    ColumnLayout {
        anchors.fill: parent
        anchors.margins: 20
        spacing: 14

        // Header: location (opens settings) + dates in a soft pill
        RowLayout {
            Layout.fillWidth: true
            spacing: 10
            RowLayout {
                Layout.fillWidth: true
                spacing: 6
                Canvas { // pin
                    width: 12; height: 14
                    onPaint: {
                        const ctx = getContext("2d"); ctx.reset(); ctx.fillStyle = Theme.accent;
                        ctx.beginPath(); ctx.arc(6, 5.5, 5.5, 0, Math.PI * 2); ctx.fill();
                        ctx.beginPath(); ctx.moveTo(1.6, 8); ctx.lineTo(6, 14); ctx.lineTo(10.4, 8); ctx.closePath(); ctx.fill();
                        ctx.fillStyle = Theme.bg; ctx.beginPath(); ctx.arc(6, 5.5, 2, 0, Math.PI * 2); ctx.fill();
                    }
                }
                Label {
                    text: settingsObj.label
                    font.pixelSize: 17
                    font.weight: Font.DemiBold
                    color: Theme.fg
                    elide: Text.ElideRight
                    Layout.fillWidth: true
                }
                TapHandler { onTapped: page.openSettings() }
                HoverHandler { cursorShape: Qt.PointingHandCursor }
            }
            Rectangle {
                radius: 8
                color: Theme.card
                border.color: Theme.line
                implicitWidth: dates.implicitWidth + 20
                implicitHeight: dates.implicitHeight + 10
                ColumnLayout {
                    id: dates
                    anchors.centerIn: parent
                    spacing: 0
                    Label { text: engine.hijriDate; color: Theme.fg; font.pixelSize: 11; font.weight: Font.Medium; Layout.alignment: Qt.AlignRight }
                    Label { text: engine.gregorianDate; color: Theme.muted; font.pixelSize: 11; Layout.alignment: Qt.AlignRight }
                }
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

        // Next prayer card, coloured by the period of the day
        Rectangle {
            Layout.fillWidth: true
            implicitHeight: nextCol.implicitHeight + 32
            radius: 16
            gradient: Gradient {
                orientation: Gradient.Horizontal
                GradientStop { position: 0; color: page.pc[0]; Behavior on color { ColorAnimation { duration: 900 } } }
                GradientStop { position: 1; color: page.pc[1]; Behavior on color { ColorAnimation { duration: 900 } } }
            }
            ColumnLayout {
                id: nextCol
                anchors { left: parent.left; right: parent.right; top: parent.top; margins: 16 }
                spacing: 2
                RowLayout {
                    Layout.fillWidth: true
                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 2
                        Label { text: Tr.t("home.nextPrayer", Tr.rev).toUpperCase(); color: page.pc[3]; font.pixelSize: 10; font.weight: Font.DemiBold; font.letterSpacing: 1.2 }
                        Label { text: engine.nextLabel; color: page.pc[2]; font.pixelSize: 28; font.weight: Font.Bold }
                        RowLayout {
                            spacing: 6
                            Label { text: engine.nextTime + (engine.nextIsTomorrow ? " " + Tr.t("home.tomorrow", Tr.rev) : "") + "  ·  " + Tr.t("home.in", Tr.rev); color: page.pc[3]; font.pixelSize: 13 }
                            Label {
                                text: engine.countdown; color: page.pc[2]; font.pixelSize: 13; font.weight: Font.DemiBold
                                Behavior on text { SequentialAnimation { NumberAnimation { target: cd; property: "opacity"; to: 0; duration: 150 } PropertyAction {} NumberAnimation { target: cd; property: "opacity"; to: 1; duration: 250 } } }
                                id: cd
                            }
                        }
                    }
                    PrayerGlyph { prayer: engine.nextKey; color: Theme.alpha(page.pc[2], 0.85); width: 40; height: 40; Layout.alignment: Qt.AlignTop }
                }
                Item { height: 6 }
                DayProgress {
                    Layout.fillWidth: true
                    progress: engine.dayProgress
                    dots: engine.dayDots
                    track: Theme.alpha(page.pc[2], 0.18)
                    fill: Theme.alpha(page.pc[2], 0.9)
                }
            }
        }

        // Today's times
        ColumnLayout {
            Layout.fillWidth: true
            spacing: 0
            Repeater {
                model: engine.today
                delegate: Rectangle {
                    id: row
                    required property var modelData
                    readonly property bool current: modelData.state === "current"
                    readonly property color fg: current ? Theme.fg : (modelData.sunrise || modelData.state === "passed" ? Theme.muted : Theme.fg)
                    Layout.fillWidth: true
                    implicitHeight: modelData.sunrise ? 34 : 40
                    radius: 8
                    color: current ? Theme.card : "transparent"
                    Rectangle { // accent bar on the leading edge
                        anchors { left: parent.left; verticalCenter: parent.verticalCenter }
                        width: 3; height: 20; radius: 2
                        color: Theme.accent; visible: row.current
                    }
                    RowLayout {
                        anchors { fill: parent; leftMargin: 12; rightMargin: 10 }
                        spacing: 12
                        PrayerGlyph { prayer: row.modelData.key; color: row.current ? Theme.accent : Theme.alpha(row.fg, row.modelData.sunrise ? 0.7 : 0.9); width: 18; height: 18 }
                        Label {
                            text: row.modelData.label
                            Layout.fillWidth: true
                            font.pixelSize: row.modelData.sunrise ? 13 : 14
                            font.weight: row.current ? Font.DemiBold : Font.Normal
                            color: row.fg
                        }
                        Label { text: row.modelData.iqamah ? Tr.t("home.iqamah", Tr.rev).substring(0, 3) + " " + row.modelData.iqamah : ""; visible: row.modelData.iqamah !== ""; color: Theme.muted; font.pixelSize: 11 }
                        Label {
                            text: row.modelData.time
                            font.pixelSize: row.modelData.sunrise ? 13 : 16
                            font.weight: row.current ? Font.DemiBold : Font.Medium
                            color: row.fg
                        }
                    }
                    Rectangle {
                        anchors { left: parent.left; right: parent.right; bottom: parent.bottom; leftMargin: 42 }
                        height: 1
                        color: Theme.line
                        visible: !row.current && row.modelData.key !== "isha"
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

        // Bottom row: method + Qibla summary on the left, icon buttons on the right
        RowLayout {
            Layout.fillWidth: true
            spacing: 4
            ColumnLayout {
                Layout.fillWidth: true
                spacing: 0
                Label { text: engine.methodLabel; color: Theme.muted; font.pixelSize: 11; elide: Text.ElideRight; Layout.fillWidth: true }
                Label {
                    text: Tr.t("home.qibla", Tr.rev) + " " + Tr.f("home.qiblaBearing", { deg: Math.round(engine.qiblaBearing) }, Tr.rev)
                          + " · " + Tr.f("home.qiblaDistance", { km: Number(Math.round(engine.qiblaDistanceKm)).toLocaleString(Qt.locale(), "f", 0) }, Tr.rev)
                    color: Theme.muted; font.pixelSize: 11; elide: Text.ElideRight; Layout.fillWidth: true
                }
            }
            Repeater {
                model: [["calendar", "home.month"], ["compass", "home.qibla"], ["gear", "home.settings"]]
                delegate: Rectangle {
                    id: nav
                    required property var modelData
                    property bool hovered: false
                    implicitWidth: navCol.implicitWidth + 20
                    implicitHeight: navCol.implicitHeight + 10
                    radius: 8
                    color: hovered ? Theme.card : "transparent"
                    ColumnLayout {
                        id: navCol
                        anchors.centerIn: parent
                        spacing: 2
                        NavGlyph { icon: nav.modelData[0]; color: nav.hovered ? Theme.accent : Theme.muted; Layout.alignment: Qt.AlignHCenter }
                        Label { text: Tr.t(nav.modelData[1], Tr.rev); font.pixelSize: 10; color: nav.hovered ? Theme.accent : Theme.muted; Layout.alignment: Qt.AlignHCenter }
                    }
                    HoverHandler { onHoveredChanged: nav.hovered = hovered; cursorShape: Qt.PointingHandCursor }
                    TapHandler { onTapped: { if (nav.modelData[0] === "calendar") page.openMonth(); else if (nav.modelData[0] === "compass") page.openQibla(); else page.openSettings() } }
                }
            }
        }
    }
}

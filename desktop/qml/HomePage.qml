import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import Salat

Page {
    id: page
    signal openSettings()
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
                Label { text: "Next prayer"; color: Theme.muted; font.pixelSize: 12 }
                Label { text: engine.nextLabel; color: Theme.fg; font.pixelSize: 26; font.weight: Font.DemiBold }
                RowLayout {
                    spacing: 6
                    Label { text: engine.nextTime + (engine.nextIsTomorrow ? " tomorrow" : "") + " · in"; color: Theme.muted; font.pixelSize: 13 }
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
                text: "Location guessed from your time zone. <a href='settings'>Set your city</a> for exact times."
                linkColor: Theme.accent
                onLinkActivated: page.openSettings()
            }
        }

        Item { Layout.fillHeight: true }

        RowLayout {
            Layout.fillWidth: true
            Label { text: engine.methodLabel; color: Theme.muted; font.pixelSize: 12; elide: Text.ElideRight; Layout.fillWidth: true }
            Label {
                text: "<a href='settings'>Settings</a>"
                font.pixelSize: 12
                linkColor: Theme.accent
                onLinkActivated: page.openSettings()
                MouseArea { anchors.fill: parent; cursorShape: Qt.PointingHandCursor; acceptedButtons: Qt.NoButton }
            }
        }
    }
}

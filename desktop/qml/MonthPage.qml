import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import Salat

Page {
    id: page
    signal back()
    background: Rectangle { color: Theme.bg }
    property date cursor: new Date()
    property int year: cursor.getFullYear()
    property int month: cursor.getMonth() + 1
    property var rows: engine.monthTable(year, month)
    Connections { target: engine; function onUpdated() { page.rows = engine.monthTable(page.year, page.month) } }
    function shift(delta) { var d = new Date(cursor); d.setDate(1); d.setMonth(d.getMonth() + delta); cursor = d; rows = engine.monthTable(year, month) }

    header: ToolBar {
        background: Rectangle { color: Theme.bg }
        RowLayout {
            anchors.fill: parent
            anchors.leftMargin: 8
            anchors.rightMargin: 12
            ToolButton { text: Tr.rtl ? "›" : "‹"; font.pixelSize: 24; onClicked: page.back() }
            Label { text: Tr.t("month.title", Tr.rev); font.pixelSize: 17; font.weight: Font.DemiBold; color: Theme.fg; Layout.fillWidth: true }
            Button { text: Tr.t("month.export", Tr.rev); flat: true; font.capitalization: Font.MixedCase; onClicked: engine.exportMonthCsv(page.year, page.month) }
        }
    }

    ColumnLayout {
        anchors.fill: parent
        anchors.margins: 12
        spacing: 6
        RowLayout {
            Layout.fillWidth: true
            ToolButton { text: "‹"; onClicked: page.shift(-1) }
            Label { text: engine.monthName(page.year, page.month); font.weight: Font.DemiBold; color: Theme.fg; horizontalAlignment: Text.AlignHCenter; Layout.fillWidth: true }
            ToolButton { text: "›"; onClicked: page.shift(1) }
            Button { text: Tr.t("month.today", Tr.rev); flat: true; font.capitalization: Font.MixedCase; onClicked: { page.cursor = new Date(); page.rows = engine.monthTable(page.year, page.month) } }
        }
        RowLayout {
            Layout.fillWidth: true
            Item { Layout.preferredWidth: 54 }
            Repeater {
                model: ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"]
                delegate: Label { required property string modelData; text: Tr.t("prayer." + modelData, Tr.rev); color: Theme.muted; font.pixelSize: 10; horizontalAlignment: Text.AlignHCenter; Layout.fillWidth: true; elide: Text.ElideRight }
            }
        }
        Rectangle { Layout.fillWidth: true; height: 1; color: Theme.line }
        ListView {
            id: list
            Layout.fillWidth: true
            Layout.fillHeight: true
            clip: true
            model: page.rows
            ScrollBar.vertical: ScrollBar {}
            Component.onCompleted: { for (var i = 0; i < page.rows.length; i++) if (page.rows[i].today) { positionViewAtIndex(i, ListView.Center); break } }
            delegate: Rectangle {
                required property var modelData
                readonly property var rowData: modelData
                width: list.width
                height: 34
                radius: 6
                color: modelData.today ? Theme.accentSoft : "transparent"
                RowLayout {
                    anchors { fill: parent; leftMargin: 6; rightMargin: 6 }
                    ColumnLayout {
                        spacing: -2
                        Layout.preferredWidth: 48
                        Label { text: modelData.day + " " + modelData.weekday; color: modelData.friday ? Theme.accent : Theme.fg; font.pixelSize: 12; font.weight: modelData.friday || modelData.today ? Font.DemiBold : Font.Normal }
                        Label { text: modelData.hijri; color: Theme.muted; font.pixelSize: 9 }
                    }
                    Repeater {
                        model: ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"]
                        delegate: Label {
                            required property string modelData
                            text: rowData[modelData]
                            color: modelData === "sunrise" ? Theme.muted : Theme.fg
                            font.pixelSize: 12
                            horizontalAlignment: Text.AlignHCenter
                            Layout.fillWidth: true
                        }
                    }
                }
                Rectangle {
                    anchors { left: parent.left; right: parent.right; bottom: parent.bottom }
                    height: 1
                    color: Theme.line
                }
            }
        }
    }
}

import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import Salat

Page {
    id: page
    signal back()
    background: Rectangle { color: Theme.bg }

    header: ToolBar {
        background: Rectangle { color: Theme.bg }
        RowLayout {
            anchors.fill: parent
            anchors.leftMargin: 8
            ToolButton { text: Tr.rtl ? "›" : "‹"; font.pixelSize: 24; onClicked: page.back() }
            Label { text: Tr.t("home.qibla", Tr.rev); font.pixelSize: 17; font.weight: Font.DemiBold; color: Theme.fg; Layout.fillWidth: true }
        }
    }

    ColumnLayout {
        anchors.fill: parent
        anchors.margins: 24
        spacing: 10
        Label { text: Tr.f("home.qiblaBearing", { deg: Math.round(engine.qiblaBearing) }, Tr.rev); font.pixelSize: 22; font.weight: Font.DemiBold; color: Theme.fg; Layout.alignment: Qt.AlignHCenter }
        Label { text: Tr.f("home.qiblaDistance", { km: Number(Math.round(engine.qiblaDistanceKm)).toLocaleString(Qt.locale(), "f", 0) }, Tr.rev); font.pixelSize: 13; color: Theme.muted; Layout.alignment: Qt.AlignHCenter }
        Item { Layout.preferredHeight: 16 }
        // Static compass rose: north at the top, arrow at the Qibla bearing. (No magnetometer on desktops.)
        Item {
            id: rose
            Layout.alignment: Qt.AlignHCenter
            width: 240; height: 240
            Rectangle { anchors.fill: parent; radius: width / 2; color: "transparent"; border.color: Theme.line; border.width: 2 }
            Repeater {
                model: 12
                delegate: Rectangle {
                    required property int index
                    width: 2; height: index % 3 === 0 ? 14 : 8; radius: 1; color: Theme.line
                    x: rose.width / 2 - 1; y: 6
                    transform: Rotation { origin.x: 1; origin.y: rose.height / 2 - 6; angle: index * 30 }
                }
            }
            Label { text: "N"; color: Theme.muted; font.pixelSize: 12; font.weight: Font.DemiBold; anchors { top: parent.top; topMargin: 22; horizontalCenter: parent.horizontalCenter } }
            // Arrow drawn with Canvas (no QtQuick.Shapes dependency).
            Canvas {
                id: arrow
                anchors.fill: parent
                property real bearing: engine.qiblaBearing
                property color fill: Theme.accent
                onBearingChanged: requestPaint()
                onFillChanged: requestPaint()
                onPaint: {
                    var ctx = getContext("2d"); ctx.reset()
                    var cx = width / 2, cy = height / 2
                    ctx.translate(cx, cy); ctx.rotate(bearing * Math.PI / 180); ctx.translate(-cx, -cy)
                    ctx.fillStyle = fill
                    ctx.beginPath()
                    ctx.moveTo(cx, 44); ctx.lineTo(cx - 16, cy + 10); ctx.lineTo(cx, cy - 4); ctx.lineTo(cx + 16, cy + 10); ctx.closePath()
                    ctx.fill()
                }
            }
            Rectangle { width: 8; height: 8; radius: 4; color: Theme.card; border.color: Theme.accent; anchors.centerIn: parent }
        }
        Label { text: Tr.t("home.qiblaNoCompass", Tr.rev); color: Theme.muted; font.pixelSize: 12; wrapMode: Text.WordWrap; horizontalAlignment: Text.AlignHCenter; Layout.fillWidth: true }
        Item { Layout.fillHeight: true }
    }
}

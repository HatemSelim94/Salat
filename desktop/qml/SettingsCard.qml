import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import Salat

Rectangle {
    id: card
    property string title
    default property alias content: inner.data
    Layout.fillWidth: true
    implicitHeight: inner.implicitHeight + 36
    radius: 14
    color: Theme.card
    border.color: Theme.line

    ColumnLayout {
        id: inner
        anchors { left: parent.left; right: parent.right; top: parent.top; margins: 18 }
        spacing: 10
        Label {
            text: card.title.toUpperCase()
            color: Theme.muted
            font.pixelSize: 11
            font.letterSpacing: 1
            font.weight: Font.DemiBold
        }
    }
}

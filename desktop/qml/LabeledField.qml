import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import Salat

ColumnLayout {
    property string label
    property alias text: field.text
    signal committed(string value)
    spacing: 0
    Layout.fillWidth: true
    Label { text: label; color: Theme.muted; font.pixelSize: 11 }
    TextField {
        id: field
        Layout.fillWidth: true
        font.pixelSize: 13
        onEditingFinished: committed(text)
    }
}

import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import Salat

// A labelled setting. Small controls (switches) sit to the right; wide ones
// (combo boxes) go on their own line underneath (stacked: true).
ColumnLayout {
    id: row
    property string label
    property string hint
    property bool stacked: false
    default property alias control: slot.data
    Layout.fillWidth: true
    spacing: 4

    RowLayout {
        Layout.fillWidth: true
        spacing: 12
        ColumnLayout {
            Layout.fillWidth: true
            spacing: 0
            Label { text: row.label; color: Theme.fg; wrapMode: Text.WordWrap; Layout.fillWidth: true }
            Label { text: row.hint; visible: row.hint.length > 0; color: Theme.muted; font.pixelSize: 12; wrapMode: Text.WordWrap; Layout.fillWidth: true }
        }
        RowLayout { id: inlineSlot; visible: !row.stacked; spacing: 6 }
    }
    RowLayout {
        id: slot
        Layout.fillWidth: true
        visible: row.stacked || children.length > 0
        spacing: 6
        Component.onCompleted: {
            if (!row.stacked) {
                // Move the control up next to the label.
                while (slot.children.length > 0) slot.children[0].parent = inlineSlot
            }
        }
    }
}

pragma Singleton
import QtQuick

QtObject {
    property bool dark: false
    readonly property color bg: dark ? "#161a19" : "#fbfaf7"
    readonly property color fg: dark ? "#ecebe6" : "#1d1f1e"
    readonly property color muted: dark ? "#96a09b" : "#6b7470"
    readonly property color line: dark ? "#2a302e" : "#e6e3dc"
    readonly property color card: dark ? "#1e2422" : "#ffffff"
    readonly property color accent: dark ? "#5fb49c" : "#2f6f5e"
    readonly property color accentSoft: dark ? "#223730" : "#e4f0ec"
    readonly property color accentFg: dark ? "#0f1513" : "#ffffff"
    readonly property int radius: 12
}

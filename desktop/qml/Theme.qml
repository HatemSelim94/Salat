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

    function alpha(c, a) { const k = Qt.color(c); return Qt.rgba(k.r, k.g, k.b, a); }

    // Time-of-day colours for the next-prayer card: [start, end, fg, fgMuted].
    function period(p) {
        const light = {
            night: ["#243b4a", "#1b2a36", "#f2efe6", "#b9c4cc"], dawn: ["#f6d9b8", "#e8c2a0", "#2b2520", "#6f5f52"],
            morning: ["#dcebf3", "#c9e0ee", "#1d2a33", "#5f7280"], day: ["#e4f0ec", "#d2e6df", "#1d1f1e", "#5e6e68"],
            afternoon: ["#f3e6cf", "#ebd7b4", "#2b2520", "#6f6150"], dusk: ["#e9c3a6", "#c89a8a", "#2b2020", "#6e5450"] };
        const darkSet = {
            night: ["#1b2a36", "#121c24", "#ecebe6", "#9aa8b3"], dawn: ["#4b3a2e", "#34281f", "#f2e6d8", "#bba894"],
            morning: ["#2a3e4c", "#1e2e39", "#e6eef3", "#9fb0bc"], day: ["#223730", "#1a2a25", "#ecebe6", "#96a09b"],
            afternoon: ["#4a3d2a", "#342b1e", "#f2e8d6", "#bdae94"], dusk: ["#4c3030", "#352222", "#f2e2da", "#c0a49c"] };
        return (dark ? darkSet : light)[p] || light.day;
    }
}

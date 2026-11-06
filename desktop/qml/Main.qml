import QtQuick
import QtQuick.Controls
import QtQuick.Controls.Material
import QtQuick.Layouts
import Salat

ApplicationWindow {
    id: root
    width: 360
    height: 600
    minimumWidth: 320
    minimumHeight: 480
    title: Tr.t("app.name", Tr.rev)
    color: Theme.bg
    LayoutMirroring.enabled: Tr.rtl
    LayoutMirroring.childrenInherit: true

    Material.theme: useDarkTheme ? Material.Dark : Material.Light
    Material.accent: Theme.accent
    Material.background: Theme.bg
    Material.foreground: Theme.fg

    Component.onCompleted: {
        Theme.dark = useDarkTheme
        if (initialPage === "settings") stack.push(settingsPage, StackView.Immediate)
        else if (initialPage === "month") stack.push(monthPage, StackView.Immediate)
        else if (initialPage === "qibla") stack.push(qiblaPage, StackView.Immediate)
    }

    StackView {
        id: stack
        anchors.fill: parent
        initialItem: homePage
        pushEnter: Transition { NumberAnimation { property: "x"; from: stack.width; to: 0; duration: 180; easing.type: Easing.OutCubic } }
        pushExit: Transition { NumberAnimation { property: "x"; from: 0; to: -stack.width / 3; duration: 180; easing.type: Easing.OutCubic } }
        popEnter: Transition { NumberAnimation { property: "x"; from: -stack.width / 3; to: 0; duration: 180; easing.type: Easing.OutCubic } }
        popExit: Transition { NumberAnimation { property: "x"; from: 0; to: stack.width; duration: 180; easing.type: Easing.OutCubic } }
    }

    Component {
        id: homePage
        HomePage {
            onOpenSettings: stack.push(settingsPage)
            onOpenMonth: stack.push(monthPage)
            onOpenQibla: stack.push(qiblaPage)
        }
    }
    Component {
        id: settingsPage
        SettingsPage { onBack: stack.pop() }
    }
    Component {
        id: monthPage
        MonthPage { onBack: stack.pop() }
    }
    Component {
        id: qiblaPage
        QiblaPage { onBack: stack.pop() }
    }
}

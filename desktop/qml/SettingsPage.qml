import QtQuick
import QtQuick.Controls
import QtQuick.Controls.Material
import QtQuick.Layouts
import Salat

Page {
    id: page
    signal back()
    background: Rectangle { color: Theme.bg }

    readonly property var methods: [
        ["MuslimWorldLeague", "Muslim World League"], ["Egyptian", "Egyptian General Authority"], ["Karachi", "Karachi"],
        ["UmmAlQura", "Umm al-Qura (Makkah)"], ["Dubai", "Dubai"], ["MoonsightingCommittee", "Moonsighting Committee"],
        ["NorthAmerica", "ISNA (North America)"], ["Kuwait", "Kuwait"], ["Qatar", "Qatar"], ["Singapore", "Singapore"],
        ["Tehran", "Tehran"], ["Turkey", "Diyanet (Turkey)"]]
    readonly property var rules: [
        ["recommended", "Automatic"], ["middleOfTheNight", "Middle of the night"],
        ["seventhOfTheNight", "One-seventh of the night"], ["twilightAngle", "Twilight angle"]]
    readonly property var prayers: ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"]

    function indexOfKey(list, key) { for (let i = 0; i < list.length; i++) if (list[i][0] === key) return i; return 0 }

    header: ToolBar {
        background: Rectangle { color: Theme.bg }
        RowLayout {
            anchors.fill: parent
            anchors.leftMargin: 8
            ToolButton { text: "‹"; font.pixelSize: 24; onClicked: page.back() }
            Label { text: "Settings"; font.pixelSize: 17; font.weight: Font.DemiBold; color: Theme.fg; Layout.fillWidth: true }
        }
    }

    Flickable {
        anchors.fill: parent
        contentHeight: col.implicitHeight + 40
        clip: true
        ScrollBar.vertical: ScrollBar {}

        ColumnLayout {
            id: col
            width: parent.width - 40
            x: 20
            y: 8
            spacing: 14

            // ---- Location --------------------------------------------------
            SettingsCard {
                title: "Location"
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 6
                    TextField {
                        id: cityField
                        Layout.fillWidth: true
                        placeholderText: "Search a city…"
                        text: settingsObj.label
                        onTextEdited: results.model = Cities.search(text, 6)
                        onActiveFocusChanged: if (!activeFocus) results.model = []
                    }
                    Rectangle {
                        Layout.fillWidth: true
                        visible: results.count > 0
                        implicitHeight: results.contentHeight
                        radius: 8
                        color: Theme.card
                        border.color: Theme.line
                        ListView {
                            id: results
                            anchors.fill: parent
                            model: []
                            interactive: false
                            delegate: ItemDelegate {
                                required property var modelData
                                width: results.width
                                height: 34
                                contentItem: RowLayout {
                                    Label { text: modelData.name + ", " + modelData.country; color: Theme.fg; Layout.fillWidth: true }
                                    Label { text: modelData.tz; color: Theme.muted; font.pixelSize: 11 }
                                }
                                onClicked: {
                                    settingsObj.setLocation(modelData.lat, modelData.lng, modelData.tz, modelData.name + ", " + modelData.country)
                                    cityField.text = settingsObj.label
                                    results.model = []
                                    cityField.focus = false
                                }
                            }
                        }
                    }
                    RowLayout {
                        spacing: 8
                        Layout.fillWidth: true
                        LabeledField { label: "Latitude"; text: settingsObj.latitude.toFixed(2); onCommitted: (v) => settingsObj.latitude = Number(v) }
                        LabeledField { label: "Longitude"; text: settingsObj.longitude.toFixed(2); onCommitted: (v) => settingsObj.longitude = Number(v) }
                        LabeledField { label: "Time zone"; text: settingsObj.timezone; onCommitted: (v) => settingsObj.timezone = v; Layout.preferredWidth: 140 }
                    }
                }
            }

            // ---- Calculation -----------------------------------------------
            SettingsCard {
                title: "Calculation"
                SettingRow {
                    label: "Method"
                    hint: "Use what your local mosque or country authority uses."
                    stacked: true
                    ComboBox {
                        model: page.methods.map(m => m[1])
                        currentIndex: page.indexOfKey(page.methods, settingsObj.method)
                        onActivated: (i) => settingsObj.method = page.methods[i][0]
                        font.pixelSize: 13
                        Layout.fillWidth: true
                    }
                }
                RowLayout {
                    Layout.fillWidth: true
                    spacing: 12
                    SettingRow {
                        label: "Asr"
                        hint: "Hanafi is later."
                        stacked: true
                        ComboBox {
                            model: ["Standard", "Hanafi"]
                            currentIndex: settingsObj.madhab === "hanafi" ? 1 : 0
                            onActivated: (i) => settingsObj.madhab = i === 1 ? "hanafi" : "shafi"
                            font.pixelSize: 13
                            Layout.fillWidth: true
                        }
                    }
                    SettingRow {
                        label: "High-latitude rule"
                        hint: "Summer, north of ~48°."
                        stacked: true
                        ComboBox {
                            model: page.rules.map(r => r[1])
                            currentIndex: page.indexOfKey(page.rules, settingsObj.highLatitudeRule)
                            onActivated: (i) => settingsObj.highLatitudeRule = page.rules[i][0]
                            font.pixelSize: 13
                            Layout.fillWidth: true
                        }
                    }
                }
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 4
                    Label { text: "Adjustments (minutes)"; color: Theme.fg }
                    Label { text: "Shift individual prayers to match a local timetable."; color: Theme.muted; font.pixelSize: 12 }
                    GridLayout {
                        columns: 3
                        columnSpacing: 12
                        rowSpacing: 4
                        Layout.fillWidth: true
                        Repeater {
                            model: page.prayers
                            delegate: RowLayout {
                                required property string modelData
                                Label { text: modelData.charAt(0).toUpperCase() + modelData.slice(1); color: Theme.fg; Layout.fillWidth: true }
                                TextField {
                                    text: settingsObj.adjustment(modelData)
                                    validator: IntValidator { bottom: -60; top: 60 }
                                    horizontalAlignment: TextInput.AlignRight
                                    font.pixelSize: 13
                                    Layout.preferredWidth: 44
                                    onEditingFinished: settingsObj.setAdjustment(modelData, Number(text))
                                }
                            }
                        }
                    }
                }
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 4
                    Label { text: "Today with these settings"; color: Theme.muted; font.pixelSize: 12 }
                    RowLayout {
                        Layout.fillWidth: true
                        Repeater {
                            model: engine.today
                            delegate: ColumnLayout {
                                required property var modelData
                                spacing: 0
                                Layout.fillWidth: true
                                Label { text: modelData.label; color: Theme.muted; font.pixelSize: 11; Layout.alignment: Qt.AlignHCenter }
                                Label { text: modelData.time; color: Theme.fg; font.pixelSize: 13; font.weight: Font.DemiBold; Layout.alignment: Qt.AlignHCenter }
                            }
                        }
                    }
                }
            }

            // ---- Reminders -------------------------------------------------
            SettingsCard {
                title: "Reminders"
                SettingRow { label: "Notify at prayer times"; Switch { checked: settingsObj.notificationsEnabled; onToggled: settingsObj.notificationsEnabled = checked } }
                SettingRow {
                    label: "Heads-up before each prayer"
                    ComboBox {
                        readonly property var values: [0, 5, 10, 15, 20, 30]
                        model: ["Off", "5 min", "10 min", "15 min", "20 min", "30 min"]
                        currentIndex: Math.max(0, values.indexOf(settingsObj.preReminderMinutes))
                        onActivated: (i) => settingsObj.preReminderMinutes = values[i]
                        font.pixelSize: 13
                        Layout.preferredWidth: 110
                    }
                }
                SettingRow { label: "Play a short chime"; hint: "A quiet tone, not an adhan recording."; Switch { checked: settingsObj.sound; onToggled: settingsObj.sound = checked } }
                SettingRow {
                    label: "Snooze length"
                    ComboBox {
                        readonly property var values: [5, 10, 15, 20, 30]
                        model: ["5 min", "10 min", "15 min", "20 min", "30 min"]
                        currentIndex: Math.max(0, values.indexOf(settingsObj.snoozeMinutes))
                        onActivated: (i) => settingsObj.snoozeMinutes = values[i]
                        font.pixelSize: 13
                        Layout.preferredWidth: 110
                    }
                }
                SettingRow { label: "12-hour clock"; Switch { checked: settingsObj.hour12; onToggled: settingsObj.hour12 = checked } }
                Button { text: "Send a test notification"; flat: true; font.capitalization: Font.MixedCase; onClicked: engine.testNotification() }
            }

            // ---- App -------------------------------------------------------
            SettingsCard {
                title: "App"
                SettingRow { label: "Start with the system"; hint: "Runs quietly in the tray at login."; Switch { checked: settingsObj.autostart; onToggled: settingsObj.autostart = checked } }
                SettingRow { label: "Start hidden in the tray"; Switch { checked: settingsObj.startHidden; onToggled: settingsObj.startHidden = checked } }
                Label {
                    text: "Salat " + Qt.application.version + " · free and open source (MIT) · <a href='https://github.com/HatemSelim94/salat'>GitHub</a>"
                    color: Theme.muted; font.pixelSize: 12; linkColor: Theme.accent
                    onLinkActivated: (link) => Qt.openUrlExternally(link)
                }
            }
        }
    }
}

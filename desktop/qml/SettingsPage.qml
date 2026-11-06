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
        ["recommended", Tr.t("rule.recommended", Tr.rev).split(" (")[0]], ["middleOfTheNight", Tr.t("rule.middleOfTheNight", Tr.rev)],
        ["seventhOfTheNight", Tr.t("rule.seventhOfTheNight", Tr.rev)], ["twilightAngle", Tr.t("rule.twilightAngle", Tr.rev)]]
    readonly property var languages: [["auto", Tr.t("settings.languageAuto", Tr.rev)], ["en", "English"], ["ar", "العربية"], ["de", "Deutsch"]]
    function minutesLabel(n) { return n === 0 ? Tr.t("settings.off", Tr.rev) : Tr.f("settings.minutes", { n: n }, Tr.rev) }
    readonly property var prayers: ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"]

    function indexOfKey(list, key) { for (let i = 0; i < list.length; i++) if (list[i][0] === key) return i; return 0 }

    header: ToolBar {
        background: Rectangle { color: Theme.bg }
        RowLayout {
            anchors.fill: parent
            anchors.leftMargin: 8
            ToolButton { text: Tr.rtl ? "›" : "‹"; font.pixelSize: 24; onClicked: page.back() }
            Label { text: Tr.t("settings.title", Tr.rev); font.pixelSize: 17; font.weight: Font.DemiBold; color: Theme.fg; Layout.fillWidth: true }
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
                title: Tr.t("settings.location", Tr.rev)
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 6
                    TextField {
                        id: cityField
                        Layout.fillWidth: true
                        placeholderText: Tr.t("settings.searchCity", Tr.rev)
                        text: settingsObj.label
                        onTextEdited: results.model = Cities.search(text, 6)
                        onActiveFocusChanged: { if (activeFocus) selectAll(); else results.model = [] }
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
                        LabeledField { label: Tr.t("settings.latitude", Tr.rev); text: settingsObj.latitude.toFixed(2); onCommitted: (v) => settingsObj.latitude = Number(v) }
                        LabeledField { label: Tr.t("settings.longitude", Tr.rev); text: settingsObj.longitude.toFixed(2); onCommitted: (v) => settingsObj.longitude = Number(v) }
                        LabeledField { label: Tr.t("settings.timezone", Tr.rev); text: settingsObj.timezone; onCommitted: (v) => settingsObj.timezone = v; Layout.preferredWidth: 140 }
                    }
                }
            }

            // ---- Calculation -----------------------------------------------
            SettingsCard {
                title: Tr.t("settings.calculation", Tr.rev)
                SettingRow {
                    label: Tr.t("settings.method", Tr.rev)
                    hint: Tr.t("settings.methodHint", Tr.rev)
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
                        label: Tr.t("settings.asr", Tr.rev)
                        hint: Tr.t("settings.asrHint", Tr.rev)
                        stacked: true
                        ComboBox {
                            model: [Tr.t("settings.asrStandard", Tr.rev).split(" (")[0], Tr.t("settings.asrHanafi", Tr.rev)]
                            currentIndex: settingsObj.madhab === "hanafi" ? 1 : 0
                            onActivated: (i) => settingsObj.madhab = i === 1 ? "hanafi" : "shafi"
                            font.pixelSize: 13
                            Layout.fillWidth: true
                        }
                    }
                    SettingRow {
                        label: Tr.t("settings.highLatRule", Tr.rev)
                        hint: Tr.t("settings.highLatHint", Tr.rev)
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
                    Label { text: Tr.t("settings.adjustments", Tr.rev); color: Theme.fg }
                    Label { text: Tr.t("settings.adjustmentsHint", Tr.rev); color: Theme.muted; font.pixelSize: 12; wrapMode: Text.WordWrap; Layout.fillWidth: true }
                    GridLayout {
                        columns: 3
                        columnSpacing: 12
                        rowSpacing: 4
                        Layout.fillWidth: true
                        Repeater {
                            model: page.prayers
                            delegate: RowLayout {
                                required property string modelData
                                Label { text: Tr.t("prayer." + modelData, Tr.rev); color: Theme.fg; Layout.fillWidth: true }
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
                    Label { text: Tr.t("settings.preview", Tr.rev); color: Theme.muted; font.pixelSize: 12 }
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

            // ---- Iqamah / Jumu'ah / Ramadan -----------------------------------
            SettingsCard {
                title: Tr.t("settings.iqamah", Tr.rev)
                Label { text: Tr.t("settings.iqamahHint", Tr.rev); color: Theme.muted; font.pixelSize: 12; wrapMode: Text.WordWrap; Layout.fillWidth: true }
                GridLayout {
                    columns: 3; columnSpacing: 12; rowSpacing: 4; Layout.fillWidth: true
                    Repeater {
                        model: ["fajr", "dhuhr", "asr", "maghrib", "isha"]
                        delegate: RowLayout {
                            required property string modelData
                            Label { text: Tr.t("prayer." + modelData, Tr.rev); color: Theme.fg; Layout.fillWidth: true }
                            TextField {
                                text: settingsObj.iqamah(modelData)
                                validator: IntValidator { bottom: 0; top: 120 }
                                horizontalAlignment: TextInput.AlignRight
                                font.pixelSize: 13
                                Layout.preferredWidth: 44
                                onEditingFinished: settingsObj.setIqamah(modelData, Number(text))
                            }
                        }
                    }
                }
                SettingRow { label: Tr.t("settings.jumuah", Tr.rev); Switch { checked: settingsObj.jumuahEnabled; onToggled: settingsObj.jumuahEnabled = checked } }
                SettingRow {
                    label: Tr.t("settings.jumuahReminder", Tr.rev)
                    ComboBox {
                        readonly property var values: [0, 15, 30, 45, 60, 90]
                        model: values.map(v => page.minutesLabel(v))
                        currentIndex: Math.max(0, values.indexOf(settingsObj.jumuahReminderMinutes))
                        onActivated: (i) => settingsObj.jumuahReminderMinutes = values[i]
                        font.pixelSize: 13
                        Layout.preferredWidth: 110
                    }
                }
                SettingRow {
                    label: Tr.t("settings.ramadan", Tr.rev)
                    ComboBox {
                        readonly property var values: ["auto", "on", "off"]
                        model: [Tr.t("settings.ramadanAuto", Tr.rev).split(" (")[0], Tr.t("settings.on", Tr.rev), Tr.t("settings.off", Tr.rev)]
                        currentIndex: Math.max(0, values.indexOf(settingsObj.ramadan))
                        onActivated: (i) => settingsObj.ramadan = values[i]
                        font.pixelSize: 13
                        Layout.preferredWidth: 110
                    }
                }
            }

            // ---- Quiet hours ----------------------------------------------------
            SettingsCard {
                title: Tr.t("settings.quietHours", Tr.rev)
                SettingRow { label: Tr.t("settings.quietHours", Tr.rev); hint: Tr.t("settings.quietHoursHint", Tr.rev); Switch { checked: settingsObj.quietHoursEnabled; onToggled: settingsObj.quietHoursEnabled = checked } }
                RowLayout {
                    spacing: 8
                    LabeledField { label: Tr.t("settings.from", Tr.rev); text: settingsObj.quietFrom; onCommitted: (v) => settingsObj.quietFrom = v }
                    LabeledField { label: Tr.t("settings.to", Tr.rev); text: settingsObj.quietTo; onCommitted: (v) => settingsObj.quietTo = v }
                }
            }

            // ---- Mosque timetable ----------------------------------------------
            SettingsCard {
                id: timetableCard
                title: Tr.t("settings.timetable", Tr.rev)
                property string status: settingsObj.timetableDays > 0 ? Tr.f("settings.timetableStatus", { days: settingsObj.timetableDays }, Tr.rev) : ""
                Label { text: Tr.t("settings.timetableHint", Tr.rev); color: Theme.muted; font.pixelSize: 12; wrapMode: Text.WordWrap; Layout.fillWidth: true }
                RowLayout {
                    Button { text: Tr.t("settings.timetableImport", Tr.rev); flat: true; font.capitalization: Font.MixedCase; onClicked: { var m = engine.importTimetable(); if (m) timetableCard.status = m } }
                    Button { text: Tr.t("settings.timetableClear", Tr.rev); flat: true; font.capitalization: Font.MixedCase; visible: settingsObj.timetableDays > 0; onClicked: { engine.clearTimetable(); timetableCard.status = "" } }
                }
                Label { text: timetableCard.status; visible: text.length > 0; color: Theme.muted; font.pixelSize: 12 }
            }

            // ---- Reminders -------------------------------------------------
            SettingsCard {
                title: Tr.t("settings.reminders", Tr.rev)
                SettingRow { label: Tr.t("settings.notify", Tr.rev); Switch { checked: settingsObj.notificationsEnabled; onToggled: settingsObj.notificationsEnabled = checked } }
                SettingRow {
                    label: Tr.t("settings.preReminder", Tr.rev)
                    ComboBox {
                        readonly property var values: [0, 5, 10, 15, 20, 30]
                        model: values.map(v => page.minutesLabel(v))
                        currentIndex: Math.max(0, values.indexOf(settingsObj.preReminderMinutes))
                        onActivated: (i) => settingsObj.preReminderMinutes = values[i]
                        font.pixelSize: 13
                        Layout.preferredWidth: 110
                    }
                }
                SettingRow { label: Tr.t("settings.chime", Tr.rev); hint: Tr.t("settings.chimeHint", Tr.rev); Switch { checked: settingsObj.sound; onToggled: settingsObj.sound = checked } }
                SettingRow {
                    label: Tr.t("settings.snooze", Tr.rev)
                    ComboBox {
                        readonly property var values: [5, 10, 15, 20, 30]
                        model: values.map(v => page.minutesLabel(v))
                        currentIndex: Math.max(0, values.indexOf(settingsObj.snoozeMinutes))
                        onActivated: (i) => settingsObj.snoozeMinutes = values[i]
                        font.pixelSize: 13
                        Layout.preferredWidth: 110
                    }
                }
                SettingRow { label: Tr.t("settings.hour12", Tr.rev); Switch { checked: settingsObj.hour12; onToggled: settingsObj.hour12 = checked } }
                Button { text: Tr.t("settings.testNotification", Tr.rev); flat: true; font.capitalization: Font.MixedCase; onClicked: engine.testNotification() }
            }

            // ---- App -------------------------------------------------------
            SettingsCard {
                title: Tr.t("settings.app", Tr.rev)
                SettingRow {
                    label: Tr.t("settings.language", Tr.rev)
                    ComboBox {
                        model: page.languages.map(l => l[1])
                        currentIndex: page.indexOfKey(page.languages, settingsObj.language)
                        onActivated: (i) => settingsObj.language = page.languages[i][0]
                        font.pixelSize: 13
                        Layout.preferredWidth: 130
                    }
                }
                SettingRow { label: Tr.t("settings.autostart", Tr.rev); hint: Tr.t("settings.autostartHint", Tr.rev); Switch { checked: settingsObj.autostart; onToggled: settingsObj.autostart = checked } }
                SettingRow { label: Tr.t("settings.startHidden", Tr.rev); Switch { checked: settingsObj.startHidden; onToggled: settingsObj.startHidden = checked } }
                Label {
                    readonly property string contact: About.contactEmail
                    text: "Salat " + Qt.application.version
                          + " · <a href='" + About.authorUrl + "'>" + Tr.f("settings.madeBy", { name: About.authorName }, Tr.rev) + "</a>"
                          + (contact ? " · <a href='mailto:" + contact + "?subject=Salat%20desktop%20" + Qt.application.version + "'>" + Tr.t("settings.contact", Tr.rev) + "</a>" : "")
                          + "<br>" + Tr.t("settings.foss", Tr.rev)
                    color: Theme.muted; font.pixelSize: 12; linkColor: Theme.accent; wrapMode: Text.WordWrap; Layout.fillWidth: true
                    onLinkActivated: (link) => Qt.openUrlExternally(link)
                }
            }
        }
    }
}

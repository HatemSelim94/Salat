package io.github.hatemselim94.salat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.hatemselim94.salat.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hatemselim94.salat.core.Cities
import io.github.hatemselim94.salat.core.City
import io.github.hatemselim94.salat.core.Engine
import io.github.hatemselim94.salat.core.Madhab
import io.github.hatemselim94.salat.core.Method
import io.github.hatemselim94.salat.core.Prayer
import io.github.hatemselim94.salat.core.Settings
import io.github.hatemselim94.salat.notify.Notifications

private val RULES = listOf(
    "recommended" to R.string.rule_recommended, "middleOfTheNight" to R.string.rule_middleOfTheNight,
    "seventhOfTheNight" to R.string.rule_seventhOfTheNight, "twilightAngle" to R.string.rule_twilightAngle,
)
private val PRE_VALUES = listOf(0, 5, 10, 15, 20, 30)
private val SNOOZE_VALUES = listOf(5, 10, 15, 20, 30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: Settings, engine: Engine, revision: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val fg = MaterialTheme.colorScheme.onBackground
    // `revision` is read so the screen recomposes after any settings write.
    @Suppress("UNUSED_VARIABLE") val rev = revision
    val minutes: (Int) -> String = { n -> if (n == 0) context.getString(R.string.settings_off) else context.getString(R.string.settings_minutes, n.toString()) }
    val rules = RULES.map { (id, res) -> id to stringResource(res).substringBefore(" (") }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back)) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Card(stringResource(R.string.settings_location)) {
                var query by remember { mutableStateOf(settings.label) }
                var results by remember { mutableStateOf(emptyList<City>()) }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; results = Cities.search(context, it, 6) },
                    label = { Text(stringResource(R.string.settings_city)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                for (c in results) {
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            settings.setLocation(c.lat, c.lng, c.tz, c.label)
                            query = c.label
                            results = emptyList()
                        }.padding(vertical = 8.dp, horizontal = 4.dp),
                    ) {
                        Text(c.label, color = fg, modifier = Modifier.weight(1f))
                        Text(c.tz, color = muted, fontSize = 12.sp)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(stringResource(R.string.settings_latitude), "%.4f".format(java.util.Locale.ROOT, settings.latitude), Modifier.weight(1f)) { v -> v.toDoubleOrNull()?.let { if (it in -90.0..90.0) settings.latitude = it } }
                    NumberField(stringResource(R.string.settings_longitude), "%.4f".format(java.util.Locale.ROOT, settings.longitude), Modifier.weight(1f)) { v -> v.toDoubleOrNull()?.let { if (it in -180.0..180.0) settings.longitude = it } }
                }
                var tz by remember { mutableStateOf(settings.timezone) }
                OutlinedTextField(value = tz, onValueChange = { tz = it; settings.timezone = it }, label = { Text(stringResource(R.string.settings_timezone)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            Card(stringResource(R.string.settings_calculation)) {
                Picker(stringResource(R.string.settings_method), Method.entries.filter { it != Method.OTHER }.map { it to it.label }, settings.method) { settings.method = it }
                Picker(stringResource(R.string.settings_asr), listOf(Madhab.SHAFI to stringResource(R.string.settings_asrStandard), Madhab.HANAFI to stringResource(R.string.settings_asrHanafi)), settings.madhab) { settings.madhab = it }
                Picker(stringResource(R.string.settings_highLatRule), rules, settings.highLatitudeRule) { settings.highLatitudeRule = it }
                Text(stringResource(R.string.settings_adjustments), color = fg)
                Text(stringResource(R.string.settings_adjustmentsHint), color = muted, fontSize = 12.sp)
                for (rowPrayers in Prayer.entries.chunked(3)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (p in rowPrayers) {
                            NumberField(p.label(context), settings.adjustment(p.key).toString(), Modifier.weight(1f)) { v -> v.toIntOrNull()?.let { settings.setAdjustment(p.key, it) } }
                        }
                    }
                }
                val now = System.currentTimeMillis() / 1000
                val day = engine.computeDay(now)
                Text(stringResource(R.string.settings_preview), color = muted, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (p in Prayer.entries) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(p.label(context), color = muted, fontSize = 11.sp)
                            Text(engine.formatTime(day[p]), color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Card(stringResource(R.string.settings_reminders)) {
                ToggleRow(stringResource(R.string.settings_notify), settings.notificationsEnabled) { settings.notificationsEnabled = it }
                Picker(stringResource(R.string.settings_preReminder), PRE_VALUES.map { it to minutes(it) }, settings.preReminderMinutes) { settings.preReminderMinutes = it }
                Picker(
                    stringResource(R.string.settings_sound),
                    listOf("default" to stringResource(R.string.settings_soundDefault), "chime" to stringResource(R.string.settings_soundChime), "silent" to stringResource(R.string.settings_soundSilent)),
                    settings.soundMode,
                ) { settings.soundMode = it }
                Picker(stringResource(R.string.settings_snooze), SNOOZE_VALUES.map { it to minutes(it) }, settings.snoozeMinutes) { settings.snoozeMinutes = it }
                ToggleRow(stringResource(R.string.settings_hour12), settings.hour12) { settings.hour12 = it }
                TextButton(onClick = { Notifications.show(context, settings, engine, Prayer.DHUHR, pre = false, epoch = System.currentTimeMillis() / 1000) }) {
                    Text(stringResource(R.string.settings_testNotification))
                }
            }

            Text("Salat · " + stringResource(R.string.settings_foss) + " · github.com/HatemSelim94/salat", color = muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        content()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, hint: String = "", onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onBackground)
            if (hint.isNotEmpty()) Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> Picker(label: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(options.firstOrNull { it.first == selected }?.second ?: "", color = MaterialTheme.colorScheme.primary)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for ((value, text) in options) {
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(value); open = false })
            }
        }
    }
}

/** Edits locally and commits when focus leaves, so typing "48." is not reset mid-way by a recomposition. */
@Composable
private fun NumberField(label: String, initial: String, modifier: Modifier = Modifier, onCommit: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.onFocusChanged { if (!it.isFocused && text != initial) onCommit(text) },
    )
}

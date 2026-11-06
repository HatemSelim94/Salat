package io.github.hatemselim94.salat.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import android.os.Build
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.hatemselim94.salat.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hatemselim94.salat.core.Engine
import io.github.hatemselim94.salat.core.Features
import io.github.hatemselim94.salat.core.Prayer
import io.github.hatemselim94.salat.core.Settings
import io.github.hatemselim94.salat.notify.Scheduler
import kotlinx.coroutines.delay

enum class Screen { HOME, SETTINGS, MONTH, QIBLA }

@Composable
fun SalatApp(settings: Settings, engine: Engine, revision: Int) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (screen) {
            Screen.SETTINGS -> SettingsScreen(settings = settings, engine = engine, revision = revision, onBack = { screen = Screen.HOME })
            Screen.MONTH -> MonthScreen(settings = settings, engine = engine, revision = revision, onBack = { screen = Screen.HOME })
            Screen.QIBLA -> QiblaScreen(settings = settings, onBack = { screen = Screen.HOME })
            Screen.HOME -> HomeScreen(settings = settings, engine = engine, revision = revision, onNavigate = { screen = it })
        }
    }
}

@Composable
fun HomeScreen(settings: Settings, engine: Engine, revision: Int, onNavigate: (Screen) -> Unit) {
    val onOpenSettings = { onNavigate(Screen.SETTINGS) }
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis() / 1000
            delay(30_000)
        }
    }
    // Recompute when settings change (revision) or time moves.
    val day = remember(revision, now / 60) { engine.computeDay(now) }
    val next = remember(revision, now / 60) { engine.nextPrayer(now) }
    val current = remember(revision, now / 60) { engine.currentPrayer(now) }
    val tomorrowFajr = remember(revision, day.date) { engine.computeDay(now, 1)[Prayer.FAJR] }
    val beforeFajr = day[Prayer.FAJR] > now
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val fg = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background
    val period = periodFor(now, day[Prayer.FAJR], day[Prayer.SUNRISE], day[Prayer.DHUHR], day[Prayer.ASR], day[Prayer.MAGHRIB], day[Prayer.ISHA])
    val (cardBrush, cardColors) = periodBrush(period)

    // Edge-to-edge window: keep content clear of both the status bar and the navigation/gesture bar.
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
        // Header: location (tappable) + dates in a soft pill
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable(onClick = onOpenSettings).padding(vertical = 6.dp),
            ) {
                PinGlyph(color = accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(settings.label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = fg)
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(engine.hijriDate(now), fontSize = 12.sp, color = fg, fontWeight = FontWeight.Medium)
                Text(engine.gregorianDate(now), fontSize = 11.sp, color = muted)
            }
        }
        Spacer(Modifier.height(16.dp))

        val ramadan = remember(revision, day.date) { Features.isRamadan(settings, day.date) }
        if (ramadan) {
            // Suhoor ends at Fajr, Iftar at Maghrib: show whichever is next.
            val suhoorNext = now < day[Prayer.FAJR]
            val iftarNext = !suhoorNext && now < day[Prayer.MAGHRIB]
            val (label, at) = when {
                suhoorNext -> stringResource(R.string.prayer_suhoor) to day[Prayer.FAJR]
                iftarNext -> stringResource(R.string.prayer_iftar) to day[Prayer.MAGHRIB]
                else -> stringResource(R.string.prayer_suhoor) to tomorrowFajr
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_ramadan).uppercase(), fontSize = 10.sp, color = accent, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Text(label, fontSize = 15.sp, color = fg, fontWeight = FontWeight.SemiBold)
                }
                Text(engine.formatTime(at) + " · " + engine.countdown(now, at), fontSize = 13.sp, color = fg)
            }
            Spacer(Modifier.height(10.dp))
        }

        // Next-prayer card, coloured by the period of the day
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(cardBrush, RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_nextPrayer).uppercase(), fontSize = 10.sp, color = cardColors.fgMuted, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(engine.label(context, next.prayer, engine.computeDay(next.epoch).date), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = cardColors.fg, lineHeight = 36.sp)
                    val tomorrow = engine.computeDay(next.epoch).date != day.date
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(engine.formatTime(next.epoch) + (if (tomorrow) " " + stringResource(R.string.home_tomorrow) else ""), fontSize = 14.sp, color = cardColors.fgMuted)
                        Text("  ·  " + stringResource(R.string.home_in) + " ", fontSize = 14.sp, color = cardColors.fgMuted)
                        AnimatedContent(targetState = engine.countdown(now, next.epoch), transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "countdown") { cd ->
                            Text(cd, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = cardColors.fg)
                        }
                    }
                }
                PrayerGlyph(next.prayer, color = cardColors.fg.copy(alpha = 0.85f), modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(12.dp))
            DayProgress(
                now = now,
                times = Prayer.OBLIGATORY.map { day[it] },
                nextFajr = tomorrowFajr,
                track = cardColors.fg.copy(alpha = 0.18f),
                fill = cardColors.fg.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth().height(14.dp),
            )
        }
        Spacer(Modifier.height(16.dp))

        // Today's times
        for (p in Prayer.entries) {
            val t = day[p]
            val isCurrent = !beforeFajr && p == current.prayer && p.obligatory
            val passed = t <= now
            val color = if (isCurrent) fg else if (!p.obligatory || passed) muted else fg
            val rowBg = if (isCurrent) MaterialTheme.colorScheme.surface else bg
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(rowBg, RoundedCornerShape(10.dp))
                    .padding(start = 2.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(3.dp).height(22.dp).background(if (isCurrent) accent else Color.Transparent, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(9.dp))
                PrayerGlyph(p, color = if (isCurrent) accent else color.copy(alpha = if (p.obligatory) 0.9f else 0.7f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(engine.label(context, p, day.date), color = color, fontSize = if (p.obligatory) 15.sp else 13.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                val iq = settings.iqamah(p.key)
                if (p.obligatory && iq > 0) {
                    Text(stringResource(R.string.home_iqamah).take(3) + " " + engine.formatTime(t + iq * 60), color = muted, fontSize = 11.sp)
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    engine.formatTime(t), color = color,
                    fontSize = if (p.obligatory) 17.sp else 14.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                )
            }
            if (p != Prayer.ISHA) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), modifier = Modifier.padding(start = 44.dp))
        }

        Spacer(Modifier.height(14.dp))
        if (!settings.locationConfigured) {
            ActionCard(stringResource(R.string.home_locationGuessed), stringResource(R.string.home_setCityShort), onClick = onOpenSettings)
        }
        val pm = context.getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            ActionCard(stringResource(R.string.home_batteryShort), stringResource(R.string.home_allow), hint = stringResource(R.string.home_batterySteps)) {
                requestBatteryExemption(context)
            }
        }
        if (Build.VERSION.SDK_INT in 31..32 && !Scheduler.canScheduleExact(context)) {
            ActionCard(stringResource(R.string.home_exactAlarmShort), stringResource(R.string.home_allow)) {
                context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }

        Spacer(Modifier.weight(1f))
        // Bottom row: method on the left, three icon buttons on the right
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(settings.method.label, fontSize = 11.sp, color = muted, modifier = Modifier.weight(1f))
            NavButton(NavIcon.CALENDAR, stringResource(R.string.home_month), muted) { onNavigate(Screen.MONTH) }
            NavButton(NavIcon.COMPASS, stringResource(R.string.home_qibla), muted) { onNavigate(Screen.QIBLA) }
            NavButton(NavIcon.GEAR, stringResource(R.string.home_settings), muted, onOpenSettings)
        }
    }
}

@Composable
private fun NavButton(icon: NavIcon, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp).heightIn(min = 48.dp),
    ) {
        NavGlyph(icon, color, Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = color)
    }
}

@Composable
private fun PinGlyph(color: Color, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width; val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(w * 0.12f, 0f, w * 0.88f, h * 0.76f))
        }
        drawPath(path, color)
        val tip = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.2f, h * 0.55f); lineTo(w * 0.5f, h); lineTo(w * 0.8f, h * 0.55f); close() }
        drawPath(tip, color)
        drawCircle(Color.White, w * 0.16f, androidx.compose.ui.geometry.Offset(w / 2, h * 0.38f))
    }
}

/**
 * Opens Salat's own App-info page (no special permission needed). From there: Battery → Unrestricted.
 * This avoids the system-wide optimisation list, where the app is hidden behind a filter.
 */
private fun requestBatteryExemption(context: android.content.Context) {
    val appInfo = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:" + context.packageName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(appInfo)
    } catch (e: Exception) {
        context.startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** A message the user must act on, with an explicit button so it does not read as plain text. */
@Composable
private fun ActionCard(text: String, action: String, hint: String = "", onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            if (hint.isNotEmpty()) Text(hint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        Button(onClick = onClick, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp), modifier = Modifier.heightIn(min = 40.dp)) {
            Text(action, fontSize = 13.sp)
        }
    }
}

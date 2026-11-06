package io.github.hatemselim94.salat.ui

import android.content.Intent
import android.os.Build
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hatemselim94.salat.core.Engine
import io.github.hatemselim94.salat.core.Prayer
import io.github.hatemselim94.salat.core.Settings
import io.github.hatemselim94.salat.notify.Scheduler
import kotlinx.coroutines.delay

@Composable
fun SalatApp(settings: Settings, engine: Engine, revision: Int) {
    var showSettings by remember { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (showSettings) SettingsScreen(settings = settings, engine = engine, revision = revision, onBack = { showSettings = false })
        else HomeScreen(settings = settings, engine = engine, revision = revision, onOpenSettings = { showSettings = true })
    }
}

@Composable
fun HomeScreen(settings: Settings, engine: Engine, revision: Int, onOpenSettings: () -> Unit) {
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
    val beforeFajr = day[Prayer.FAJR] > now
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val fg = MaterialTheme.colorScheme.onBackground

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(settings.label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = fg, modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(engine.hijriDate(now), fontSize = 12.sp, color = muted)
                Text(engine.gregorianDate(now), fontSize = 12.sp, color = muted)
            }
        }
        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp))
                .padding(16.dp),
        ) {
            Text("Next prayer", fontSize = 12.sp, color = muted)
            Text(next.prayer.label, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = fg)
            val tomorrow = engine.computeDay(next.epoch).date != day.date
            Row {
                Text(engine.formatTime(next.epoch) + (if (tomorrow) " tomorrow" else "") + " · in ", fontSize = 13.sp, color = muted)
                Text(engine.countdown(now, next.epoch), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg)
            }
        }
        Spacer(Modifier.height(14.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        for (p in Prayer.entries) {
            val t = day[p]
            val isCurrent = !beforeFajr && p == current.prayer && p.obligatory
            val passed = t <= now
            val color = if (isCurrent) fg else if (!p.obligatory || passed) muted else fg
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(if (isCurrent) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(p.label, color = color, fontSize = if (p.obligatory) 15.sp else 14.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                if (isCurrent) {
                    Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(Modifier.width(8.dp))
                }
                Text(engine.formatTime(t), color = color, fontSize = if (p.obligatory) 15.sp else 14.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal)
            }
            if (!isCurrent) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }

        Spacer(Modifier.height(14.dp))
        if (!settings.locationConfigured) {
            NoticeCard("Location guessed from your time zone. Tap to set your city for exact times.", onClick = onOpenSettings)
        }
        val pm = context.getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            NoticeCard(
                "To make sure reminders fire on time, allow Salat to run without battery restrictions.",
                onClick = {
                    context.startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
            )
        }
        if (Build.VERSION.SDK_INT in 31..32 && !Scheduler.canScheduleExact(context)) {
            NoticeCard(
                "Android needs permission for exact alarms so reminders are on the minute. Tap to allow.",
                onClick = {
                    context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
            )
        }

        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(settings.method.label, fontSize = 12.sp, color = muted, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenSettings) { Text("Settings") }
        }
    }
}

@Composable
private fun NoticeCard(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    )
}

package io.github.hatemselim94.salat.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import io.github.hatemselim94.salat.R
import io.github.hatemselim94.salat.core.Engine
import io.github.hatemselim94.salat.core.Features
import io.github.hatemselim94.salat.core.Prayer
import io.github.hatemselim94.salat.core.Settings
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Scrollable monthly timetable with prev/next month and CSV export via the share sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(settings: Settings, engine: Engine, revision: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val zone = ZoneId.of(settings.timezone)
    val today = LocalDate.now(zone)
    var ym by remember { mutableStateOf(YearMonth.from(today)) }
    val rows = remember(ym, revision) { Features.monthTable(settings, engine, ym) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val fg = MaterialTheme.colorScheme.onBackground
    val listState = rememberLazyListState()
    LaunchedEffect(ym) { if (ym == YearMonth.from(today)) listState.scrollToItem((today.dayOfMonth - 1).coerceAtLeast(0)) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TopAppBar(
            title = { Text(stringResource(R.string.month_title), fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back)) } },
            actions = {
                TextButton(onClick = { shareCsv(context, settings, engine, ym, rows) }) { Text(stringResource(R.string.month_export)) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { ym = ym.minusMonths(1) }) { Text("‹", fontSize = 22.sp) }
            Text(
                ym.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())),
                modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = fg,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            TextButton(onClick = { ym = ym.plusMonths(1) }) { Text("›", fontSize = 22.sp) }
        }
        // Header
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text("", Modifier.weight(0.9f))
            for (p in Prayer.entries) Text(p.label(context).take(if (p == Prayer.SUNRISE) 4 else 7), Modifier.weight(1f), fontSize = 11.sp, color = muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(rows, key = { it.date.toString() }) { r ->
                val isToday = r.date == today
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(0.9f)) {
                        Text(r.date.dayOfMonth.toString(), fontSize = 14.sp, fontWeight = if (isToday || r.friday) FontWeight.SemiBold else FontWeight.Normal, color = fg)
                        Text(r.date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())), fontSize = 10.sp, color = if (r.friday) MaterialTheme.colorScheme.primary else muted)
                    }
                    for (p in Prayer.entries) {
                        Text(engine.formatTime(r.times[p]), Modifier.weight(1f), fontSize = 12.sp, color = if (p == Prayer.SUNRISE) muted else fg,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

private fun shareCsv(context: android.content.Context, settings: Settings, engine: Engine, ym: YearMonth, rows: List<Features.MonthRow>) {
    val csv = Features.monthCsv(engine, rows)
    val dir = File(context.cacheDir, "export").apply { mkdirs() }
    val file = File(dir, "salat-${settings.label.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-')}-$ym.csv")
    file.writeText(csv)
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

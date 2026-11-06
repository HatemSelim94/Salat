package io.github.hatemselim94.salat.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hatemselim94.salat.R
import io.github.hatemselim94.salat.core.Features
import io.github.hatemselim94.salat.core.Settings
import kotlin.math.roundToInt

/**
 * Qibla bearing from the configured coordinates, with a compass arrow when the
 * device has a rotation-vector sensor. The sensor is only active while this
 * screen is visible (DisposableEffect) — nothing runs in the background.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(settings: Settings, onBack: () -> Unit) {
    val context = LocalContext.current
    val bearing = remember(settings.latitude, settings.longitude) { Features.qiblaBearing(settings.latitude, settings.longitude) }
    val distance = remember(settings.latitude, settings.longitude) { Features.distanceToKaabaKm(settings.latitude, settings.longitude) }
    var heading by remember { mutableFloatStateOf(Float.NaN) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            private val rot = FloatArray(9)
            private val orientation = FloatArray(3)
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                SensorManager.getOrientation(rot, orientation)
                val az = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val next = (az + 360f) % 360f
                // Light smoothing so the arrow does not jitter.
                heading = if (heading.isNaN()) next else lerpAngle(heading, next, 0.15f)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val fg = MaterialTheme.colorScheme.onBackground
    val hasCompass = remember { (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TopAppBar(
            title = { Text(stringResource(R.string.home_qibla), fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back)) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.home_qiblaBearing, bearing.roundToInt().toString()), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = fg)
            Text(stringResource(R.string.home_qiblaDistance, distance.roundToInt().toString()), fontSize = 13.sp, color = muted)
            Spacer(Modifier.height(28.dp))
            Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                val accent = MaterialTheme.colorScheme.primary
                val line = MaterialTheme.colorScheme.outline
                val arrowRotation = if (heading.isNaN()) bearing.toFloat() else (bearing.toFloat() - heading + 360f) % 360f
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2
                    val c = Offset(size.width / 2, size.height / 2)
                    drawCircle(line, r - 2.dp.toPx(), c, style = Stroke(2.dp.toPx()))
                    // Tick marks every 30°
                    for (i in 0 until 12) rotate(i * 30f, c) {
                        drawLine(line, Offset(c.x, c.y - r + 6.dp.toPx()), Offset(c.x, c.y - r + (if (i % 3 == 0) 18 else 12).dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                    }
                    // North marker (rotates with the device when a compass is available)
                    val northRotation = if (heading.isNaN()) 0f else (360f - heading) % 360f
                    rotate(northRotation, c) {
                        drawCircle(muted, 5.dp.toPx(), Offset(c.x, c.y - r + 30.dp.toPx()))
                    }
                    // Qibla arrow
                    rotate(arrowRotation, c) {
                        val path = Path().apply {
                            moveTo(c.x, c.y - r + 44.dp.toPx())
                            lineTo(c.x - 16.dp.toPx(), c.y + 10.dp.toPx())
                            lineTo(c.x, c.y - 4.dp.toPx())
                            lineTo(c.x + 16.dp.toPx(), c.y + 10.dp.toPx())
                            close()
                        }
                        drawPath(path, accent)
                    }
                    drawCircle(Color.White, 4.dp.toPx(), c)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                if (hasCompass) stringResource(R.string.home_qiblaCompassHint) else stringResource(R.string.home_qiblaNoCompass),
                fontSize = 13.sp, color = muted, textAlign = TextAlign.Center,
            )
        }
    }
}

private fun lerpAngle(from: Float, to: Float, t: Float): Float {
    var d = ((to - from + 540f) % 360f) - 180f
    return (from + d * t + 360f) % 360f
}

package io.github.hatemselim94.salat

import android.Manifest
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import io.github.hatemselim94.salat.core.Engine
import io.github.hatemselim94.salat.core.Settings
import io.github.hatemselim94.salat.notify.Notifications
import io.github.hatemselim94.salat.notify.Scheduler
import io.github.hatemselim94.salat.ui.SalatApp
import io.github.hatemselim94.salat.ui.SalatTheme

class MainActivity : ComponentActivity() {
    private lateinit var settings: Settings
    private lateinit var engine: Engine

    /** Bumped on every settings change so Compose recomputes. */
    private var revision by mutableIntStateOf(0)
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        revision++
        Scheduler.reschedule(this)
    }

    // Asked once on first launch; after a refusal the home screen shows a card that opens the system page.
    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { revision++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settings = Settings(this)
        engine = Engine(settings)
        Notifications.ensureChannels(this)
        Scheduler.reschedule(this)
        if (Build.VERSION.SDK_INT >= 33) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            SalatTheme {
                SalatApp(settings = settings, engine = engine, revision = revision)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        settings.registerListener(listener)
        revision++ // refresh after returning from system settings
    }

    override fun onStop() {
        settings.unregisterListener(listener)
        super.onStop()
    }
}

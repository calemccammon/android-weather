package com.calemccammon.androidweather

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.calemccammon.androidweather.core.designsystem.WeatherTheme
import com.calemccammon.androidweather.core.notifications.AlertIntents
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Not again after a rotation: by then the user may have moved on.
        if (savedInstanceState == null) open(intent)

        setContent {
            WeatherTheme {
                WeatherNavHost(placeOpened = viewModel.placeOpened)
            }
        }
    }

    /** An alert pressed while the app is already open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        open(intent)
    }

    private fun open(intent: Intent?) {
        // Coming back through Recents hands over the intent the task began
        // with. An alert pressed then is not pressed again now.
        val fromRecents = intent != null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (fromRecents) return
        AlertIntents.coordinates(intent)?.let { (latitude, longitude) ->
            viewModel.openSavedPlaceAt(latitude, longitude)
        }
    }
}

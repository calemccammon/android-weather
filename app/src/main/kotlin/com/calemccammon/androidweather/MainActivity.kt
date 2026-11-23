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
                WeatherNavHost()
            }
        }
    }

    /** An alert pressed while the app is already open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        open(intent)
    }

    private fun open(intent: Intent?) {
        AlertIntents.savedPlaceId(intent)?.let(viewModel::openSavedPlace)
    }
}

package com.calemccammon.androidweather.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Has the widget drawn again whenever what it shows changes.
 *
 * A widget on the home screen is a picture the app sent earlier; it does not
 * watch anything once drawn. So while the app's process is alive, for a
 * screen or for the background refresh, this watches on its behalf: a newer
 * forecast, another place or other units, and every widget is redrawn.
 */
@Singleton
class WidgetUpdater
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val observeWidgetState: ObserveWidgetState,
    ) {
        // Lives as long as the process does.
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /** Starts watching. Called once, when the process starts. */
        fun start() {
            scope.launch {
                // With no widget placed this costs one database read per change and draws nothing.
                observeWidgetState().collect { WeatherWidget().updateAll(context) }
            }
        }
    }

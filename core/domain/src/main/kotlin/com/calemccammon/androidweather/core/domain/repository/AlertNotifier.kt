package com.calemccammon.androidweather.core.domain.repository

import com.calemccammon.androidweather.core.model.WeatherAlert

/** Tells the user of severe weather while the app is not on screen. */
fun interface AlertNotifier {
    /** Shows one notification for each of [alerts]. Does nothing if the system will not let the app notify. */
    fun show(alerts: List<WeatherAlert>)
}

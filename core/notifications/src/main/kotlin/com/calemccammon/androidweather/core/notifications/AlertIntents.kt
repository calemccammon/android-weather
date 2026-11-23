package com.calemccammon.androidweather.core.notifications

import android.content.Intent
import com.calemccammon.androidweather.core.model.Place

/** What an alert's notification puts in the intent that opens the app. */
object AlertIntents {
    private const val EXTRA_LATITUDE = "com.calemccammon.androidweather.ALERT_LATITUDE"
    private const val EXTRA_LONGITUDE = "com.calemccammon.androidweather.ALERT_LONGITUDE"

    internal fun put(
        intent: Intent,
        place: Place,
    ) {
        intent.putExtra(EXTRA_LATITUDE, place.latitude)
        intent.putExtra(EXTRA_LONGITUDE, place.longitude)
    }

    /**
     * Where the place an alert was about is, as latitude and longitude, if
     * [intent] came from one. It says where and not which saved place, so it
     * still finds a place that was removed and saved again since.
     */
    fun coordinates(intent: Intent?): Pair<Double, Double>? {
        if (intent == null || !intent.hasExtra(EXTRA_LATITUDE) || !intent.hasExtra(EXTRA_LONGITUDE)) return null
        return intent.getDoubleExtra(EXTRA_LATITUDE, 0.0) to intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
    }
}

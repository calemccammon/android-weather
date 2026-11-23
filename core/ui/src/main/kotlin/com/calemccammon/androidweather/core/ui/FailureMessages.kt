package com.calemccammon.androidweather.core.ui

import com.calemccammon.androidweather.core.model.WeatherFailure

/** What to tell the user about each kind of failure. */
val WeatherFailure.message: UiText
    get() =
        uiText(
            when (this) {
                WeatherFailure.Timeout -> R.string.core_ui_failure_timeout
                WeatherFailure.NoConnection -> R.string.core_ui_failure_no_connection
                WeatherFailure.BadResponse -> R.string.core_ui_failure_bad_response
                WeatherFailure.Rejected -> R.string.core_ui_failure_rejected
                WeatherFailure.Unavailable -> R.string.core_ui_failure_unavailable
                WeatherFailure.InvalidPlace -> R.string.core_ui_failure_invalid_place
            },
        )

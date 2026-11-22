package com.calemccammon.androidweather.core.model

/** What the user has chosen, kept between launches. */
data class UserSettings(
    /** The units the user chose, or null if they have never chosen. */
    val units: Units? = null,
    /** The place whose forecast the home screen shows, if any. */
    val selectedPlace: Place? = null,
)
